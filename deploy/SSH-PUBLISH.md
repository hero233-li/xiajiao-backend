# 1Panel SSH 增量发布

服务器通过 GitHub SSH 443 同步 main，不调用 GitHub REST API。

## 一次性授权

在仓库 Settings → Deploy keys → Add deploy key 中添加服务器公钥：
`/home/ubuntu/.ssh/xiajiao-backend-deploy.pub`。
不要勾选 Allow write access。私钥仅保留在服务器。

## 1Panel Shell 任务

用户 root 或 ubuntu，解释器 /bin/bash，不勾选在容器中执行。

```bash
/bin/bash /opt/projects/xiajao/deploy.sh backend
```

手动发布建议关闭自动定时执行；失败重试次数设为 0。

Git 缓存：`/opt/projects/xiajao/.deploy/git/xiajiao-backend.git`。
首次 fetch 获取仓库 main 的历史，后续仅传输缺少的 Git 对象。
源代码从缓存导出至独立发布目录，不包含服务器运行配置和数据。
版本未变化时不构建或重启；有变更时复用 Docker 构建缓存。
访问失败时发布停止，当前容器保持运行。
