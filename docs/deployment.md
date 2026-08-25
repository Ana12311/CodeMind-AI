# 部署与启动

当前部署方式为本地启动（源码运行）。Docker 部署尚未完成。

## 1. 前置条件

- JDK 17
- MySQL 8.x
- Redis
- Python 3.10+（AI 服务）

## 2. 环境变量

根目录 `.env.example` 提供全部环境变量占位模板。两个服务所需关键变量：

| 变量 | 服务 | 必填 | 说明 |
|------|------|------|------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | Java | 是 | MySQL 连接 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Java | 是 | Redis 连接 |
| `JWT_SECRET` | Java | 是 | JWT HS256 密钥，≥32 字节 |
| `INTERNAL_SECRET` | Java | 是 | 内部服务 HMAC 签名密钥 |
| `AI_SERVICE_URL` | Java | 是 | FastAPI 地址，默认 `http://localhost:8000` |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Java | 条件 | 首次启动创建管理员（密码 ≥8 位含字母数字） |
| `CALLBACK_URL` | Python | 条件 | 结果回调地址，空则不回调 |
| `LLM_PROVIDER` | Python | 是 | `mock` / `deepseek` |
| `DEEPSEEK_API_KEY` | Python | 条件 | 使用 deepseek 时必填 |
| `EMBEDDING_PROVIDER` | Python | 否 | `hashing`（默认）/ `bge-m3` |
| `CODE_REVIEW_DIR` | Python | 否 | 代码审查入库目录，空则不启用 |

Java 侧通过 `${VAR}` 占位符读取环境变量；Python 侧 pydantic-settings 读取 `.env` 与系统环境变量。

## 3. 启动 Backend

1. 建库建表：

```bash
mysql -uroot -p < "CodeMind AI Backend/src/main/resources/schema.sql"
```

2. 配置环境变量，或将 `application-example.properties` 复制为 `application.properties` 填写真实值。

3. 启动：

```bash
cd "CodeMind AI Backend"
./gradlew bootRun
```

默认监听 8080 端口（可用 `SERVER_PORT` 覆盖）。

首次启动时，若数据库无管理员且 `ADMIN_PASSWORD` 已配置，`AdminInitializer` 自动创建管理员并分配 ADMIN 角色；未配置则启动失败（fail-fast）。

## 4. 启动 AI Service

1. 安装依赖：

```bash
cd "AI Services"
pip install -r requirements.txt
```

2. 配置 `.env`（可复制 `.env.example`）。

3. 启动：

```bash
uvicorn app.main:app --reload
```

默认监听 8000 端口，健康检查 `GET /health`。

## 5. 联调链路

1. 启动 MySQL、Redis。
2. 启动 Backend（8080）。
3. 启动 AI Service（8000）。
4. 前端（或接口工具）登录获取 JWT，创建项目、上传代码、创建 CODE_REVIEW 任务。
5. Backend 提交任务到 AI Service，AI Service 异步执行后回调 Backend，结果落库。

## 6. 说明

- Docker 部署尚未完成，本文件不提供 Docker 启动方式。
- 生产环境必须通过环境变量注入密钥，禁止在配置文件提交真实密码 / 密钥。
