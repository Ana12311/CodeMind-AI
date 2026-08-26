# 部署与启动

支持本地源码运行与 Docker 部署两种方式。三个服务均已 Docker 化。

## 0. 总控编排（推荐）

根目录 `docker-compose.yml` 一条命令启动完整系统（MySQL + Redis + Backend + AI Service + Frontend），五个服务加入同一 `codemind-network` 网络，容器内互访用服务名（禁止 localhost）。

```bash
cp .env.example .env      # 填写 DB_PASSWORD / JWT_SECRET / INTERNAL_SECRET / ADMIN_PASSWORD 等
docker compose up -d --build
docker compose ps          # 确认 5 个服务 running
docker compose logs -f backend
docker compose down        # 停止；down -v 连数据卷一起清空（谨慎）
```

服务与端口（宿主机 → 容器）：

| 服务 | 容器名 | 宿主机端口 | 容器内地址 |
|------|--------|-----------|-----------|
| frontend | codemind-frontend | `${FRONTEND_HOST_PORT:-80}` | `frontend:80` |
| backend | codemind-backend | `${APP_HOST_PORT:-8080}` | `backend:8080` |
| ai-service | codemind-ai-service | `${AI_HOST_PORT:-8000}` | `ai-service:8000` |
| mysql | codemind-mysql | `${MYSQL_HOST_PORT:-3306}` | `mysql:3306` |
| redis | codemind-redis | `${REDIS_HOST_PORT:-6379}` | `redis:6379` |

要点：

- Backend 通过 `AI_SERVICE_URL=http://ai-service:8000` 提交任务；AI Service 通过 `CALLBACK_URL=http://backend:8080/api/ai/task/callback` 回调。
- Embedding 默认 `hashing`（离线零依赖）；切换 `bge-m3` 需先下载模型 `huggingface-cli download BAAI/bge-m3 --local-dir "AI Services/models/bge-m3"`，再改 `.env` 的 `EMBEDDING_PROVIDER=bge-m3`。模型约 2.2GB，宿主机挂载 `./AI Services/models` → 容器 `/models`（不进镜像）。
- RAG 文档挂载：`./AI Services/docs` → `/app/docs`，`RAG_DOCS_DIR=docs`（启动自动入库）。
- 代码审查代码挂载：`./codes` → `/code`，`CODE_REVIEW_DIR=/code`（空则不启用代码入库）。
- 宿主机已跑原生 MySQL/Redis/AI 服务时，改 `MYSQL_HOST_PORT` / `REDIS_HOST_PORT` / `AI_HOST_PORT` 避开端口冲突。
- MySQL 首启自动执行 `schema.sql` 建库建表 + 角色种子；Backend 首启自动创建管理员（`ADMIN_USERNAME` / `ADMIN_PASSWORD`）。
- 四个服务均配置 healthcheck，Backend 依赖 mysql / redis / ai-service 就绪后启动，避免启动顺序错误。

以下各节为分服务本地运行与单独 Docker 部署，均已被总控编排覆盖。

## 1. 前置条件

### 本地运行

- JDK 17
- MySQL 8.x
- Redis
- Python 3.10+（AI 服务）

### Docker 部署

- Docker + Docker Compose
- （可选）bge-m3 模型权重，放置于 `AI Services/models/bge-m3`（约 2.2GB，运行时卷挂载，不进镜像）

## 2. 环境变量

根目录 `.env.example` 为单一环境变量模板（占位符），覆盖两个服务。关键变量：

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

Java 侧通过 `${VAR}` 占位符读取环境变量；Python 侧 pydantic-settings 读取 `.env` 与系统环境变量（环境变量优先）。

## 3. 本地启动

### Backend

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

默认监听 8080（可用 `SERVER_PORT` 覆盖）。首次启动若数据库无管理员且 `ADMIN_PASSWORD` 已配置，`AdminInitializer` 自动创建管理员并分配 ADMIN 角色；未配置则启动失败（fail-fast）。

### AI Service

```bash
cd "AI Services"
python -m venv .venv
.venv/Scripts/activate           # Windows；Linux/macOS 用 source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

默认监听 8000，健康检查 `GET /health`。

## 4. Docker 部署

### Backend

多阶段构建：Stage 1 用 `gradle:9.5.1-jdk17` 打 fat jar，Stage 2 用 `eclipse-temurin:17-jre` 运行。敏感配置全部通过环境变量注入，镜像内无写死的密码 / secret。

```bash
cd "CodeMind AI Backend"
cp ../.env.example .env   # 填写 DB_PASSWORD / JWT_SECRET / INTERNAL_SECRET 等
docker build -t codemind-backend .
```

单容器运行（依赖本机 MySQL / Redis / FastAPI）：

```bash
docker run -d --name codemind-backend -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=3306 \
  -e DB_NAME=codemind \
  -e DB_USERNAME=root \
  -e DB_PASSWORD='<你的数据库密码>' \
  -e REDIS_HOST=host.docker.internal \
  -e REDIS_PORT=6379 \
  -e REDIS_DATABASE=0 \
  -e REDIS_PASSWORD='' \
  -e JWT_SECRET='<随机32字节以上>' \
  -e INTERNAL_SECRET='<随机密钥>' \
  -e AI_SERVICE_URL=http://host.docker.internal:8000 \
  codemind-backend
```

一键编排（MySQL + Redis + App）：

```bash
cd "CodeMind AI Backend"
cp ../.env.example .env
docker compose up -d --build
docker compose ps
docker compose logs -f app
docker compose down     # 停止并移除容器
docker compose down -v  # 连带清空数据卷（谨慎）
```

MySQL 容器首次启动通过挂载 `schema.sql` 自动初始化表结构与角色。

> 说明：容器内 `localhost` 指向容器自身。开发环境本机 MySQL/Redis/FastAPI 用 `host.docker.internal` 访问（Docker Desktop Windows/Mac 支持）；生产环境改为实际服务地址或容器网络。

### AI Service

基础镜像 `python:3.12-slim`，torch 走 CPU 构建（避免 PyPI 默认 CUDA 版拉取多 GB `nvidia_*` 依赖），bge-m3 模型外部挂载不打进镜像。

```bash
cd "AI Services"
cp ../.env.example .env   # 填写 DEEPSEEK_API_KEY / CALLBACK_URL 等
docker compose up -d --build
docker compose logs -f
```

`docker-compose.yml` 关键点：

- `env_file: .env` 注入密钥与通用配置。
- `EMBEDDING_MODEL_NAME=/models/bge-m3` 覆盖为容器内路径。
- `./models/bge-m3:/models/bge-m3:ro` 模型外部挂载。

## 5. 联调链路

1. 启动 MySQL、Redis。
2. 启动 Backend（8080）。
3. 启动 AI Service（8000）。
4. 前端（或接口工具）登录获取 JWT，创建项目、上传代码、创建 CODE_REVIEW 任务。
5. Backend 提交任务到 AI Service，AI Service 异步执行后回调 Backend，结果落库。

## 6. 安全说明

- 生产环境必须通过环境变量注入密钥，禁止在配置文件提交真实密码 / 密钥。
- `.env` 已被 `.gitignore` 忽略，仅 `.env.example`（占位符）入库。
- 国内加速构建可用 `--build-arg PIP_INDEX_URL` / `PIP_TORCH_WHEEL` 覆盖镜像源（见 `AI Services/Dockerfile`）。
