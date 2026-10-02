const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {load,uuid,source}=require('./legacy-content.cjs');
const {initialCourses,initialOutlines} = load(path.join(source,'lib/study.ts'));
const ids = {'00023':'afdac469-0fe4-5007-833c-51a71333967b','02324':'ccd819bb-a48b-5c75-bfbf-e26219023fef','13175':'6450a55a-7d59-5632-b9ad-2a4e170af220','13015':'4c6db663-13a2-5b2b-8cfa-6422a6c37205','13216':'e2dd5f58-a12f-55e0-a691-455ea2233699','13171':'72494a28-2cb8-5df8-8f3f-2ba84b681e0d'};
const q=v=>v==null?'NULL':"'"+String(v).replace(/\\/g,'\\\\').replace(/'/g,"''")+"'";
const sql=['-- Public content copied from zikao-app initialCourses/initialOutlines. No personal progress, payments or scores.',
'-- Exam dates retain the existing project schedule; they are not copied from OpenAPI examples.'];
const insert=(table,columns,values)=>sql.push(`INSERT INTO ${table} (${columns}) VALUES (${values.join(',')});`);
const cycle='0ebdbbfe-d607-54b5-9c21-4e0adade5e4c';
insert('exam_cycle','id,name,start_date,end_date,timezone',[q(cycle),q('2026年10月考试'),q('2026-10-01'),q('2026-10-31'),q('Asia/Shanghai')]);
let chapters=0,items=0;
for(const c of initialCourses.filter(c=>c.current)) {
  const id=ids[c.code],outline=initialOutlines.find(o=>o.id===c.id),release=uuid(c.code+':release:1');
  const sha=crypto.createHash('sha256').update(JSON.stringify(outline)).digest('hex');
  insert('course','id,code,legacy_id,name,course_type,active',[q(id),q(c.code),q(c.id),q(c.name),q(c.type==='理论'?'THEORY':'PRACTICE'),1]);
  insert('cycle_course','cycle_id,course_id,exam_date,starts_at,ends_at',[q(cycle),q(id),q(c.date||null),q(c.time?c.time+':00':null),q(c.endTime?c.endTime+':00':null)]);
  insert('content_release','id,course_id,version_no,state,published_at,source_sha',[q(release),q(id),1,q('PUBLISHED'),'UTC_TIMESTAMP(6)',q(sha)]);
  outline.stages.forEach((s,ci)=>{
    const chapter=uuid(c.code+':chapter:'+s.id);chapters++;
    insert('chapter','id,course_id,stable_key',[q(chapter),q(id),q(s.id)]);
    // Assessment policy is not imported here; imported reading chapters are not asserted eligible for assessment.
    insert('chapter_revision','release_id,chapter_id,course_id,title,sort_order,participates_in_assessment',[q(release),q(chapter),q(id),q(s.title),ci,0]);
    s.items.forEach((i,ii)=>{
      const item=uuid(c.code+':chapter:'+s.id+':item:'+i.id);items++;
      const duration=i.duration?.split(':').map(Number);const minutes=duration?.length===2?Math.max(1,Math.ceil(duration[0]+duration[1]/60)):30;
      const url=i.url||s.sourceUrl;const resource=url?.startsWith('https://')?JSON.stringify({kind:'LINK',label:i.title,url,fileId:null}):null;
      insert('study_item','id,chapter_id,stable_key',[q(item),q(chapter),q(i.id)]);
      insert('item_revision','release_id,item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order',[q(release),q(item),q(chapter),q(i.title),q(resource),minutes,ii]);
    });
  });
}
const target=path.resolve('backend/src/main/resources/db/migration/V2__learning_content.sql');fs.writeFileSync(target,sql.join('\n')+'\n');
console.log(JSON.stringify({courses:6,chapters,items,target}));
