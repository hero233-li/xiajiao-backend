# 服务器批改（DSH 桥接）

Mac 关机或离线时，由服务器上的同一套 worker 接管批改。模型走本机 DSH 的
`dsh --profile sdk`（stdio JSON-RPC，模型 `deepseek-flash`，支持图片输入），
不需要额外的模型 API Key。

## 组成

| 文件 | 作用 |
|---|---|
| `dsh-bridge.mjs` | 桥接：读任务目录的 `claim.json`/`prompt.txt`/答卷图 → 调 DSH SDK → 写 `deepseek-result.json` |
| `dsh-bridge.sh` | mac-grader 只接受单条命令，用它包一层 node |
| `gate.mjs` | failover 网关：Mac worker 在线时创建 `state/pause`，离线后移除 |
| `config.json` | worker 配置，`deepSeekFirst: true` 表示批改直接走 DSH，不碰 Codex |
| `gate.json` | 网关配置（只在这台机器上，含登录态与 worker id） |

## 部署位置

```
/opt/projects/xiajao/server-grader/
  dist/               # mac-grader 编译产物（npm run build 后 rsync）
  dsh-bridge.mjs  dsh-bridge.sh  gate.mjs
  config.json  gate.json  pairing.token  state/
```

systemd：

```sh
systemctl status xiajiao-server-grader xiajiao-server-grader-gate
journalctl -u xiajiao-server-grader -f
```

## 行为约定

- **Mac 优先**：网关每 30s 读一次 `/api/v1/grading/workers`，只要还有别的
  worker 在线且未暂停，就给本机 worker 写 `state/pause`，服务器不领取任务。
- **服务器只做批改**：收到 `RUBRIC` 或 `KNOWLEDGE` 任务时立即回 `failure`
  （retryable），交还队列给 Mac，避免用不支持的流程处理。
- **必须人工核对**：桥接会给每个结果追加 reviewItem，后端因此把任务置为
  `REVIEW` 而不是 `COMPLETED`，不会自动生成成绩。

## 关闭服务器批改

```sh
sudo systemctl disable --now xiajiao-server-grader xiajiao-server-grader-gate
```
