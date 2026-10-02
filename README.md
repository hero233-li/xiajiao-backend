# 学习知途后端

已补齐现有OpenAPI约定的业务后端：课程与报考、目录进度、知识与手册、备注、练习与检测、真题与成绩、预测趋势、学习计划与顺延、首页聚合、内容草稿发布、管理审核及旧记录归档。完整范围和验证见[后端业务收尾记录](../docs/阶段10_业务后端收尾.md)。此前四批记录保留在阶段6至9文档中。

使用Java21、Spring Boot3.5.16、Maven、Spring Security、MySQL8.4、Flyway和springdoc。JUnit5、MockMvc与独立Testcontainers数据库用于验证。真实试卷、题目原创资格和模拟权重需要管理员录入审核；功能实现不会自动放行未经审核的数据。

## 结构

```text
backend/
  pom.xml
  src/main/java/cn/xuexizhitu/
    XuexiZhituApplication.java
    controller/  HTTP入口和DTO校验
    service/     认证、学习内容、练习检测、真题成绩、计划看板、管理审核及历史业务
    repository/  认证JPA仓库及业务JDBC查询
    dto/         输入/输出，与Entity分离
    entity/      AppUser、LoginIdentifier、Role
    config/      安全、JWT、注册开关、初始化配置、OpenAPI
    security/    JWT用户转换、CurrentUser
    common/      响应、错误码、异常处理
    validation/  密码校验及OpenAPI请求结构校验
  src/main/resources/application.yml
  src/main/resources/db/migration/  V1至V6结构及内容迁移
  src/test/java/cn/xuexizhitu/
```

Flyway负责全部54张已确认表；JPA只映射当前认证所需的两张表，`ddl-auto=validate`、`open-in-view=false`，不会自动建表。V1来自确认DDL，仅将MySQL不接受的`CHECK (read_only)`改为等价的`CHECK (read_only = 1)`，同步了设计SQL和数据字典。V2另导入旧项目六科公共课程及目录（47章、546条目）和2026年10月周期，不导入个人状态。V3另创建六科第二版发布并导入知识及手册；章节和条目ID保持不变，个人进度不复制或重置。V4为四门理论课创建第三版发布并导入题库与426个考点，新增五个系统题库章节；当前共52章、546阅读条目，业务表仍54张。提交结果使用JSON快照保证幂等重试返回原统计。V5补齐检测运行字段、超时索引及四门理论课默认策略；题目资格仍待审核。V6增加草稿内容快照；编辑草稿不会直接改变当前发布内容。V1至V5迁移未修改。

## 本地运行

在仓库根目录`xiajiao/repo/`准备配置：

```bash
cp .env.example .env
```

编辑`.env`，替换DB_PASSWORD、MYSQL_ROOT_PASSWORD、APP_BOOTSTRAP_PASSWORD；使用以下命令生成JWT_SECRET_BASE64并把结果填入`.env`：

```bash
openssl rand -base64 32
```

初始化密码至少8个字符，UTF-8总长度不超过72字节。首次启动保留APP_BOOTSTRAP_ENABLED=true，填写本人的用户名/邮箱。注册范围待确认，APP_AUTH_REGISTRATION_MODE保持DISABLED。

准备Java21，并把JAVA_HOME设为该JDK根目录（包含bin/java）；当前机器默认Java17，不能直接构建。检查：

```bash
java -version
mvn -version
```

两者应使用Java21。本次验证下载的临时JDK仍在本机时，可直接设置（正式使用可换为你自己的Java21安装路径）：

```bash
export JAVA_HOME="/private/tmp/xiajiao-jdk21/amazon-corretto-21.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

然后在仓库根目录执行：

```bash
set -a
source .env
set +a
docker compose up -d --wait
export DB_URL="jdbc:mysql://127.0.0.1:${MYSQL_PORT:-3306}/xuexizhitu?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8&sslMode=DISABLED&allowPublicKeyRetrieval=true"
cd backend
mvn spring-boot:run
```

`.env`须为可加载的键值配置；含空格或shell特殊字符的值使用单引号包围。数据库端口默认3306，只绑定127.0.0.1；如本机端口已占用，调整MYSQL_PORT，上面DB_URL会随之匹配。示例数据库连接参数用于本机Docker，远程部署应提供适合其环境的DB_URL。

首次连接独立空库，Flyway执行V1至V6并记录版本，随后按配置创建首个ADMIN。重复初始化同一账号不会重置密码；已有其他账号时拒绝再创建首个管理员。初始化完成后把APP_BOOTSTRAP_ENABLED改为false，并从运行环境移除初始化密码。

默认服务地址`http://localhost:8080`，可用SERVER_PORT修改。正常停止数据库保留数据卷：

```bash
# 在仓库根目录执行
docker compose down
```

## 打包运行

保持上面的Java21及环境变量：

