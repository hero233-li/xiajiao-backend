// Generate the backend request validator's schema snapshot from the shared OpenAPI contract.
// Run from xiajiao/repo: node backend/scripts/generate-business-schemas.cjs ../../frontend/node_modules
const fs=require('node:fs'),path=require('node:path');
const YAML=require(path.join(path.resolve(process.argv[2]||'../../frontend/node_modules'),'yaml'));
const document=YAML.parse(fs.readFileSync('docs/openapi.yaml','utf8'));
function strip(value){
  if(Array.isArray(value))return value.map(strip);
  if(!value||typeof value!=='object')return value;
  return Object.fromEntries(Object.entries(value).filter(([key])=>!['description','example','readOnly'].includes(key)).map(([key,v])=>[key,strip(v)]));
}
const output=JSON.stringify(strip(document.components.schemas));
const target='backend/src/main/resources/business-contract-schemas.json';
if(process.argv.includes('--check')){
  if(fs.readFileSync(target,'utf8').trim()!==output)throw new Error('Backend schema snapshot differs from OpenAPI; regenerate it.');
  console.log('Backend input schemas match OpenAPI.');
}else fs.writeFileSync(target,output+'\n');
