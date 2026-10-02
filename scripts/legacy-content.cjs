// Regenerate only before V2 has been deployed. Later content changes require a new migration.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const crypto = require('node:crypto');
const source = path.resolve(process.argv[2] || '../../zikao-app');
const ts = require(path.resolve(process.argv[3] || '../../frontend/node_modules/typescript/lib/typescript.js'));
const cache = new Map();
function load(filename) {
  if (cache.has(filename)) return cache.get(filename).exports;
  const module = {exports:{}}; cache.set(filename,module);
  const code = ts.transpileModule(fs.readFileSync(filename,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
  new vm.Script('(function(require,module,exports){'+code+'\n})',{filename}).runInThisContext()(name => {
    if (name.endsWith('?raw')) return {default:fs.readFileSync(path.join(source,name.slice(2,-4)),'utf8')};
    if(name.endsWith('.json')) return {default:JSON.parse(fs.readFileSync(path.resolve(path.dirname(filename),name),'utf8'))};
    if (name.startsWith('.')) return load(path.resolve(path.dirname(filename),name+'.ts'));
    throw new Error('Unexpected dependency: '+name);
  },module,module.exports);
  return module.exports;
}
function uuid(key) {
  const b=crypto.createHash('sha1').update('xuexizhitu:learning:v1:'+key).digest().subarray(0,16);b[6]=(b[6]&15)|80;b[8]=(b[8]&63)|128;
  const h=b.toString('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-${h.slice(12,16)}-${h.slice(16,20)}-${h.slice(20)}`;
}

module.exports={load,uuid,source};
