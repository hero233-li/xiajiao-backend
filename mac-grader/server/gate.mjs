#!/usr/bin/env node
/**
 * Failover 网关：Mac 批改程序在线时让服务器 worker 保持暂停，
 * Mac 离线超过阈值后才接管任务。
 *
 * 暂停方式 = 创建 <stateDir>/pause（mac-grader 的 paused() 会读取它），
 * 不影响 worker 自身因登录/额度问题写入的 paused.json。
 *
 * 需要一份能读 /api/v1/grading/workers 的登录态；凭证只保存在本机 gate.json。
 */
import { readFileSync, existsSync, writeFileSync, rmSync, chmodSync } from 'node:fs';
import { join } from 'node:path';

const CONFIG_PATH = process.env.XZT_GATE_CONFIG ?? '/opt/projects/xiajao/server-grader/gate.json';
const cfg = JSON.parse(readFileSync(CONFIG_PATH, 'utf8'));
const pollMs = Number(cfg.pollMs ?? 30_000);
const pauseFile = join(cfg.stateDir, 'pause');
const log = (message) => console.log(new Date().toISOString(), message);

let token = '';
let tokenAt = 0;

async function login() {
  const response = await fetch(`${cfg.apiBase}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ identifier: cfg.account.identifier, password: cfg.account.password }),
  });
  if (!response.ok) throw new Error(`LOGIN_${response.status}`);
  const payload = await response.json();
  token = payload?.data?.accessToken ?? '';
  tokenAt = Date.now();
  if (!token) throw new Error('LOGIN_NO_TOKEN');
  log('登录成功');
}

async function workers() {
  if (!token || Date.now() - tokenAt > 45 * 60_000) await login();
  let response = await fetch(`${cfg.apiBase}/api/v1/grading/workers`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (response.status === 401) {
    await login();
    response = await fetch(`${cfg.apiBase}/api/v1/grading/workers`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  }
  if (!response.ok) throw new Error(`WORKERS_${response.status}`);
  return (await response.json())?.data ?? [];
}

function setPaused(paused, reason) {
  const currently = existsSync(pauseFile);
  if (paused && !currently) {
    writeFileSync(pauseFile, JSON.stringify({ reason, at: new Date().toISOString() }), { mode: 0o600 });
    chmodSync(pauseFile, 0o600);
    log(`暂停服务器批改：${reason}`);
  } else if (!paused && currently) {
    rmSync(pauseFile, { force: true });
    log('Mac 已离线或不在线，服务器接管批改');
  }
}

let lastState = '';
for (;;) {
  try {
    const list = await workers();
    const others = list.filter(
      (w) => w.id !== cfg.ownWorkerId && w.online && !w.revoked && !w.paused,
    );
    const state = others.length ? 'paused' : 'active';
    setPaused(others.length > 0, others.length ? `Mac 批改程序在线（${others.map((w) => w.label).join('、')}）` : '');
    if (state !== lastState) {
      lastState = state;
      log(`状态 -> ${state}（在线 worker：${list.filter((w) => w.online).map((w) => w.label).join('、') || '无'}）`);
    }
  } catch (error) {
    log(`网关检查失败：${String(error).slice(0, 160)}`);
  }
  await new Promise((resolve) => setTimeout(resolve, pollMs));
}
