import { homedir } from "node:os";
import { resolve, join, dirname } from "node:path";
import { mkdir, writeFile, rm } from "node:fs/promises";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
const root = resolve(dirname(fileURLToPath(import.meta.url)), ".."),
  label = "cn.xuexizhitu.grader";
const plist = join(homedir(), "Library", "LaunchAgents", label + ".plist"),
  domain = `gui/${process.getuid()}`;
const command = process.argv[2];
const config = resolve(process.argv[3] || join(root, "config.json"));
const run = (...args) => {
  const r = spawnSync("/bin/launchctl", args, { stdio: "inherit" });
  if (r.status && command !== "uninstall") process.exitCode = r.status;
};
const xml = (s) =>
  s
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
if (command === "install") {
  const logs = join(homedir(), "Library", "Logs", "XuexiZhituGrader");
  await mkdir(logs, { recursive: true, mode: 0o700 });
  await mkdir(dirname(plist), { recursive: true });
  await writeFile(
    plist,
    `<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd"><plist version="1.0"><dict><key>Label</key><string>${label}</string><key>ProgramArguments</key><array><string>${xml(process.execPath)}</string><string>${xml(join(root, "dist/main.js"))}</string><string>${xml(config)}</string></array><key>WorkingDirectory</key><string>${xml(root)}</string><key>RunAtLoad</key><true/><key>KeepAlive</key><true/><key>ThrottleInterval</key><integer>30</integer><key>EnvironmentVariables</key><dict><key>PATH</key><string>/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin</string></dict><key>StandardOutPath</key><string>${xml(join(logs, "worker.log"))}</string><key>StandardErrorPath</key><string>${xml(join(logs, "error.log"))}</string></dict></plist>`,
    { mode: 0o600 },
  );
  console.log(`已生成 ${plist}；执行 start 才启动服务`);
} else if (command === "start") {
  run("bootstrap", domain, plist);
} else if (command === "stop") {
  run("bootout", domain + "/" + label);
} else if (command === "restart") {
  run("kickstart", "-k", domain + "/" + label);
} else if (command === "uninstall") {
  run("bootout", domain + "/" + label);
  await rm(plist, { force: true });
  console.log("启动项已卸载；凭证、材料和未回传结果保留，请确认后自行清理");
} else
  throw new Error(
    "用法：node scripts/launchd.mjs install|start|stop|restart|uninstall [config.json]",
  );
