# 运行、迁移、备份与回退

本次没有发布生产、没有修改现有业务库，也没有执行现有服务器发布脚本。测试使用Testcontainers的独立MySQL8.4，自动删除临时容器；浏览器中的测试账号、训练、食谱和打卡均在该隔离库。

## 本地构建与独立验收

要求Java17、Maven、Node22.12至22.x、Docker Desktop及已安装的前端Playwright Chromium。

从两个仓库各自目录运行：

```sh
# backend
mvn -Pmysql-it verify

# frontend
python3 scripts/generate-fitness-types.py --check
node scripts/generate-platform-openapi.mjs
npm run api:check
npm run lint
npm test
npm run build
```

只重现平台全栈验收可用：

```sh
cd backend
mvn -Pmysql-it -Dit.test=FitnessHttpMySqlIT,FitnessMigrationMySqlIT verify
```

此命令启动临时后端、随机端口Vite及真实浏览器；`frontend/docs/platform/evidence/browser.json`记录请求状态与检查结论，PNG保存页面。没有使用MSW或浏览器请求拦截。前端单元测试允许受控测试夹具，与最终运行方式区分。

## 运行实际应用

先在隔离副本验证现有数据库，再决定切换。不要直接用原本含固定开发密码的application.yml连接正式环境。继续用既有production profile显式提供配置，即使是本机的安全副本也可以使用该profile。

需要通过你的本地环境或秘密管理注入：

- DB_URL：独立副本的MySQL JDBC URL，明确UTC连接时区，例如`jdbc:mysql://127.0.0.1:13328/zhitu_preview?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true`。
- DB_USER、DB_PASSWORD：副本用户，仅该数据库必要权限。
- JWT_SECRET_BASE64：随机至少32字节，沿用或明确轮换的密钥，不提交仓库。
- APP_FILES_ROOT：副本的私有附件目录；数据库与附件需要一致复制。
- SERVER_PORT：例如18081，与已有8080服务隔离。

```sh
cd backend
mvn -DskipTests package
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

前端在另一个终端：

```sh
cd frontend
API_PROXY_TARGET=http://127.0.0.1:18081 VITE_API_MOCK=false VITE_API_ORIGIN= npm run dev -- --port 5189
```

访问Vite显示的地址。使用副本中既有账号登录。注册政策保持关闭，不插入默认目标/体重/打卡。没有账号的全新库请按已有后台账号管理/初始化机制由管理员建立账号；不要为了体验而打开公共注册。

## V11上线准备

V1–V10不改动；新V11只新增fitness_record、fitness_goal_state、fitness_goal_event、用户外键、唯一键、日期/状态索引及精确体重列。MySQL DDL会隐式提交，因此发布前应停止写入，保留数据库与私有文件的一致备份，并在副本上执行迁移。

1. 记录前后端版本、Flyway版本/校验值、实际用户和学习业务表的精确计数。保存已有未提交内容，避免发布脚本遗漏。
2. 使用受保护的MySQL登录配置导出完整库，例如`mysqldump --login-path=zhitu-backup --single-transaction --routines --triggers --set-gtid-purged=OFF xuexizhitu > backup.sql`。密码不写入命令、文档或版本控制。
3. 在停写期间同步备份APP_FILES_ROOT及部署配置。不要公开私有附件。恢复到独立库和独立目录。
4. 在副本启动新后端，让Flyway执行V11；验证现有账号、学习/练习/成绩、管理员和健身闭环，再核对原表、文件及迁移校验值。
5. 正式采用时先后端迁移、再前端静态资源；服务器代理全部`/api`到新后端，前端history fallback必须覆盖`/study`、`/fitness`、`/settings`和旧`/zikao`。保留现有私有文件与管理接口代理规则。
6. 验证GET `/api/v1/health/readiness`、登录与两个空间，核对`Cache-Control:no-store`。前端VITE_API_MOCK=false，生产构建亦禁用Mock。经用户明确授权后才能执行实际发布；本任务未执行。

## 回退

V11是附加表，旧学习数据没有被改写。前端与后端必须作为兼容版本一起回退。Flyway不会自动向下迁移；不通过删除history行或clean回退。

回到旧jar前先评估旧程序是否允许存在V11记录/新表。为了最稳妥的恢复，在维护窗口保全V11之后新增的健身数据及幂等记录，再恢复上线前完整数据库和附件备份；新增健身数据另存，重新升级时按用户键、日期、版本去重导入。不要盲目drop新增表，否则会丢失上线后的个人记录。已有V9等迁移的回退限制仍然适用，继续遵循原迁移文档。

## 现有修改和数据保护

开始时分别检查frontend/backend独立Git仓库。原有未提交管理员、答卷批改、成绩修订代码未被重置。源代码与原有diff基线保存于本机`/private/tmp/zhitu-platform-baseline/`；该目录可能包含原工程私密开发配置，仅作本机回溯，不公开提交。

现有`xuexizhitu-dev-mysql-1`数据库仅作只读数量核查，测试未连接它写入。没有修改application.yml本地配置、清理数据卷、执行生产SSH或发布网站。
