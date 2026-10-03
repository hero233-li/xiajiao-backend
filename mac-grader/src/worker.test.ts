import test from "node:test";
import assert from "node:assert/strict";
import { createServer } from "node:http";
import {
  mkdtemp,
  writeFile,
  readFile,
  chmod,
  stat,
  rm,
  readdir,
} from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createHash } from "node:crypto";
import { Worker, atomic, ApiFault } from "./worker.js";
import { validateResult, type Claim } from "./schema.js";
const uuid = "11111111-1111-4111-8111-111111111111";
const claim: Claim = {
  taskId: uuid,
  kind: "GRADE",
  leaseToken: uuid,
  leaseUntil: new Date(Date.now() + 120000).toISOString(),
  deadline: new Date(Date.now() + 900000).toISOString(),
  inputs: {
    courseId: uuid,
    cycleId: uuid,
    practice: {},
    rubric: {
      id: uuid,
      document: {
        questions: [
          {
            number: "1",
            stem: "7+5",
            referenceAnswer: "12",
            maximum: 100,
            points: [{ id: "answer", maximum: 100 }],
          },
        ],
      },
    },
    pages: [
      {
        fileId: uuid,
        pageNo: 1,
        mimeType: "image/png",
        sha256: "a".repeat(64),
        sizeBytes: 1,
      },
    ],
  },
};
const result = {
  rubricId: uuid,
  answers: [
    {
      number: "1",
      recognizedAnswer: "12",
      points: [{ pointId: "answer", score: 100, reason: "" }],
      pages: [1],
      reviewItems: [],
    },
  ],
  reviewItems: [],
};
const wait = async (f: () => Promise<boolean>, timeout = 15000) => {
  const start = Date.now();
  while (!(await f())) {
    if (Date.now() - start > timeout) throw new Error("test timeout");
    await new Promise((r) => setTimeout(r, 100));
  }
};
test("validation rejects invalid coverage, range, duplicate points and unsupported page", () => {
  validateResult(result, claim);
  for (const mutation of [
    (r: any) => (r.answers[0].points[0].score = 101),
    (r: any) => (r.answers = []),
    (r: any) => (r.answers[0].pages = [2]),
    (r: any) => r.answers[0].points.push(r.answers[0].points[0]),
    (r: any) => (r.rubricId = "wrong"),
  ]) {
    const r = structuredClone(result);
    mutation(r);
    assert.throws(() => validateResult(r, claim));
  }
});
test("non HTTPS server is rejected except explicit loopback test", () => {
  assert.throws(
    () =>
      new Worker({
        server: "http://example.com",
        tokenFile: "x",
        stateDir: "x",
        codexBinary: "x",
      }),
  );
  assert.throws(
    () =>
      new Worker({
        server: "https://user:password@example.com",
        tokenFile: "x",
        stateDir: "x",
        codexBinary: "x",
      }),
  );
});
test("completed result is retried after restart without invoking Codex or claiming again", async () => {
  const root = await mkdtemp(join(tmpdir(), "xzt-worker-test-"));
  const token = join(root, "token");
  await writeFile(token, "dedicated-token", { mode: 0o600 });
  let rejectCallback = true,
    callbacks = 0,
    claims = 0,
    heartbeats = 0;
  const server = createServer(async (req, res) => {
    let body = "";
    for await (const b of req) body += b;
    assert.equal(req.headers["x-worker-token"], "dedicated-token");
    if (req.url?.endsWith("/result")) {
      callbacks++;
      assert.deepEqual(JSON.parse(body).result, result);
      if (rejectCallback) {
        res.writeHead(503);
        res.end("{}");
        return;
      }
      res.end(
        JSON.stringify({
          code: 0,
          data: { state: "COMPLETED" },
          message: "ok",
        }),
      );
      return;
    }
    if (req.url?.endsWith("/heartbeat")) heartbeats++;
    if (req.url?.endsWith("/claim")) claims++;
    res.end(JSON.stringify({ code: 0, data: null, message: "ok" }));
  });
  await new Promise<void>((r) => server.listen(0, "127.0.0.1", r));
  const address = server.address() as { port: number };
  const state = join(root, "state");
  await import("node:fs/promises").then((fs) =>
    fs.mkdir(join(state, uuid), { recursive: true, mode: 0o700 }),
  );
  await writeFile(join(state, uuid, "answer.png"), "preserved material", {
    mode: 0o600,
  });
  await atomic(join(state, uuid, "claim.json"), claim);
  await atomic(join(state, uuid, "model-result.json"), {
    output: result,
    model: "mock",
  }); // crash before pending.json is written
  const config = {
    server: `http://127.0.0.1:${address.port}`,
    allowLocalHttp: true,
    tokenFile: token,
    stateDir: state,
    codexBinary: "/must/not/be/called",
  };
  const first = new Worker(config);
  const run = first.run();
  await wait(async () => callbacks === 3);
  first.stop();
  await run;
  assert.equal(claims, 0);
  assert.equal(callbacks, 3);
  assert.equal(
    await readFile(join(state, uuid, "answer.png"), "utf8"),
    "preserved material",
  );
  rejectCallback = false;
  const next = new Worker(config);
  const restarted = next.run();
  await wait(
    async () =>
      !(await stat(join(state, "pending.json"))
        .then(() => true)
        .catch(() => false)),
  );
  next.stop();
  await restarted;
  assert.equal(callbacks, 4);
  assert.equal(claims, 0);
  assert.ok(heartbeats >= 2);
  assert.equal(
    await stat(join(state, uuid))
      .then(() => true)
      .catch(() => false),
    false,
  );
  await new Promise<void>((r) => server.close(() => r()));
  await rm(root, { recursive: true, force: true });
});
test("pairing revoked is not retried as temporary network failure", async () => {
  let calls = 0;
  const server = createServer((req, res) => {
    calls++;
    res.writeHead(401);
    res.end("{}");
  });
  await new Promise<void>((r) => server.listen(0, "127.0.0.1", r));
  const address = server.address() as { port: number };
  const w = new Worker({
    server: `http://127.0.0.1:${address.port}`,
    allowLocalHttp: true,
    tokenFile: "x",
    stateDir: tmpdir(),
    codexBinary: "x",
  });
  await assert.rejects(
    w.request("/claim", {}),
    (e: unknown) => e instanceof ApiFault && e.status === 401,
  );
  assert.equal(calls, 1);
  await new Promise<void>((r) => server.close(() => r()));
});
test("claim result validation accepts explicit blank zero but requires review for no page", () => {
  const r = structuredClone(result);
  r.answers[0].recognizedAnswer = "";
  r.answers[0].points[0].score = 0;
  r.answers[0].points[0].reason = "明确空白未作答";
  validateResult(r, claim);
  r.answers[0].pages = [];
  assert.throws(() => validateResult(r, claim));
  (r.answers[0].reviewItems as string[]).push("页面缺失");
  validateResult(r, claim);
});

