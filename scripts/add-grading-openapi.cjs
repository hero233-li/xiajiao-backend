const fs = require("node:fs"),
  path = require("node:path");
const YAML = require(
  path.resolve(process.argv[2] || "../frontend/node_modules", "yaml"),
);
const file = "docs/openapi.yaml";
const spec = YAML.parse(fs.readFileSync(file, "utf8"));
const schemas = spec.components.schemas;
const s = { type: "string" },
  id = { type: "string", format: "uuid" },
  bool = { type: "boolean" },
  n = { type: "integer" },
  decimal = { type: "number", minimum: 0, maximum: 100, multipleOf: 0.01 },
  instant = { type: "string", format: "date-time" };
const ref = (name) => ({ $ref: "#/components/schemas/Local" + name });
const arr = (items) => ({ type: "array", items });
const nullable = (x) =>
  x.$ref
    ? { anyOf: [x, { nullable: true, enum: [null] }] }
    : { ...x, nullable: true };
const object = (props) => ({
  type: "object",
  additionalProperties: false,
  required: Object.keys(props),
  properties: props,
});
const define = (name, props) => (schemas["Local" + name] = object(props));
define("SubmissionWrite", { courseId: id, cycleId: id, paperId: id });
define("Page", {
  fileId: id,
  pageNo: { ...n, minimum: 1, maximum: 20 },
  name: s,
  mimeType: s,
  sizeBytes: n,
  sha256: s,
});
define("Submission", {
  id,
  courseId: id,
  cycleId: id,
  paperId: id,
  revision: n,
  pages: arr(ref("Page")),
});
define("PageOrder", {
  expectedRevision: { ...n, minimum: 0 },
  fileIds: { ...arr(id), minItems: 1, maxItems: 20 },
});
define("Point", {
  id: s,
  description: s,
  maximum: { ...decimal, minimum: 0.01 },
});
define("Question", {
  number: s,
  stem: s,
  referenceAnswer: s,
  maximum: { ...decimal, minimum: 0.01 },
  points: { ...arr(ref("Point")), minItems: 1, maxItems: 100 },
});
define("RubricDocument", {
  questions: { ...arr(ref("Question")), minItems: 1, maxItems: 200 },
});
define("Rubric", {
  id,
  paperId: id,
  version: n,
  state: { ...s, enum: ["DRAFT", "PUBLISHED"] },
  document: ref("RubricDocument"),
});
define("Apply", {
  practicedOn: { ...s, format: "date" },
  minutes: { ...n, minimum: 1, maximum: 1440 },
  limitMinutes: { ...n, minimum: 1, maximum: 1440 },
  complete: bool,
  closedBook: bool,
  answersSeenBefore: bool,
  expectedRevision: { ...n, minimum: 0 },
});
define("Earned", { pointId: s, score: decimal, reason: s });
define("Answer", {
  number: s,
  recognizedAnswer: s,
  points: arr(ref("Earned")),
  pages: arr(n),
  reviewItems: arr(s),
});
define("Result", {
  rubricId: id,
  answers: { ...arr(ref("Answer")), minItems: 1, maxItems: 200 },
  reviewItems: arr(s),
});
define("Task", {
  id,
  kind: { ...s, enum: ["GRADE", "RUBRIC"] },
  state: { ...s, enum: ["QUEUED", "GRADING", "REVIEW", "COMPLETED", "FAILED"] },
  submissionId: nullable(id),
  rubricId: nullable(id),
  model: nullable(s),
  result: nullable(ref("Result")),
  rubricDraft: nullable(ref("RubricDocument")),
  scoreId: nullable(id),
  error: nullable(s),
  leaseUntil: nullable(instant),
  workerOnline: bool,
  workerReason: nullable(s),
});
define("Worker", {
  id,
  label: s,
  online: bool,
  paused: bool,
  reason: nullable(s),
  revoked: bool,
});
define("PairWrite", { label: { ...s, minLength: 1, maxLength: 100 } });
define("Pair", { id, token: s });
define("Inputs", {
  courseId: id,
  cycleId: id,
  practice: nullable(ref("Apply")),
  rubric: nullable(ref("Rubric")),
  pages: arr(ref("Page")),
});
define("Claim", {
  taskId: id,
  kind: { ...s, enum: ["GRADE", "RUBRIC"] },
  leaseToken: id,
  leaseUntil: instant,
  deadline: instant,
  inputs: ref("Inputs"),
});
define("Material", {
  fileId: id,
  pageNo: n,
  mimeType: s,
  sha256: s,
  contentBase64: s,
});
define("Callback", {
  leaseToken: id,
  model: { ...s, minLength: 1, maxLength: 100 },
  result: nullable(ref("Result")),
  rubric: nullable(ref("RubricDocument")),
});
define("Lease", { leaseToken: id });
define("Heartbeat", { paused: bool, reason: { ...s, maxLength: 500 } });
define("Failure", {
  leaseToken: id,
  reason: { ...s, minLength: 1, maxLength: 500 },
  retryable: bool,
  pause: bool,
});
const uuid = "11111111-1111-4111-8111-111111111111";
function sample(schema) {
  if (schema.anyOf) return sample(schema.anyOf[schema.anyOf.length - 1]);
  if (schema.nullable) return null;
  if (schema.$ref) return sample(schemas[schema.$ref.split("/").pop()]);
  if (schema.enum) return schema.enum[0];
  if (schema.type === "object")
    return Object.fromEntries(
      Object.entries(schema.properties).map(([k, v]) => [k, sample(v)]),
    );
  if (schema.type === "array")
    return Array.from({ length: schema.minItems || 0 }, () =>
      sample(schema.items),
    );
  if (schema.type === "boolean") return false;
  if (schema.type === "integer" || schema.type === "number")
    return schema.minimum || 0;
  if (schema.format === "uuid") return uuid;
  if (schema.format === "date") return "2026-10-03";
  if (schema.format === "date-time") return "2026-10-03T00:00:00Z";
  return "示例";
}
const query = (name, type = id) => ({
  in: "query",
  name,
  required: true,
  schema: type,
});
let sequence = 0;
function op(
  method,
  url,
  input,
  output,
  description,
  params = [],
  worker = false,
  multipart = false,
) {
  const full = (worker ? "/grading-worker" : "/grading") + url;
  const responseSchema = output === null ? { nullable: true } : output;
  const operation = {
    operationId: "localGrading" + ++sequence,
    tags: [worker ? "LocalGradingWorker" : "LocalGrading"],
    summary: description,
    description,
    parameters: [
      ...Array.from(full.matchAll(/\{(.*?)\}/g), (m) => ({
        name: m[1],
        in: "path",
        required: true,
        schema: id,
      })),
      ...params,
    ],
    security: worker ? [{ WorkerPairing: [] }] : [{ bearerAuth: [] }],
    responses: {
      200: {
        description: "成功",
        content: {
          "application/json": {
            schema: object({
              code: { ...n, enum: [0] },
              data: responseSchema,
              message: s,
            }),
            example: {
              code: 0,
              data: output === null ? null : sample(output),
              message: "ok",
            },
          },
        },
      },
      400: { description: "参数或结果不合法" },
      401: { description: "登录或配对凭证失效" },
      403: { description: "权限或成绩写入资格不足" },
      404: { description: "资源不属于当前用户或不存在" },
      409: { description: "答卷版本冲突、重复结果不同或租约失效" },
      422: { description: "缺少已发布评分标准、分值或关联不合法" },
    },
  };
  if (input)
    operation.requestBody = {
      required: true,
      content: {
        [multipart ? "multipart/form-data" : "application/json"]: {
          schema: input,
        },
      },
    };
  (spec.paths[full] ||= {})[method] = operation;
}
op(
  "get",
  "/submissions",
  null,
  arr(ref("Submission")),
  "列出本人成绩周期答卷（最近100份）",
  [query("courseId"), query("cycleId")],
);
op(
  "post",
  "/submissions",
  ref("SubmissionWrite"),
  ref("Submission"),
  "新建独立答卷；复用成绩写入权限",
);
op("get", "/submissions/{id}", null, ref("Submission"), "读取本人答卷");
op(
  "post",
  "/submissions/{id}/pages",
  object({ file: { type: "string", format: "binary" } }),
  ref("Submission"),
  "上传真实JPG/PNG；8MiB每张，最多20张",
  [query("expectedRevision", n)],
  false,
  true,
);
op(
  "put",
  "/submissions/{id}/pages",
  ref("PageOrder"),
  ref("Submission"),
  "调整全部页序；乐观版本校验",
);
op(
  "delete",
  "/submissions/{id}/pages/{file}",
  null,
  ref("Submission"),
  "移除答卷页；任务已冻结的实体文件保留",
  [query("expectedRevision", n)],
);
op(
  "post",
  "/submissions/{id}/tasks",
  ref("Apply"),
  ref("Task"),
  "申请批改，冻结页序、哈希、评分标准及练习信息；同版本重复申请幂等",
);
op(
  "get",
  "/submissions/{id}/tasks",
  null,
  arr(ref("Task")),
  "答卷任务历史（最近100项）",
);
op(
  "get",
  "/tasks/{id}/inputs",
  null,
  ref("Inputs"),
  "本人读取任务冻结输入及评分标准",
);
op(
  "get",
  "/tasks/{id}/materials/{file}",
  null,
  ref("Material"),
  "本人读取本次原始答卷供核对",
);
op(
  "get",
  "/tasks/{id}",
  null,
  ref("Task"),
  "任务状态、逐题结果及本人工作程序在线原因",
);
op(
  "post",
  "/tasks/{id}/review",
  ref("Result"),
  ref("Task"),
  "本人核对；全部待核对项处理后原子生成CODEX成绩",
);
op(
  "get",
  "/rubrics",
  null,
  arr(ref("Rubric")),
  "已发布评分标准；管理员同时可读草稿",
  [query("courseId"), query("cycleId"), query("paperId")],
);
op(
  "post",
  "/rubrics",
  ref("RubricDocument"),
  ref("Rubric"),
  "管理员建立评分标准新版本草稿，总分100",
  [query("courseId"), query("paperId")],
);
op(
  "put",
  "/rubrics/{id}",
  ref("RubricDocument"),
  ref("Rubric"),
  "管理员修改草稿；发布后不可变",
);
op(
  "post",
  "/rubrics/{id}/publish",
  null,
  ref("Rubric"),
  "管理员人工核对并发布评分标准",
);
op(
  "post",
  "/rubric-tasks",
  null,
  ref("Task"),
  "管理员请求本地从试卷和答案PDF生成草稿；不自动发布",
  [query("courseId"), query("cycleId"), query("paperId")],
);
op(
  "get",
  "/workers",
  null,
  arr(ref("Worker")),
  "本人配对设备及心跳状态（90秒离线）",
);
op(
  "post",
  "/workers",
  ref("PairWrite"),
  ref("Pair"),
  "创建仅本人任务权限的随机凭证；服务器仅存SHA256，明文只返回一次",
);
op(
  "delete",
  "/workers/{id}",
  null,
  null,
  "本人撤销专用凭证；轮换使用新增配对后撤销旧凭证",
);
op(
  "post",
  "/heartbeat",
  ref("Heartbeat"),
  null,
  "工作程序在线、暂停及原因",
  [],
  true,
);
op(
  "post",
  "/claim",
  object({}),
  nullable(ref("Claim")),
  "原子串行领取本人任务，120秒租约，最长15分钟，最多3次处理",
  [],
  true,
);
op(
  "post",
  "/tasks/{id}/renew",
  ref("Lease"),
  ref("Claim"),
  "本次领取凭据续租，不能延长15分钟截止",
  [],
  true,
);
op(
  "get",
  "/tasks/{id}/materials/{file}",
  null,
  ref("Material"),
  "凭有效领取凭据读取冻结材料及哈希",
  [{ in: "header", name: "X-Claim-Token", required: true, schema: id }],
  true,
);
op(
  "post",
  "/tasks/{id}/result",
  ref("Callback"),
  ref("Task"),
  "验证全部题号和评分点、独立总分；幂等回传；原子结果/成绩/照片/状态",
  [],
  true,
);
op(
  "post",
  "/tasks/{id}/failure",
  ref("Failure"),
  ref("Task"),
  "失败报告、最多两次重试；授权/用量问题暂停领取",
  [],
  true,
);
spec.components.securitySchemes.WorkerPairing = {
  type: "apiKey",
  in: "header",
  name: "X-Worker-Token",
  description:
    "可撤销、可轮换且仅本人任务处理范围的配对凭证；不是管理员密码/JWT",
};
spec.info.description = spec.info.description.replace(
  "不接模型，不开放新批改/补题/快捷模块",
  "新增本地Codex答卷批改；不开放补题/快捷模块",
);
for (const schema of Object.values(schemas)) {
  if (
    schema.properties?.source?.enum?.includes("MANUAL") &&
    !schema.properties.source.enum.includes("CODEX")
  )
    schema.properties.source.enum.push("CODEX");
}
for (const item of Object.values(spec.paths))
  for (const operation of Object.values(item))
    if (operation.description)
      operation.description = operation.description.replace(
        "不开放新的批改/补题任务。",
        "历史归档只读；新批改使用 /grading，补题仍未开放。",
      );
fs.writeFileSync(file, YAML.stringify(spec, { lineWidth: 0 }));
for (const dest of ["../docs/openapi.yaml", "../frontend/docs/openapi.yaml"])
  fs.copyFileSync(file, dest);
console.log(`${sequence} grading operations added`);
