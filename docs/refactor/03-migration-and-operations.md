# 数据迁移、部署与回退

本次没有连接或部署正式服务。迁移对象是现有 MySQL V1–V7 数据；所有既有主键、原始字段、外键、内容版本、通过记录、归档 payload 和实体附件保留。V1–V7 校验和不变。新增 V8、V9，由 Flyway 在启动时执行，禁止 clean、baseline-on-migrate 或修改历史校验和。

## 字段映射与转换

|原结构|新结构|转换及含义|
|---|---|---|
|score_record 的全部原字段|原表字段继续保留|不改日期、成绩、revision、source 或未知 answers_seen_before|
|现存 score_record 当前版本|score_record_revision|每条复制一份 MIGRATION_BASELINE；actor_id=NULL，captured_at 为迁移时间，不伪造过去的修订人/修订时间|
|新建/修订成绩|score_record_revision|与主表、审计在同一事务生成 CREATED/REVISED 快照，失败一起回滚|
|paper.paper_month + course.code|score_record.paper_key_snapshot|迁移时固定 `课程代码:YYYY-MM`，后续管理员改试卷月份或课程代码不改原成绩含义；revision 同步固定|
|content_release|draft_revision|新增非负版本号从0开始；草稿写入与发布加1，If-Match 不匹配返回409|
|stored_file DELETE_PENDING|file_cleanup_task|为待删除文件补任务；保留实体，后台领取后删除，完成才设DELETED|
|崩溃上传残留|pending 日志、quarantine、file_reconciliation|24小时宽限期后对账；无数据库记录的字节移入隔离目录，禁止自动销毁|
|个人写入重试|request_replay|用户+操作+UUID唯一，记录请求摘要及原始响应；同键不同请求409|
|检测后台失败|background_failure|目标ID、尝试次数、错误类型与下次重试；成功恢复后清除活动失败，历史判分记录仍留在 session/pass|
|既有学习计划与旧默认复习任务|原 plan_* 表及 snapshot|不重新组排、不修改60分钟历史估时、不补造管理员模板；新建/完整编辑五周计划要求每科 REVIEW 模板|
|旧计划顺延预览|原 plan_preview|保持可读；指纹编码改进后，旧未确认预览可能409，重新生成后确认；不改变已确认版本|

修改旧数据之前已经覆盖掉的成绩版本无法从当前表重建。迁移基线保留现在能够取得的真实记录，不把基线冒充完整历史。线上 D1/R2 导出未提供，本次没有宣称完成该来源的数据导入。

## 一致备份与演练

1. 暂停业务写入和后台任务，保留正在进行的检测记录；确认没有上传/提交事务仍在执行。数据库与私有文件使用同一冻结窗口。
2. 使用受保护的 MySQL 客户端配置文件（权限0600），执行 `mysqldump --defaults-extra-file=/secure/mysql.cnf --single-transaction --routines --triggers --events DATABASE > database.sql`。配置文件包含目标主机、端口、用户名与密码，勿把密码写入命令行或报告。另保存现有应用、配置、Flyway历史和部署版本。
3. 完整备份 `APP_FILES_ROOT`（含 managed、pending、quarantine）；jar 中的 classpath 手册/资料由该版本 jar 一起备份。禁止只备份元数据而遗漏字节。
4. 在独立验证环境还原数据库与文件备份，再运行下面的只读对账工具。`DB_URL/DB_USER/DB_PASSWORD` 必须明确指向验证副本。工具不会迁移、清表或导出记录值。

```sh
scripts/migration-inventory.sh capture before.tsv
python3 scripts/file-inventory.py capture "$APP_FILES_ROOT" before-files.json
```

启动新 jar，保持 `APP_FILES_CLEANUP_ENABLED=false`、`APP_ASSESSMENTS_TIMEOUT_ENABLED=false`，避免对账期间后台合法修改旧字段。检测开关对应 Spring 属性；可用启动参数 `--app.assessments.timeout-enabled=false` 明确覆盖。

```sh
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production --app.assessments.timeout-enabled=false
scripts/migration-inventory.sh compare before.tsv
python3 scripts/file-inventory.py compare "$APP_FILES_ROOT" before-files.json
```

对账工具读取每张原表的全部原列，按稳定主键流式计算摘要，逐表比较数量与摘要；新增表/列不会使原字段检查失效。演练使用54张旧表，包含个人成绩、作答、通过、归档和旧默认复习计划。工具启动需要Java17和构建已下载的 MySQL JDBC 驱动；也可设置 MYSQL_JDBC_JAR。

检查 Flyway 全部成功、9个版本校验通过，旧字段对账成功，新成绩基线数等于原成绩数，新 paper_key_snapshot 无NULL。人工抽查最重要用户的旧题目答案、成绩图片、计划历史和通过凭据。验证副本通过后，正式环境需由部署人员在同样冻结窗口重复备份、迁移和核对。

## 本地运行与部署配置

Java17、Maven、MySQL8.4，实际前端需要Node22.12–22.x。不增加运行时缓存或消息队列。

生产使用 `production` profile，必须显式设置 DB_URL、DB_USER、DB_PASSWORD、JWT_SECRET_BASE64、APP_FILES_ROOT；JWT密钥至少32字节随机值的base64。保留用户的 application.yml 本地修改，生产profile覆盖固定本地凭据。数据源不使用 Hibernate 自动建表；Flyway负责升级，JPA只验证。

私有目录使用应用账号的可写目录，不放在静态站点目录，不共享公开URL，不接受符号链接路径。后端用8080或SERVER_PORT，反向代理仅公开API；前端运行 `npm ci`、`npm run build`，发布 dist 并代理 `/api` 到后端。本地开发前端可设置 API_PROXY_TARGET、VITE_API_MOCK=false，再运行 `npm run dev`。

存活检查 GET `/api/v1/health`；数据库就绪检查 GET `/api/v1/health/readiness`，数据库不可用时503且data=null。下载仍通过授权API返回私有字节，不暴露 storage_key。部署验收通过之后才恢复业务写入和后台任务。

## 恢复与回退

MySQL DDL不能依赖一个事务自动回滚。迁移失败时保持维护状态，不盲目 repair 或重跑不完整DDL。使用已验证的同一时间点数据库+文件+旧jar备份恢复，并重新核对数量和字节。V9新增NOT NULL字段，旧jar写成绩时不会填入，因此**不能只切回旧jar**。

恢复备份前如果已经恢复新版本写入，先备份并导出新增业务数据，明确完成差异合并后才允许回退；不得丢弃期间新增成绩/作答。优先修复并继续向前迁移。回退演练仅在独立副本进行，本次未对正式库执行恢复。

文件删除失败保留DELETE_PENDING和任务错误，修复目录权限/占用后到期自动重试。租约过期可重新领取，磁盘已删除但数据库未提交时再次删除为幂等。ACTIVE_FILE_MISSING从一致备份恢复同一 storage_key 的原字节并核对size/sha；核对通过后由运维标记 file_reconciliation.resolved_at。隔离目录中的孤儿先核对来源，不直接删除或建立虚假元数据。

监控 file_cleanup_task 的 last_error/attempts/next_attempt_at、file_reconciliation 的未解决记录、background_failure 和超时session积压。只读诊断查询与人工恢复须使用受限运维账号，不能通过普通用户接口跨用户处理。
