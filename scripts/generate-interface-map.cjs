// Generate reviewable old-to-new paths and real frontend dependencies from the contract and sources.
const fs=require('node:fs'),path=require('node:path');
const modules=path.resolve(process.argv[2]||'../frontend/node_modules');
const YAML=require(path.join(modules,'yaml'));
const root=path.resolve(__dirname,'..'),front=path.resolve(root,'../frontend');
const doc=YAML.parse(fs.readFileSync(path.join(root,'docs/openapi.yaml'),'utf8'));
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>{const f=path.join(dir,e.name);if(e.isDirectory())return e.name==='generated'?[]:walk(f);return /\.(ts|tsx)$/.test(e.name)&&!e.name.includes('.test.')?[f]:[];});}
const sources=walk(path.join(front,'src')).map(f=>[path.relative(front,f),fs.readFileSync(f,'utf8')]);
let text=`# 接口映射与前端依赖\n\n既有104个操作保留原HTTP方法、路径、成功响应envelope、分页及统计含义；不是运行两套旧新后端。两个新增操作列在下表。OpenAPI快照位于后端和前端各自的docs目录，可独立审查与生成。\n\n行为补充：创建成绩可传Idempotency-Key（UUID）；同键重放原响应，同键不同请求409。目录和计划完成沿用clientMutationId并实现同样重放。成绩修订继续使用expectedRevision，新增修订查询。草稿写入/发布可传If-Match整数版本，旧请求省略时保持兼容；管理UI尚不存在，不宣称已适配未实现页面。五周新建/完整编辑缺少每科复习模板时422，实际计划页面展示服务错误，不自动补任务。\n\n实际前端适配：api/exams.ts发送稳定创建键，ScoreDialog展示真实修订；私有下载和图片存档沿用原契约，代码生成更新类型/方法；修复成绩页按钮被浮层挡住的真实点击问题；历史原始JSON仅作为归档显示，不进入新请求模型。\n\n|旧方法与路径|当前方法与路径|操作|实际前端调用源（不含生成代码/测试）|\n|---|---|---|---|\n`;
let count=0;
for(const[route,operations]of Object.entries(doc.paths))for(const[method,op]of Object.entries(operations)){
 if(!op.operationId)continue;count++;
 const uses=sources.filter(([,s])=>new RegExp('\\b'+op.operationId+'\\b').test(s)).map(([p])=>p);
 const added=['getReadiness','listScoreRevisions'].includes(op.operationId);
 text+=`|${added?'新增':`${method.toUpperCase()} ${route}`}|${method.toUpperCase()} ${route}|${op.operationId}|${uses.join('、')||'无实际页面调用；已生成客户端/后端能力'}|\n`;
}
text+=`\n共${count}个操作。安全规则与数据归属见业务基线；管理API只有管理员可用，私有资料需额外课程资格，个人成绩/计划/笔记/历史以安全上下文owner隔离。测试样本逐一验证实际响应，不从Mock示例推断实现能力。\n`;
fs.writeFileSync(path.join(root,'docs/refactor/04-interface-map.md'),text);
