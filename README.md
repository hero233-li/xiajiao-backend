# 学习知途后端

Java17、Spring Boot3.5.16、MySQL8.4、Flyway的模块化单体。实际前端位于相邻的 `../frontend` 仓库。现有业务覆盖身份认证、科目周期报考、目录进度、知识手册笔记、练习错题、检测判分、真题成绩预测、计划顺延、管理审核和历史归档。

[重构交付与验证](docs/refactor/06-delivery.md) · [真实业务基线](docs/refactor/01-business-baseline.md) · [设计与实施顺序](docs/refactor/02-design-and-sequence.md) · [数据迁移、备份、部署、回退](docs/refactor/03-migration-and-operations.md) · [接口映射](docs/refactor/04-interface-map.md) · [OpenAPI](docs/openapi.yaml)

## 模块

```text
cn.xuexizhitu/
  identity/     用户、认证、令牌
  learning/     科目、周期、目录与个人进度
  content/      知识手册、草稿、发布与版本
  practice/     练习、错题、作答历史和门槛
  assessment/   组卷、计时、判分、通过与审核
  exams/        真题、成绩、图片、预测与修订历史
  grading/      答卷、版本化评分标准、租约任务与本地工作程序配对
  planning/     计划、完成状态、顺延与确认
  archive/      旧资料与历史认领
  files/        私有存储、恢复日志和删除租约
  dashboard/    首页聚合
  audit/        审计持久化
  operations/   就绪检查、后台失败追踪和管理入口
  common/ config/ security/ validation/ contract/
```

模块内使用 api/application/domain/infrastructure，领域对象表达真实规则，不机械增加接口。旧controller/service/repository/dto/entity目录已替换；使用同一套运行实现。规则和历史快照由应用层事务统一组织。

## 构建和验证

```sh
mvn -Pmysql-it verify
```

需要Java17和Docker Desktop。Testcontainers创建独立MySQL8.4，测试不会连接业务库。实际浏览器联调另外需要相邻前端已安装依赖与Playwright Chromium；`RefactorHttpMySqlIT`自动启动临时前端、后端和独立数据库，结束后关闭。普通算法/认证/输入验证可运行 `mvn test`。

```sh
cd ../frontend
npm ci
npm run api:check
npm test
npm run build
```

契约快照与真实响应核对（从两个仓库的父目录执行）：

```sh
node backend/scripts/generate-business-schemas.cjs frontend/node_modules --check
python3 backend/scripts/generate-workflow-dtos.py --check
node backend/scripts/validate-learning-contract.cjs frontend/node_modules --all
```

## 运行

先按迁移说明备份并在独立副本验证已有数据。保留用户的本地application.yml修改；部署使用production profile显式提供 DB_URL、DB_USER、DB_PASSWORD、JWT_SECRET_BASE64、APP_FILES_ROOT，避免继承本地默认凭据。

```sh
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

端口默认8080，可设置SERVER_PORT。Flyway新增V8/V9/V10，不修改V1–V7；Hibernate只验证结构。私有文件目录不能公开托管，数据库和文件必须一致备份。迁移时先禁用文件清理和超时任务、核对后再恢复。

前端本地开发设置 `API_PROXY_TARGET=http://127.0.0.1:8080`、`VITE_API_MOCK=false`，运行 `npm run dev`；生产发布dist并代理 `/api`。不要把Mock示例当作正式业务数据。

存活 GET `/api/v1/health`，数据库就绪 GET `/api/v1/health/readiness`。受保护接口使用 `Authorization: Bearer <accessToken>`，响应禁止缓存。刷新/退出按既有用户级token_version撤销旧令牌。注册默认DISABLED，现有用户保留；本次不改变注册政策。

真实题目原创资格、近五年权重、试卷和复习模板须管理员维护。新建/完整编辑五周计划每科必须有REVIEW模板及估时，缺少时提示补齐；既有默认任务快照保持。上传成绩图片仍是私有存档。独立答卷新增[服务器申请、Mac本地Codex批改](docs/grading-delivery.md)，工作程序与安装步骤见[mac-grader](mac-grader/README.md)。网页新增答卷批改入口。

V9包含新非空成绩身份字段，**回退不能只切换旧jar**；必须遵循一致备份恢复与新增数据保全步骤。本次未部署正式环境。
