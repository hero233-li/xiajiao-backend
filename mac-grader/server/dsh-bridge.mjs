#!/usr/bin/env node
/**
 * 服务器批改桥接：把 mac-grader 交来的任务目录转成一次 DSH SDK 调用。
 *
 * 契约（由 mac-grader/worker.ts 的 deepSeek() 定义）：
 *   argv[2] = 任务目录；cwd 也是该目录
 *   env.XZT_GRADER_IMAGES = 图片绝对路径的 JSON 数组（顺序即答卷页序）
 *   读取：<dir>/claim.json、<dir>/prompt.txt
 *   输出：<dir>/deepseek-result.json = { model, output }，output 必须能通过 validateResult
 *
 * 模型由本机 DSH 提供（deepseek-flash，支持图片输入），不需要额外 API Key。
 */
import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { readFileSync, writeFileSync, rmSync } from 'node:fs';
import { extname, join } from 'node:path';

const MIME = { '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp', '.gif': 'image/gif' };
const TIMEOUT_MS = Number(process.env.XZT_DSH_TIMEOUT_MS ?? 12 * 60_000);
const MODEL = process.env.XZT_DSH_MODEL ?? 'deepseek-flash';
const PROVIDER = process.env.XZT_DSH_PROVIDER ?? 'deepseek-account';

const dir = process.argv[2] ?? process.env.XZT_GRADER_DIR;
if (!dir) throw new Error('缺少任务目录');
const outPath = join(dir, 'deepseek-result.json');
rmSync(outPath, { force: true });

const claim = JSON.parse(readFileSync(join(dir, 'claim.json'), 'utf8'));
const basePrompt = readFileSync(join(dir, 'prompt.txt'), 'utf8');
const images = JSON.parse(process.env.XZT_GRADER_IMAGES ?? '[]');
const questions = claim?.inputs?.rubric?.document?.questions ?? [];
const rubricId = claim?.inputs?.rubric?.id;

const shape = JSON.stringify({
  answers: questions.map((q) => ({
    number: q.number,
    recognizedAnswer: '学生作答内容',
    points: q.points.map((p) => ({ pointId: p.id, score: 0, reason: '扣分原因' })),
    pages: [1],
    reviewItems: [],
  })),
  reviewItems: [],
});

const instruction = `

====
输出要求（必须严格遵守）
只输出一个 JSON 对象，不要解释、不要 Markdown、不要代码围栏。
必须覆盖全部 ${questions.length} 道题，题号与评分点 id 必须与评分标准完全一致：
${shape}
`;

function extractJson(text) {
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i);
  const body = fenced ? fenced[1] : text;
  const start = body.indexOf('{');
  const end = body.lastIndexOf('}');
  if (start < 0 || end <= start) throw new Error('DSH_NO_JSON');
  return JSON.parse(body.slice(start, end + 1));
}

function normalize(raw) {
  const answers = new Map();
  for (const a of Array.isArray(raw?.answers) ? raw.answers : []) answers.set(String(a?.number ?? ''), a);
  return {
    rubricId,
    answers: questions.map((q) => {
      const a = answers.get(String(q.number)) ?? {};
      const earned = new Map((Array.isArray(a.points) ? a.points : []).map((p) => [String(p?.pointId ?? ''), p]));
      return {
        number: q.number,
        recognizedAnswer: typeof a.recognizedAnswer === 'string' ? a.recognizedAnswer : '',
        points: q.points.map((p) => {
          const got = earned.get(String(p.id)) ?? {};
          const score = Number(got.score);
          return {
            pointId: p.id,
            score: Number.isFinite(score) ? Math.min(Math.max(score, 0), Number(p.maximum)) : 0,
            reason: typeof got.reason === 'string' ? got.reason : '模型未返回该评分点',
          };
        }),
        pages: (Array.isArray(a.pages) ? a.pages : [])
          .map((n) => Number(n))
          .filter((n) => Number.isInteger(n) && n >= 1 && n <= 20),
        reviewItems: [
          ...(Array.isArray(a.reviewItems) ? a.reviewItems : []).filter(
            (s) => typeof s === 'string' && s.trim(),
          ),
          ...(!Array.isArray(a.pages) || !a.pages.length ? ['模型未返回本题页码，请人工确认'] : []),
        ],
      };
    }),
    // 服务器批改一律进入待核对：不自动生成成绩，由人确认。
    reviewItems: [
      ...(Array.isArray(raw?.reviewItems) ? raw.reviewItems : []).filter(
        (s) => typeof s === 'string' && s.trim(),
      ),
      '本卷由服务器 DSH 批改，请人工核对后再生成成绩',
    ],
  };
}

function callDsh() {
  return new Promise((resolve, reject) => {
    const child = spawn('/opt/dsh/node/bin/dsh', ['--profile', 'sdk'], {
      cwd: dir,
      env: {
        PATH: '/opt/dsh/node/bin:/usr/local/bin:/usr/bin:/bin',
        HOME: process.env.HOME,
        DSH_HOME: process.env.DSH_HOME ?? '/opt/dsh/home',
      },
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    const rl = createInterface({ input: child.stdout });
    const pending = new Map();
    const chunks = [];
    let nextId = 0;
    let settled = false;
    const timer = setTimeout(() => {
      settled = true;
      child.kill('SIGKILL');
      reject(new Error('DSH_TIMEOUT'));
    }, TIMEOUT_MS);

    const send = (method, params) =>
      new Promise((res, rej) => {
        const id = ++nextId;
        pending.set(id, { res, rej });
        child.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
      });

    rl.on('line', (line) => {
      let msg;
      try {
        msg = JSON.parse(line);
      } catch {
        return;
      }
      if (msg.id && !msg.method) {
        const p = pending.get(msg.id);
        if (!p) return;
        pending.delete(msg.id);
        msg.error ? p.rej(new Error(JSON.stringify(msg.error))) : p.res(msg.result);
        return;
      }
      if (msg.method !== 'session.event') return;
      const event = msg.params?.event;
      if (event?.type === 'assistant/message' && Array.isArray(event?.data?.message?.content))
        for (const block of event.data.message.content)
          if (block?.type === 'text' && block.text) chunks.push(block.text);
    });

    (async () => {
      try {
        await send('initialize', { cwd: dir, provider: PROVIDER, model: MODEL });
        const contentBlocks = [{ type: 'text', text: basePrompt + instruction }];
        for (const path of images) {
          const ext = extname(path).toLowerCase();
          const mimeType = MIME[ext];
          if (!mimeType) continue;
          contentBlocks.push({
            type: 'image',
            data: readFileSync(path).toString('base64'),
            mimeType,
          });
        }
        const sessionId = `grade-${claim.taskId}-${Date.now()}`;
        await send('session/prompt', { sessionId, contentBlocks });
        await send('session/wait', { sessionId });
        if (settled) return;
        clearTimeout(timer);
        settled = true;
        child.kill();
        if (!chunks.length) return reject(new Error('DSH_EMPTY_RESPONSE'));
        resolve(chunks.join(''));
      } catch (error) {
        if (!settled) {
          settled = true;
          clearTimeout(timer);
          child.kill();
          reject(error);
        }
      }
    })();
  });
}

const text = await callDsh();
const output = normalize(extractJson(text));
writeFileSync(outPath, JSON.stringify({ model: `deepseek-dsh:${MODEL}`, output }), { mode: 0o600 });
process.stdout.write(JSON.stringify({ ok: true, answers: output.answers.length }));
