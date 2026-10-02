# 云服务器部署

当前服务器通过 `ssh learning` 登录。前端为 `xiajiao-frontend` 容器，入口为 `http://124.222.151.76:8080`；数据库使用已有的 1Panel MySQL 8.4 容器 `1Panel-mysql-uQU5`，连接网络 `1panel-network`。

后端目录为 `/opt/projects/xiajao/backend-java`，使用 Java 17 运行镜像。后端不映射公网端口，前端通过 `xiajiao-network` 内的 `backend:8080` 转发 `/api/`。

## 首次迁移

先导出本地 `xuexizhitu` 数据库，包含 `flyway_schema_history`；同时复制 `data/private`。数据库导出使用单事务，迁移期间不要继续修改本地学习记录或上传附件。服务器创建独立数据库和仅授权该数据库的业务账号，再导入备份。已有管理员随数据库保留，不执行管理员初始化。

`deploy/.env` 保存服务器专用数据库密码和随机 JWT 密钥，权限设为 600，不提交 Git。附件不提交 Git，宿主机目录须允许容器用户 10001 读写。生产 JWT 密钥与本地不同，原登录密码保留，登录令牌需重新获取。

创建网络并接入现有前端（已连接时跳过）：

```sh
docker network create xiajiao-network
docker network connect xiajiao-network xiajiao-frontend
```

## 更新后端

在本地后端目录使用 Java 17 执行 `mvn clean package`，将 JAR 和部署文件传到服务器对应目录。只有改代码时替换 JAR；不要用本地数据库再次覆盖服务器数据库。

在服务器执行：

```sh
cd /opt/projects/xiajao/backend-java
docker compose -f deploy/compose.yml build
docker compose -f deploy/compose.yml up -d
docker compose -f deploy/compose.yml logs --tail=80 backend
curl --fail http://127.0.0.1:8080/api/v1/health
```

健康接口只验证 HTTP 服务。还应验证登录、课程和个人记录，以及附件读取。更新前保留旧 JAR、数据库备份和附件备份；数据库迁移执行后，不应只回退 JAR 而忽略数据库兼容性。

数据库和 `data/private` 必须一起备份，备份含个人数据，应限制访问。不要执行数据库删除、Docker 数据卷删除或 Flyway clean。

当前入口使用 HTTP，账号密码和令牌缺少传输加密；开放给他人使用前应配置域名和 HTTPS。
