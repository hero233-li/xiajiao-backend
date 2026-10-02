const fs=require('node:fs'),crypto=require('node:crypto');
const {load,uuid,source}=require('./legacy-content.cjs');
const {practiceQuestions}=load(source+'/lib/practice.ts'),{initialOutlines}=load(source+'/lib/study.ts');
const ids={calculus:['00023','afdac469-0fe4-5007-833c-51a71333967b'],discrete:['02324','ccd819bb-a48b-5c75-bfbf-e26219023fef'],linear:['13175','6450a55a-7d59-5632-b9ad-2a4e170af220'],systems:['13015','4c6db663-13a2-5b2b-8cfa-6422a6c37205']};
const q=v=>v==null?'NULL':"'"+String(v).replace(/\\/g,'\\\\').replace(/'/g,"''")+"'";
const sql=['-- V4 imports deduplicated practice questions into new releases. Original eligibility remains unreviewed (false).',
"ALTER TABLE practice_submission ADD COLUMN result_snapshot json, ADD CONSTRAINT ck_practice_result_snapshot CHECK (result_snapshot IS NULL OR JSON_TYPE(result_snapshot) = 'OBJECT');"];
const insert=(t,c,v)=>sql.push(`INSERT INTO ${t} (${c}) VALUES (${v.join(',')});`);
const norm=x=>x.normalize('NFKC').replace(/\s+/g,'').trim();
const seen=new Map(),report={input:0,imported:0,duplicates:[],rejected:[],courses:{}};
const v3=fs.readFileSync('backend/src/main/resources/db/migration/V3__knowledge_and_manuals.sql','utf8');
for(const [legacy,questions]of Object.entries(practiceQuestions)) {
 const[code,course]=ids[legacy],old=uuid(code+':release:2'),release=uuid(code+':release:3');
 const sha=crypto.createHash('sha256').update(JSON.stringify(questions)).digest('hex');
 insert('content_release','id,course_id,version_no,state,published_at,source_sha',[q(release),q(course),3,q('PUBLISHED'),'UTC_TIMESTAMP(6)',q(sha)]);
 sql.push(`INSERT INTO chapter_revision SELECT ${q(release)},chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id=${q(old)};`);
 sql.push(`INSERT INTO item_revision SELECT ${q(release)},item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order FROM item_revision WHERE release_id=${q(old)};`);
 sql.push(`INSERT INTO module_revision SELECT ${q(release)},module_id,title,content,difficulty,formulas,resources FROM module_revision WHERE release_id=${q(old)};`);
 for(const match of v3.matchAll(/INSERT INTO knowledge_example .*? VALUES \('([^']+)','([^']+)'/g))if(match[2]===old) {
  const previous=match[1],next=uuid(previous+':release:3');
  sql.push(`INSERT INTO knowledge_example SELECT ${q(next)},${q(release)},module_id,stars,question,sort_order FROM knowledge_example WHERE id=${q(previous)};`);
  sql.push(`INSERT INTO example_solution SELECT ${q(next)},answer,solution FROM example_solution WHERE example_id=${q(previous)};`);
 }
 const outlines=initialOutlines.find(o=>o.id===legacy).stages,chapterMap=new Map(outlines.map(s=>[s.title,s.id]));
 if(legacy==='calculus')chapterMap.set('前置基础 · 一元函数微积分','foundation-24-40');
 if(legacy==='systems')for(const[ci,title]of [...new Set(questions.map(x=>x.chapter))].entries()) {
  const key='practice-systems-'+(ci+1),chapter=uuid(code+':chapter:'+key);chapterMap.set(title,key);
  insert('chapter','id,course_id,stable_key',[q(chapter),q(course),q(key)]);
  insert('chapter_revision','release_id,chapter_id,course_id,title,sort_order,participates_in_assessment',[q(release),q(chapter),q(course),q(title),outlines.length+ci,0]);
 }
 const points=new Set();let imported=0;
 for(const[index,item]of questions.entries()) {
  report.input++;
  if(!chapterMap.has(item.chapter)||!Array.isArray(item.options)||item.options.length<2||!Number.isInteger(item.answer)||item.answer<0||item.answer>=item.options.length||!item.stem||!item.explanation||item.difficulty<1||item.difficulty>5)throw new Error('Invalid question '+item.id);
  if(new Set(item.options.map(norm)).size!==item.options.length){report.rejected.push({legacyId:item.id,reason:'选项标准化后重复，暂不导入'});continue;}
  const fingerprint=crypto.createHash('sha256').update(JSON.stringify([norm(item.stem),item.options.map(norm).sort()])).digest('hex');
  if(seen.has(fingerprint)) {
   const previous=seen.get(fingerprint);if(previous.answer!==norm(item.options[item.answer]))throw new Error('Conflicting duplicate '+item.id);
   report.duplicates.push({legacyId:item.id,canonicalLegacyId:previous.legacyId,questionId:previous.questionId});continue;
  }
  const chapter=uuid(code+':chapter:'+chapterMap.get(item.chapter)),question=uuid(code+':question:'+item.id),revision=uuid(code+':question-revision:3:'+item.id);
  seen.set(fingerprint,{legacyId:item.id,questionId:question,answer:norm(item.options[item.answer])});
  insert('question','id,course_id,chapter_id,original_key,content_fingerprint,legacy_id,mode,eligible_original',[q(question),q(course),q(chapter),q('legacy:'+code+':'+item.id),q(fingerprint),q(item.id),q(item.mode.toUpperCase()),0]);
  insert('question_revision','id,release_id,question_id,stem,options,difficulty,source_locator,sort_order',[q(revision),q(release),q(question),q(item.stem),q(JSON.stringify(item.options)),item.difficulty,q(item.paper+' · '+item.topic),index]);
  insert('question_solution','revision_id,correct_option,explanation',[q(revision),item.answer,q(item.explanation)]);
  const key=item.topic,point=uuid(code+':chapter:'+chapterMap.get(item.chapter)+':point:'+key);
  if(!points.has(point)) {
   if(key.length>150)throw new Error('Topic too long');points.add(point);
   insert('knowledge_point','id,chapter_id,stable_key,title',[q(point),q(chapter),q(key),q(key)]);
   insert('release_point','release_id,chapter_id,point_id,title_snapshot',[q(release),q(chapter),q(point),q(key)]);
  }
  insert('question_point','revision_id,point_id',[q(revision),q(point)]);imported++;report.imported++;
 }
 report.courses[code]={input:questions.length,imported,points:points.size};
 sql.push(`UPDATE content_release SET state='RETIRED' WHERE id=${q(old)};`);
}
fs.writeFileSync('backend/src/main/resources/db/migration/V4__practice_bank.sql',sql.join('\n')+'\n');
fs.writeFileSync('docs/题库迁移去重报告.json',JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({input:report.input,imported:report.imported,duplicates:report.duplicates.length,rejected:report.rejected.length,courses:report.courses}));