```bash
cd backend
mvn clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

无需前端工程即可验证后端。真实`.env`、target和日志已列入忽略规则。

## 健康与认证

统一响应：成功`{"code":0,"data":...,"message":"ok"}`；失败非0、data=null、中文原因。私有响应为Cache-Control:no-store；安全过滤器的401/403也返回JSON。未知请求字段拒绝，不接受role/userId等越权输入。

| 方法与路径 | 行为 |
|---|---|
| GET `/api/v1/health` | 无需登录，返回data.status=UP；仅HTTP进程存活，不代表数据库当前就绪 |
| POST `/api/v1/auth/login` | body为identifier/password；按用户名或邮箱原值匹配，返回访问/刷新令牌、到期时间、用户 |
| POST `/api/v1/auth/register` | body为username/email/password；默认关闭，返回40301；配置启用时只创建USER |
| POST `/api/v1/auth/refresh` | body为refreshToken；轮换后返回新令牌对，旧令牌拒绝重放 |
| GET `/api/v1/auth/me` | 使用访问令牌，返回当前用户DTO，无密码哈希 |
| POST `/api/v1/auth/logout` | 使用访问令牌；撤销账号全部访问/刷新令牌，成功data=null |

健康检查：

```bash
curl http://localhost:8080/api/v1/health
```

应返回：

```json
{"code":0,"data":{"status":"UP"},"message":"ok"}
```

登录请求体：

```json
{"identifier":"你的用户名或邮箱","password":"你的密码"}
```

将该JSON通过POST发送到`/api/v1/auth/login`。返回data.accessToken用于`Authorization: Bearer <accessToken>`；data.refreshToken只用于刷新，不能访问me或其他受保护接口。令牌不要作为URL参数。登录失败对不存在账号、错误密码及停用账号统一返回40102。

JWT使用HS256，至少32随机字节密钥；校验签名、issuer/audience、有效期、类型和用户token_version，每次受保护请求还检查账号启用和当前角色。默认访问15分钟、刷新7天，可用JWT_ACCESS_TTL/JWT_REFRESH_TTL配置。此实现沿用已确认的用户级token_version，不新增会话表：刷新/退出会使该账号其他设备的旧令牌一并失效。若未来需各设备独立刷新/退出，需要先设计会话表。

注册模式：DISABLED（当前默认）、ADMIN_ONLY（仅持有效访问令牌的ADMIN创建USER）、PUBLIC（无需登录但只能创建USER）。本次尚未确认允许范围，默认保持DISABLED；不要在未确认时启用其他模式。ADMIN仅由首个初始化流程建立，客户端不能指定role。

当前用户工具：`CurrentUser.require()`、`CurrentUser.idOrThrow()`；身份从Spring Security上下文获得，未认证抛40101。业务阶段须在Service使用本人ID限制数据范围，业务查询及写入使用当前用户ID隔离；目录进度和报考写入使用修订号防止覆盖。

## 接口文档

完整评审契约为`../docs/openapi.yaml`，已同步基础健康、注册开关和刷新字段。现有契约全部104个操作已实现，其中98个业务操作的真实响应通过契约校验；认证和健康检查另有独立测试。springdoc生成当前实际实现的接口，评审YAML用于精确输入输出约定。`/v3/api-docs`和Swagger资源只允许ADMIN访问，可用ADMIN访问令牌请求JSON；浏览器直接访问而不携带Bearer会返回401。

## 测试与验证

基础测试不需要数据库或Docker：

```bash
mvn test
```

完整构建与真实MySQL集成测试（先启动Docker Desktop）：

```bash
mvn -Pmysql-it verify
```

Testcontainers自动创建独立MySQL8.4并清理，测试配置不连接compose数据库，也不会读取真实账号密码。包括54表/Flyway版本/CHECK约束、用户名与邮箱登录、命名空间冲突回滚、刷新重放、旧访问令牌失效、刷新类型隔离、退出撤销、默认关闭注册与健康检查。基础测试另覆盖统一错误、参数校验/未知字段拒绝、JWT过期/issuer/audience/签名、注册模式角色约束及UTF-8密码长度。

当前后端71项测试通过（23项基础/算法测试、48项真实MySQL集成测试）；98个业务操作全部有真实响应样本并通过OpenAPI校验。前端176项测试及构建通过。此前前端单元测试使用Mock；现已另用repo/frontend完成真实浏览器联调，详见[前后端联调记录](../docs/阶段11_前后端真实联调.md)，仍未生产部署。验证细节见阶段10文档。

接口响应和请求结构快照检查（从仓库根目录运行，使用当前前端已有依赖）：

```bash
node backend/scripts/validate-learning-contract.cjs ../../frontend/node_modules --all
node backend/scripts/generate-business-schemas.cjs ../../frontend/node_modules --check
```

私有文件默认保存在后端工作目录的`data/private`，用`APP_FILES_ROOT`配置持久化目录；不要放在公开静态目录。数据库和文件目录需一起备份。上传限制8MB，实际校验文件内容，不信任客户端MIME；附件删除先阻止下载，后台每30秒清理实体文件。`APP_FILES_CLEANUP_ENABLED=false`仅用于显式任务测试，正常运行保持开启。检测超时结算任务也默认开启。

## 版本依据

采用Spring Boot3的3.5系列；[官方环境要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)支持本工程Java21/Maven环境。[springdoc官方兼容表](https://springdoc.org/v2/faq.html)对应3.5.x使用2.8.x。持久化选择沿用阶段2建议JPA，依赖其余版本由Spring Boot BOM管理。
