// Run from xiajiao/repo after mvn -Pmysql-it verify. Uses the existing frontend validation tools.
const fs=require('node:fs'),path=require('node:path');
const modules=path.resolve(process.argv[2]||'../../frontend/node_modules');
const YAML=require(path.join(modules,'yaml'));
const Ajv=require(path.join(modules,'ajv')).default;
const formats=require(path.join(modules,'ajv-formats')).default;
const doc=YAML.parse(fs.readFileSync('docs/openapi.yaml','utf8'));
function normalize(value) {
  if(Array.isArray(value)) return value.map(normalize);
  if(!value||typeof value!=='object') return value;
  const result={};for(const [k,v] of Object.entries(value)) if(!['nullable','example','discriminator','xml'].includes(k)) result[k]=normalize(v);
  if(value.nullable && typeof result.type==='string') {result.type=[result.type,'null'];if(result.enum&&!result.enum.includes(null))result.enum.push(null);}
  return result;
}
const ajv=new Ajv({strict:false,allErrors:true});formats(ajv);
ajv.addSchema(normalize({components:doc.components}),'contract');
const samplePath=process.argv[3]||'backend/target/learning-contract-samples.json';
const all=samplePath==='--all';
const expected=Number(process.argv[4]||(all?98:12));
const samples=all?['learning','content','practice','assessment','remaining'].flatMap(name=>JSON.parse(fs.readFileSync(`backend/target/${name}-contract-samples.json`,'utf8'))):JSON.parse(fs.readFileSync(samplePath,'utf8'));
const routes=Object.entries(doc.paths).sort(([a],[b])=>Number(a.includes('{'))-Number(b.includes('{')));
const validated=new Set();
for(const sample of samples) {
  const match=routes.find(([route,ops])=>ops[sample.method]&&new RegExp('^'+route.replace(/\{[^}]+\}/g,'[^/]+')+'$').test(sample.path));
  if(!match)throw new Error('Unknown operation: '+sample.method+' '+sample.path);
  const schema=match[1][sample.method].responses[String(sample.status||200)].content['application/json'].schema;
  const validate=ajv.compile({$ref:'contract'+schema.$ref});
  if(!validate(sample.body))throw new Error(`${sample.method} ${sample.path}: ${JSON.stringify(validate.errors)}`);
  validated.add(sample.method+' '+match[0]);
}
if(all){const publicIds=new Set(['login','getCurrentUser','logout','refreshTokens','registerUser','getHealth']);for(const [route,operations]of routes)for(const [method,op]of Object.entries(operations))if(op.operationId&&!publicIds.has(op.operationId)&&!validated.has(method+' '+route))throw new Error('Missing business response: '+method+' '+route);}
if(validated.size!==expected)throw new Error(`Expected ${expected} operations, got ${validated.size}`);
console.log(`Validated ${samples.length} real responses across ${validated.size} operations against OpenAPI.`);