test("expired completed result is safely rebound after claiming identical frozen task without rerunning model", async () => {
  const root = await mkdtemp(join(tmpdir(), "xzt-reclaim-test-"));
  const token = join(root, "token"),
    state = join(root, "state");
  await writeFile(token, "dedicated-token", { mode: 0o600 });
  await import("node:fs/promises").then((fs) =>
    fs.mkdir(join(state, uuid), { recursive: true, mode: 0o700 }),
  );
  const original = structuredClone(claim);
  original.deadline = new Date(Date.now() - 10000).toISOString();
  original.leaseUntil = new Date(Date.now() - 20000).toISOString();
  await atomic(join(state, uuid, "claim.json"), original);
  await atomic(join(state, uuid, "model-result.json"), {
    output: result,
    model: "mock",
    completedAt: new Date(Date.now() - 30000).toISOString(),
  });
  const newClaim = structuredClone(claim);
  newClaim.leaseToken = "22222222-2222-4222-8222-222222222222";
  newClaim.deadline = new Date(Date.now() + 900000).toISOString();
  let posts = 0,
    claims = 0,
    materials = 0;
  const server = createServer(async (req, res) => {
    let body = "";
    for await (const b of req) body += b;
    if (req.url?.includes("/materials/")) materials++;
    if (req.url?.endsWith("/result")) {
      posts++;
      const payload = JSON.parse(body);
      assert.deepEqual(payload.result, result);
      if (posts === 1) {
        assert.equal(payload.leaseToken, original.leaseToken);
        res.writeHead(409);
        res.end(
          JSON.stringify({ code: 40902, message: "lease expired", data: null }),
        );
        return;
      }
      assert.equal(payload.leaseToken, newClaim.leaseToken);
      res.end(
        JSON.stringify({
          code: 0,
          data: { state: "COMPLETED" },
          message: "ok",
        }),
      );
      return;
    }
    if (req.url?.endsWith("/claim")) {
      claims++;
      res.end(
        JSON.stringify({
          code: 0,
          data: claims === 1 ? newClaim : null,
          message: "ok",
        }),
      );
      return;
    }
    res.end(JSON.stringify({ code: 0, data: null, message: "ok" }));
  });
  await new Promise<void>((r) => server.listen(0, "127.0.0.1", r));
  const address = server.address() as { port: number };
  const w = new Worker({
    server: `http://127.0.0.1:${address.port}`,
    allowLocalHttp: true,
    tokenFile: token,
    stateDir: state,
    codexBinary: "/must/not/be/called",
  });
  const running = w.run();
  await wait(async () => posts === 2, 20000);
  w.stop();
  await running;
  assert.equal(materials, 0);
  assert.equal(claims, 1);
  assert.equal(
    await stat(join(state, "pending.json"))
      .then(() => true)
      .catch(() => false),
    false,
  );
  await new Promise<void>((r) => server.close(() => r()));
  await rm(root, { recursive: true, force: true });
});
