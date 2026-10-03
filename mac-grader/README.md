# 学习知途 Mac 本地批改程序

运行在当前Mac用户下，主动通过HTTPS领取本人任务。需要Node22+、Codex CLI（本次真实验证0.147.0）及ChatGPT授权；不需要OpenAI API Key。PDF评分标准草稿另外需要Poppler的pdftoppm。服务器只运行Java/MySQL/网页/私有存储。

## 安装和前台运行

```sh
cd /Users/huagu/IDEA/自考/xiajiao/repo/backend/mac-grader
npm ci
npm run build
codex --version
codex login status
```

如本地未登录，用 `codex login` 选择ChatGPT授权；不要添加API Key。网页“Mac批改程序配对与状态”创建专用凭证，一次性返回。把它保存为本人可读的文件，不要写入源码、日志或Codex提示词：

```sh
mkdir -p "$HOME/Library/Application Support/XuexiZhituGrader"
chmod 700 "$HOME/Library/Application Support/XuexiZhituGrader"
# 用本地编辑器将网页凭证保存到此文件，避免凭证进入命令历史：
# $HOME/Library/Application Support/XuexiZhituGrader/pairing.token
chmod 600 "$HOME/Library/Application Support/XuexiZhituGrader/pairing.token"
cp config.example.json config.json
```

编辑config.json，填写真实HTTPS origin、tokenFile、stateDir、codexBinary的绝对路径。PDF草稿使用 `pdfRenderer` 指向 `pdftoppm`；缺少时安装Poppler（例如 `brew install poppler`），再确认可执行文件路径。可选model保持为空使用本地Codex默认模型；配置指定模型后应重新验证授权，不自动切换支付方式。

```sh
node dist/main.js /绝对路径/config.json
```

启动后每5秒领取，任务期间每30秒心跳续租。所有实际模型输入都是独立会话的localImage，outputSchema约束结果。私有状态目录包含临时答卷和结果，chmod700；不可在任务处理期间移动或删除。

授权探针（不影响服务器任务）：本地准备root/materials/answer.png，运行 `PROBE_ROOT=/绝对路径/root CODEX_BIN=/绝对路径/codex node dist/probe.js`。它会实际调用模型、消耗当前ChatGPT用量，输出授权类型及结构化识别结果；不会打印账户邮箱或授权文件。

## 暂停和恢复

```sh
node dist/main.js /绝对路径/config.json pause
node dist/main.js /绝对路径/config.json resume
```

pause只停止领取新任务，当前任务继续完成。登录失效或用量限制会持久暂停并把原因显示在网页；本地完成登录/等待用量恢复后再resume。撤销配对后，网页创建新凭证、替换token文件，停止并重新启动工作程序；随后撤销旧凭证。凭证只允许本人任务处理，不能访问管理员接口。

## launchd（当前用户）

先前台验证，再生成启动项；install不自动启用服务，不需要sudo：

```sh
node scripts/launchd.mjs install /绝对路径/config.json
node scripts/launchd.mjs start
node scripts/launchd.mjs stop
node scripts/launchd.mjs restart
node scripts/launchd.mjs uninstall
```

保存到 `~/Library/LaunchAgents/cn.xuexizhitu.grader.plist`。登录时运行并在退出后重启，串行单实例；日志在 `~/Library/Logs/XuexiZhituGrader/`，只有状态说明，不含凭证、模型提示词或答卷内容。卸载保留凭证及未确认材料，先在网页撤销，再确认是否删除本地敏感材料。Mac关机/休眠时任务在服务器保留，用户重新登录后恢复；不会避免睡眠，也不要求公网入口。

## 异常恢复

- pending.json：模型已完成、服务器尚未确认。优先重传，不重新推理；不要手工清除。
- stale-*.json / rejected-*.json：租约过期或服务器校验拒绝。合法重新领取同一任务后，若冻结输入完全一致、模型结果在原截止前完成且未被校验拒绝，可以用新领取凭据回传已有结果；不重新调用模型。输入变化、被拒绝或时间无法证明时保留原结果供核查。
- 任务目录中的model-result.json：原模型输出保留到服务器确认；失败目录不自动删除。确认无需保留后才能手工清理。
- worker.lock：单实例锁。正常停止会清除，异常退出下次启动会核验进程是否存在后恢复。PID复用等极少数情况先核对确无其他程序在处理，再清除锁。
- 每次服务器网络请求最多重试两次。模型侧自动网络/流重试关闭，由任务重新领取最多两次；授权/用量错误优先暂停。
- 每次领取最长15分钟，睡眠跨越截止恢复后立即终止。过期结果保留；必须重新合法领取，校验上述条件并取得新凭据后才能复用结果，旧凭据不能写入。

## 验证

```sh
npm run build
npm test
cd ..
REAL_CODEX_GRADING=true mvn -Pmysql-it verify
```

最后命令需要独立MySQL测试容器、相邻前端依赖/Playwright、本地Codex ChatGPT授权，会实际调用模型。未设置环境变量时真实Codex测试跳过，不能据此声称完整链路通过。参见[交付及范围](../docs/grading-delivery.md)。
