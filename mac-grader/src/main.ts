import { readFile, rm, mkdir, writeFile } from "node:fs/promises";
import { resolve, join } from "node:path";
import { Worker, type Config } from "./worker.js";
const configPath = resolve(process.argv[2] || "config.json");
const config: Config = JSON.parse(await readFile(configPath, "utf8"));
config.tokenFile = resolve(config.tokenFile);
config.stateDir = resolve(config.stateDir);
const command = process.argv[3] || "run";
if (command === "pause") {
  await mkdir(config.stateDir, { recursive: true, mode: 0o700 });
  await writeFile(join(config.stateDir, "pause"), "paused", { mode: 0o600 });
} else if (command === "resume") {
  await rm(join(config.stateDir, "pause"), { force: true });
  await rm(join(config.stateDir, "paused.json"), { force: true });
} else {
  const worker = new Worker(config);
  process.on("SIGINT", () => worker.stop());
  process.on("SIGTERM", () => worker.stop());
  await worker.run();
}
