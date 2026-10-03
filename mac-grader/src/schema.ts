const str = { type: "string" };
const array = (items: unknown) => ({ type: "array", items });
const object = (properties: Record<string, unknown>) => ({
  type: "object",
  properties,
  required: Object.keys(properties),
  additionalProperties: false,
});
export const resultSchema = object({
  rubricId: str,
  answers: array(
    object({
      number: str,
      recognizedAnswer: str,
      points: array(
        object({ pointId: str, score: { type: "number" }, reason: str }),
      ),
      pages: array({ type: "integer" }),
      reviewItems: array(str),
    }),
  ),
  reviewItems: array(str),
});
export const rubricSchema = object({
  questions: array(
    object({
      number: str,
      stem: str,
      referenceAnswer: str,
      maximum: { type: "number" },
      points: array(
        object({ id: str, description: str, maximum: { type: "number" } }),
      ),
    }),
  ),
});
export interface Page {
  fileId: string;
  pageNo: number;
  mimeType: string;
  sha256: string;
  sizeBytes: number;
}
export interface Claim {
  taskId: string;
  kind: "GRADE" | "RUBRIC";
  leaseToken: string;
  leaseUntil: string;
  deadline: string;
  inputs: {
    courseId: string;
    cycleId: string;
    practice: unknown;
    rubric: { id: string; document: { questions: any[] } } | null;
    pages: Page[];
  };
}
export function validateResult(result: any, claim: Claim) {
  if (claim.kind === "RUBRIC") {
    if (
      !Array.isArray(result?.questions) ||
      result.questions.length < 1 ||
      result.questions.length > 200
    )
      throw new Error("INVALID_RUBRIC");
    let total = 0;
    const numbers = new Set();
    for (const q of result.questions) {
      if (
        numbers.has(q.number) ||
        typeof q.stem !== "string" ||
        typeof q.referenceAnswer !== "string" ||
        !Array.isArray(q.points)
      )
        throw new Error("INVALID_RUBRIC");
      numbers.add(q.number);
      let sum = 0;
      const ids = new Set();
      for (const p of q.points) {
        if (
          ids.has(p.id) ||
          typeof p.description !== "string" ||
          !Number.isFinite(p.maximum) ||
          p.maximum <= 0
        )
          throw new Error("INVALID_RUBRIC");
        ids.add(p.id);
        sum += p.maximum;
      }
      if (Math.abs(sum - q.maximum) > 0.00001)
        throw new Error("INVALID_RUBRIC");
      total += q.maximum;
    }
    if (Math.abs(total - 100) > 0.00001)
      throw new Error("INVALID_RUBRIC_TOTAL");
    return;
  }
  if (
    result?.rubricId !== claim.inputs.rubric?.id ||
    !Array.isArray(result.answers) ||
    !Array.isArray(result.reviewItems)
  )
    throw new Error("INVALID_RESULT");
  const questions = claim.inputs.rubric!.document.questions;
  if (result.answers.length !== questions.length)
    throw new Error("INVALID_QUESTION_COVERAGE");
  const numbers = new Set();
  for (const a of result.answers) {
    const q = questions.find((q) => q.number === a.number);
    if (
      !q ||
      numbers.has(a.number) ||
      typeof a.recognizedAnswer !== "string" ||
      !Array.isArray(a.points) ||
      !Array.isArray(a.pages) ||
      !Array.isArray(a.reviewItems)
    )
      throw new Error("INVALID_RESULT");
    numbers.add(a.number);
    const ids = new Set();
    if (a.points.length !== q.points.length)
      throw new Error("INVALID_POINT_COVERAGE");
    for (const p of a.points) {
      const standard = q.points.find((x: any) => x.id === p.pointId);
      if (
        !standard ||
        ids.has(p.pointId) ||
        !Number.isFinite(p.score) ||
        p.score < 0 ||
        p.score > standard.maximum ||
        Math.abs(p.score * 100 - Math.round(p.score * 100)) > 0.00001 ||
        typeof p.reason !== "string" ||
        (p.score < standard.maximum && !p.reason.trim())
      )
        throw new Error("INVALID_SCORE");
      ids.add(p.pointId);
    }
    if (
      a.pages.some(
        (n: any) =>
          !Number.isInteger(n) || n < 1 || n > claim.inputs.pages.length,
      ) ||
      (!a.pages.length && !a.reviewItems.length)
    )
      throw new Error("INVALID_PAGE");
  }
}

export function validateClaim(claim: Claim) {
  const uuid =
    /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (
    !claim ||
    !uuid.test(claim.taskId) ||
    !uuid.test(claim.leaseToken) ||
    !["GRADE", "RUBRIC"].includes(claim.kind) ||
    !Number.isFinite(Date.parse(claim.deadline)) ||
    !Number.isFinite(Date.parse(claim.leaseUntil)) ||
    !Array.isArray(claim.inputs?.pages)
  )
    throw new Error("INVALID_CLAIM");
  const pages = claim.inputs.pages;
  if (!pages.length || pages.length > (claim.kind === "GRADE" ? 20 : 2))
    throw new Error("INVALID_CLAIM_PAGE_COUNT");
  const ids = new Set();
  for (const p of pages) {
    if (
      !uuid.test(p.fileId) ||
      ids.has(p.fileId) ||
      !/^[0-9a-f]{64}$/i.test(p.sha256) ||
      !Number.isInteger(p.sizeBytes) ||
      p.sizeBytes < 1 ||
      p.sizeBytes > 8 * 1024 * 1024 ||
      !(
        claim.kind === "GRADE"
          ? ["image/jpeg", "image/png"]
          : ["application/pdf"]
      ).includes(p.mimeType)
    )
      throw new Error("INVALID_CLAIM_MATERIAL");
    ids.add(p.fileId);
  }
}
