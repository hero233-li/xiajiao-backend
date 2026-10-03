import { execFileSync } from "node:child_process";
import { Codex, isolatedHome } from "./codex.js";
import { mkdir, writeFile } from "node:fs/promises";
const root = process.env.PROBE_ROOT || "/private/tmp/xzt-grading-probe";
await mkdir(`${root}/materials`, { recursive: true });
await isolatedHome(`${root}/codex`);
const c = new Codex(
  process.env.CODEX_BIN || "/Users/huagu/.local/bin/codex",
  `${root}/codex`,
  `${root}/materials`,
);
try {
  const auth = await c.initialize();
  console.log(
    JSON.stringify({
      version: execFileSync(
        process.env.CODEX_BIN || "/Users/huagu/.local/bin/codex",
        ["--version"],
        { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] },
      ).trim(),
      ...auth,
    }),
  );
  const result = await c.run(
    `${root}/materials`,
    "读取图片上的算式和手写答案，判断是否正确。不要调用工具。",
    [`${root}/materials/answer.png`],
    {
      type: "object",
      properties: {
        equation: { type: "string" },
        answer: { type: "string" },
        correct: { type: "boolean" },
      },
      required: ["equation", "answer", "correct"],
      additionalProperties: false,
    },
  );
  await writeFile(
    `${root}/evidence.json`,
    JSON.stringify({ auth, ...result }, null, 2),
  );
  console.log(JSON.stringify(result));
} finally {
  c.close();
}
