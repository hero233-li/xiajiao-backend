const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {load,uuid,source}=require('./legacy-content.cjs');
const {initialCourses,initialOutlines}=load(path.join(source,'lib/study.ts'));
const {knowledge,knowledgeSources}=load(path.join(source,'lib/knowledge.ts'));
const {manuals,manualExercises,parseManualSections}=load(path.join(source,'lib/manuals.ts'));
const ids={'00023':'afdac469-0fe4-5007-833c-51a71333967b','02324':'ccd819bb-a48b-5c75-bfbf-e26219023fef','13175':'6450a55a-7d59-5632-b9ad-2a4e170af220','13015':'4c6db663-13a2-5b2b-8cfa-6422a6c37205','13216':'e2dd5f58-a12f-55e0-a691-455ea2233699','13171':'72494a28-2cb8-5df8-8f3f-2ba84b681e0d'};
const q=v=>v==null?'NULL':"'"+String(v).replace(/\\/g,'\\\\').replace(/'/g,"''")+"'";
const sql=['-- V3 extends public content through new releases; V1/V2 remain unchanged.'];
const insert=(t,c,v)=>sql.push(`INSERT INTO ${t} (${c}) VALUES (${v.join(',')});`);
let modules=0,examples=0,manualSections=0,manualExercisesCount=0;
for(const c of initialCourses.filter(c=>c.current)) {
 const old=uuid(c.code+':release:1'),release=uuid(c.code+':release:2'),course=ids[c.code];
 const k=knowledge[c.id];if(!k?.length)throw new Error('Missing knowledge '+c.id);
 const manual=manuals[c.id];
 const sha=crypto.createHash('sha256').update(JSON.stringify({k,manual})).digest('hex');
 insert('content_release','id,course_id,version_no,state,published_at,source_sha',[q(release),q(course),2,q('PUBLISHED'),'UTC_TIMESTAMP(6)',q(sha)]);
 sql.push(`INSERT INTO chapter_revision SELECT ${q(release)},chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id=${q(old)};`);
 sql.push(`INSERT INTO item_revision SELECT ${q(release)},item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id=${q(old)};`);
 for(const m of k) {
  const id=uuid(c.code+':module:'+m.id);modules++;
  insert('knowledge_module','id,course_id,stable_key',[q(id),q(course),q(m.id)]);
  // Worked examples embedded in conceptual text move to explicitly revealed solutions.
  const worked=[],content=m.content.split('\n').filter(line=>{
   if(/例[\d一二三]*[：:]|例\d+[：:]/.test(line)){worked.push(line);return false;}return true;
  }).join('\n');
  const resources=(knowledgeSources[c.id]||[]).filter(r=>r.url.startsWith('https://')).map(r=>({kind:'LINK',label:r.title,url:r.url,fileId:null}));
  insert('module_revision','release_id,module_id,title,content,difficulty,formulas,resources',[q(release),q(id),q(m.title),q(content),m.difficulty,q(JSON.stringify(m.formulas||[])),q(JSON.stringify(resources))]);
  for(const [i,e]of (m.exercises||[]).entries()) addExample(uuid(c.code+':example:v2:'+m.id+':'+i),id,e.question,e.stars,e.answer,e.solution,i);
  for(const [i,line]of worked.entries()) {
   const split=line.indexOf('⇒'),question=split<0?m.title+' · 教学示例':line.slice(0,split);
   addExample(uuid(c.code+':worked:v2:'+m.id+':'+i),id,question, m.difficulty, split<0?line:line.slice(split+1),line,1000+i);
  }
 }
 function addExample(id,module,question,stars,answer,solution,order) {
  examples++;insert('knowledge_example','id,release_id,module_id,stars,question,sort_order',[q(id),q(release),q(module),stars,q(question),order]);
  insert('example_solution','example_id,answer,solution',[q(id),q(answer),q(solution)]);
 }
 if(manual) {
  const outline=initialOutlines.find(o=>o.id===c.id),stages=outline.stages.filter(s=>s.id.startsWith('manual-practice-'));
  const exercises=manualExercises(c.id),groups=new Map(stages.map(s=>[s.part? s.title.replace('实践手册练习 · ',''):s.title,s]));
  const parts=[...groups.keys()];const sections=new Map(stages.map(s=>[s.id,{chapterId:uuid(c.code+':chapter:'+s.id),title:s.title,markdown:'',exercises:[]} ]));
  let lastStage=stages[0];
  for(const section of parseManualSections(manual.markdown)) {
   const local=exercises.filter(e=>e.sectionId===section.id);
   const part=c.id==='database'?(!section.number||section.number<=8?parts[0]:section.number<=14?parts[1]:section.number<=17?parts[2]:parts[3]):section.part||parts[0];
   const stage=local.length?groups.get(local[0].part):(groups.get(section.kind==='part'?section.title:part)||lastStage);
   if(!stage)throw new Error('Missing manual chapter '+part);lastStage=stage;
   const target=sections.get(stage.id),answers=[];
   let text=section.lines.join('\n').replace(/<details>[\s\S]*?<\/details>/g,block=>{answers.push(block.replace(/<\/?details>/g,'').replace(/<summary>[\s\S]*?<\/summary>/g,'').trim());return '';});
   text=text.replace(/### 验收示例\n([\s\S]*?)(?=\n### |$)/g,(_,block)=>{answers.push(block.trim());return '';});
   const example=answers.length?uuid(c.code+':manual-answer:v2:'+section.id):null;
   if(example) addExample(example,uuid(c.code+':module:'+k[0].id),local.length?local.map(e=>e.title).join('\n'):section.title+' · 练习',1,answers.join('\n\n'),answers.join('\n\n'),2000+manualSections);
   target.markdown+='\n\n## '+section.title+'\n\n'+text;manualSections++;
   for(const e of local) {
    if(!stage.items.some(i=>i.id===e.id))throw new Error('Unmapped manual exercise '+e.id);
    target.exercises.push({itemId:uuid(c.code+':chapter:'+stage.id+':item:'+e.id),exampleId:example});manualExercisesCount++;
   }
  }
  const file=uuid(c.code+':manual-file:v2'),key='manuals/'+c.id+'-v2.json',manifest={courseId:course,releaseId:release,sections:[...sections.values()]};
  const data=JSON.stringify(manifest,null,2)+'\n';fs.mkdirSync('backend/src/main/resources/manuals',{recursive:true});fs.writeFileSync('backend/src/main/resources/'+key,data);
  insert('stored_file','id,owner_id,purpose,storage_key,original_name,mime_type,size_bytes,sha256,contains_answers,state',[q(file),'NULL',q('MANUAL'),q('classpath:'+key),q(c.id+'-manual.json'),q('application/json'),Buffer.byteLength(data),q(crypto.createHash('sha256').update(data).digest('hex')),0,q('ACTIVE')]);
  const resource={kind:'FILE',label:manual.label,url:null,fileId:file};
  sql.push(`UPDATE module_revision SET resources=JSON_ARRAY_APPEND(resources,'$',CAST(${q(JSON.stringify(resource))} AS JSON)) WHERE release_id=${q(release)} AND module_id=${q(uuid(c.code+':module:'+k[0].id))};`);
 }
 sql.push(`UPDATE content_release SET state='RETIRED' WHERE id=${q(old)};`);
}
fs.writeFileSync('backend/src/main/resources/db/migration/V3__knowledge_and_manuals.sql',sql.join('\n')+'\n');
console.log(JSON.stringify({modules,examples,manualSections,manualExercises:manualExercisesCount}));
