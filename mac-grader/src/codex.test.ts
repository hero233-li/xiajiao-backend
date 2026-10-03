import test from "node:test";
import assert from "node:assert/strict";
import { mkdtemp, writeFile, chmod, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { Codex } from "./codex.js";
async function adapter(mode: string) {
  const root = await mkdtemp(join(tmpdir(), "xzt-codex-rpc-"));
  const binary = join(root, "fake.cjs");
  await writeFile(
    binary,
    `#!/usr/bin/env node
const rl=require('node:readline').createInterface({input:process.stdin});
const mode=${JSON.stringify(mode)};
const emit=x=>process.stdout.write(JSON.stringify(x)+'\\n');
rl.on('line',line=>{const m=JSON.parse(line);if(!m.id)return;
 if(m.method==='initialize')return emit({id:m.id,result:{}});
 if(m.method==='account/read')return emit({id:m.id,result:{account:{type:mode==='login'?'apiKey':'chatgpt',planType:'test'}}});
 if(m.method==='thread/start'){
  if(!m.params.ephemeral||m.params.approvalPolicy!=='never'||process.env.DB_PASSWORD||process.env.OPENAI_API_KEY)throw new Error('isolation violated');
  return emit({id:m.id,result:{thread:{id:'independent-thread'},model:'mock'}});
 }
 if(m.method==='turn/start'){
  if(m.params.input[1]?.type!=='localImage'||!m.params.outputSchema||m.params.sandboxPolicy.networkAccess!==false)throw new Error('input contract violated');
  emit({id:m.id,result:{turn:{id:'turn',status:'inProgress'}}});
  if(mode==='timeout')return;
  if(mode==='quota')return emit({method:'turn/completed',params:{threadId:'independent-thread',turn:{status:'failed',error:{message:'UsageLimitExceeded'}}}});
  if(mode==='interrupted')return emit({method:'turn/completed',params:{threadId:'independent-thread',turn:{status:'interrupted'}}});
  emit({method:'item/completed',params:{threadId:'independent-thread',item:{type:'agentMessage',phase:'final_answer',text:mode==='refused'?'I cannot grade this':JSON.stringify({answer:'12'})}}});
  emit({method:'turn/completed',params:{threadId:'independent-thread',turn:{status:'completed'}}});
 }
});
`,
    { mode: 0o700 },
  );
  const c = new Codex(binary, root, root, 100);
  return {
    c,
    root,
    close: async () => {
      c.close();
      await rm(root, { recursive: true, force: true });
    },
  };
}
test("RPC image input, schema, independent ephemeral thread and stripped credentials", async () => {
  const a = await adapter("ok");
  try {
    await a.c.initialize();
    const r = await a.c.run(a.root, "grade", ["answer.png"], {
      type: "object",
    });
    assert.deepEqual(r.output, { answer: "12" });
  } finally {
    await a.close();
  }
});
test("API-key account is refused without switching payment mode", async () => {
  const a = await adapter("login");
  try {
    await assert.rejects(a.c.initialize(), /LOGIN_REQUIRED/);
  } finally {
    await a.close();
  }
});
for (const [mode, expected] of [
  ["quota", /UsageLimitExceeded/],
  ["refused", /INVALID_STRUCTURED_OUTPUT/],
  ["interrupted", /interrupted/],
  ["timeout", /TIMEOUT_15_MINUTES|APP_SERVER_EXIT/],
] as const) {
  test(`RPC ${mode} never becomes a successful grading result`, async () => {
    const a = await adapter(mode);
    try {
      await a.c.initialize();
      await assert.rejects(
        a.c.run(a.root, "grade", ["answer.png"], {}),
        expected,
      );
    } finally {
      await a.close();
    }
  });
}
