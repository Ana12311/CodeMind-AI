# CodeMind AI Docker Compose 部署验收报告

- 日期：2026-08-26
- 范围：根目录 `docker-compose.yml` 总控编排 + 一条命令启动完整系统
- 约束遵循：Docker 编排阶段未修改业务代码；后续经用户授权修复 G1（回调 HMAC 签名），改动仅 Python 侧 `callback_service.py` / `config.py` / `task_service.py`，后端 Java 未动。

---

## A. docker-compose 结构

根目录 `docker-compose.yml`（总控），四个服务：

```
docker-compose.yml
├── name: codemind
├── networks: codemind-network（bridge）
├── volumes:  mysql-data / redis-data / backend-uploads
└── services
    ├── mysql        image mysql:8.0，挂载 schema.sql 初始化
    ├── redis        image redis:7-alpine，AOF 持久化
    ├── backend      build ./CodeMind AI Backend，8080
    └── ai-service   build ./AI Services，8000，挂载 bge-m3 模型
```

关键编排点：

- 容器内互访全部使用服务名（`mysql` / `redis` / `ai-service` / `backend`），无 `localhost`。
- Backend `depends_on` mysql / redis / ai-service 三个 `service_healthy`，避免启动顺序错误。
- 四服务均带 healthcheck。
- Backend 上传目录 `/app/uploads` 持久化到 `backend-uploads` 卷。
- AI Service 只读挂载三处宿主机目录：`models`（bge-m3 权重 2.2GB）、`docs`（RAG 文档入库）、`codes`（CODE_REVIEW 代码入库）。

## B. 服务列表

| 服务 | 镜像/构建 | 容器名 | 端口（宿主机→容器） | 健康检查 |
|------|-----------|--------|---------------------|----------|
| mysql | mysql:8.0 | codemind-mysql | `${MYSQL_HOST_PORT}`:3306 | mysqladmin ping |
| redis | redis:7-alpine | codemind-redis | `${REDIS_HOST_PORT}`:6379 | redis-cli ping |
| backend | build（多阶段 Gradle→JRE） | codemind-backend | `${APP_HOST_PORT}`:8080 | TCP 8080 探测 |
| ai-service | build（python:3.12-slim + torch CPU） | codemind-ai-service | `${AI_HOST_PORT}`:8000 | HTTP GET /health |

Backend 无 HTTP 健康接口（无 actuator/health controller），故用 `/dev/tcp` 端口探测代替。

## C. 网络设计

单一自定义 bridge `codemind-network`，四服务同网：

- backend 可达：mysql、redis、ai-service
- ai-service 可达：backend（回调）

通信链路：

- Backend 提交任务：`AI_SERVICE_URL=http://ai-service:8000`（POST `/api/tasks`）
- AI Service 回调结果：`CALLBACK_URL=http://backend:8080/api/ai/task/callback`

## D. 环境变量设计

单一源根目录 `.env.example`（占位符），覆盖两个服务，Docker 相关端口映射变量：

| 变量 | 服务 | 用途 |
|------|------|------|
| `DB_PASSWORD` | mysql/backend | MySQL root 密码（必填） |
| `DB_NAME` | mysql/backend | 库名，默认 codemind |
| `JWT_SECRET` | backend | JWT HS256 密钥，≥32 字节（必填） |
| `INTERNAL_SECRET` | backend | 内部服务 HMAC 密钥（必填） |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | backend | 首启创建管理员（密码 ≥8 位含字母数字） |
| `DEEPSEEK_API_KEY` | ai-service | 使用 deepseek 时必填 |
| `LLM_PROVIDER` | ai-service | `mock` / `deepseek` |
| `EMBEDDING_PROVIDER` / `EMBEDDING_MODEL_NAME` | ai-service | bge-m3 配置 |
| `AI_SERVICE_URL` | backend | 编排内覆盖为 `http://ai-service:8000` |
| `CALLBACK_URL` | ai-service | 编排内覆盖为 `http://backend:8080/...` |
| `MYSQL_HOST_PORT` / `REDIS_HOST_PORT` / `APP_HOST_PORT` / `AI_HOST_PORT` | 编排 | 宿主机端口映射，避免与原生服务冲突 |

`.env.example` 仅占位符，无真实值；本地联调用 `.env`（已被 `.gitignore` 忽略）。

## E. 启动结果

`docker compose up -d --build` 一次成功构建两个镜像并启动四服务。

```
NAME                  STATUS
codemind-ai-service   Up (healthy)
codemind-backend      Up (healthy)
codemind-mysql        Up (healthy)
codemind-redis        Up (healthy)
```

- 宿主 3306/6379/8000 已被原生 MySQL/Redis/AI 服务占用，故编排端口映射为 `3307/6378/8001`（8080 空闲沿用）。
- Backend 启动日志：`Started CodeMindAiBackendApplication in 4.387 seconds`；`AdminInitializer: 管理员初始化完成 username=admin, role=ADMIN`。
- MySQL 首启自动建库建表 + 角色种子（schema.sql）。

## F. 全链路测试结果

真实 HTTP 调用验证（LLM 用 mock，因无 `DEEPSEEK_API_KEY`）：

| 步骤 | 结果 |
|------|------|
| 1. 用户登录（admin，JWT） | ✅ 通过，返回 accessToken |
| 2. 创建项目 | ✅ 通过 |
| 3. 上传代码文件 | ✅ 通过（修复上传目录权限后） |
| 4. 创建 CODE_REVIEW 任务 | ✅ 通过，状态 PROCESSING |
| 5. Backend 调 FastAPI（POST /api/tasks） | ✅ 通过，200，任务受理 |
| 6. Agent 执行（Planner → Worker → Reviewer，mock LLM） | ✅ 通过，`reviewer 审核完成` |
| 7. RAG 检索（bge-m3） | ✅ 通过（加载模型 + 编码 9 向量 + 检索命中 3 代码块） |
| 8. LLM 分析 | ⚠️ mock 替代（真实 LLM 待 DEEPSEEK_API_KEY，见 G4） |
| 9. Callback 回调 Backend | ✅ 通过（HMAC 签名修复后） |
| 10. Java 保存结果 | ✅ 通过（ai_review_result 落库，detail 含检索代码上下文） |

结论：**全链路 SUCCESS 已打通**——登录 → 项目 → 上传 → 建任务 → 提交 → Agent（mock LLM）→ 回调（HMAC 签名）→ 结果落库，任务终态 `status=2 成功`。

实测日志（关键行）：

```
任务受理 taskId=2092445708804907009 taskType=CODE_REVIEW
MockProvider 调用，prompt 长度=419   # Planner
MockProvider 调用，prompt 长度=581   # Worker
MockProvider 调用，prompt 长度=1700  # Reviewer
reviewer 审核完成
POST http://backend:8080/api/ai/task/callback "HTTP/1.1 403"
回调最终失败 taskId=2092445708804907009
回调失败，任务标记 FAILED
```

## G. 遗留问题

### G1.【已修复】回调 HMAC 未签名 + result 字段类型不匹配

- 现象：AI Service 回调 Backend 被 `InternalAuthFilter` 拒 403，任务无法 SUCCESS。
- 根因（两处，均在 Python 侧）：
  1. `callback_service.py` 回调未携带 `X-Timestamp` / `X-Signature` HMAC 签名。
  2. `AiTaskCallbackRequest.result` 为 `String`，但 AI Service 发送的是 JSON 对象，Jackson 反序列化失败（「请求体格式错误」），结果无法落库。
- 修复：
  1. `callback_service.py` 按 canonical 规则（`METHOD\npath+query\ntimestamp\nbody`）计算 HmacSHA256，加 `X-Timestamp` / `X-Signature` 头；`config.py` 新增 `internal_secret`；`task_service.py` 传入密钥；编排给 ai-service 注入 `INTERNAL_SECRET`。
  2. `callback_service.py` 将 dict/list 类型的 `result` 序列化为 JSON 字符串，匹配后端 String 字段。
- 验证：全链路 SUCCESS，任务终态 `status=2 成功`，`ai_review_result` 落库（detail 2205 字符）。

### G2.【已修复】Backend 上传目录无写权限

- 现象：上传文件返回 400「文件保存失败」。根因：Backend 镜像 `USER app` 非 root，`/app/uploads` 无写权限，`LocalStorageServiceImpl` 写文件抛 IOException。
- 修复：Dockerfile 增加 `RUN mkdir -p /app/uploads && chown -R app:app /app/uploads`；编排挂载 `backend-uploads:/app/uploads` 持久化。已验证上传通过。

### G3.【已修复】AI Service 镜像缺 `docs` 目录

- 现象：启动后 RAG 入库日志 `RAG 入库失败，跳过增强：路径不存在: docs`。
- 根因：`.dockerignore` 有意排除 `docs/`（本地开发脚手架），Dockerfile 未复制、编排未挂载 docs 目录。
- 修复：编排将 `./AI Services/docs` 只读挂载到 `/app/docs`，`RAG_DOCS_DIR=docs` 入库成功（`RAG 入库 docs -> 1 块`），告警消失。

### G4.【部分修复】真实 LLM 未实测 / bge-m3 检索已实测

- 真实 LLM：无 `DEEPSEEK_API_KEY`，本次 `LLM_PROVIDER=mock`，Agent 流程通过但「LLM 分析」为 mock 输出。**待提供真实 key 后复测。**
- bge-m3 检索：已实测通过。编排挂载 `./codes` 到 `/code` 并设 `CODE_REVIEW_DIR=/code`；`CodeLoader` 加载 2 文件 → bge-m3 编码 9 向量 → 检索命中 3 代码块，代码上下文进入评审结果（detail 含 OrderService / user_repo / SQL 注入内容）。新增 `CODE_REVIEW_MIN_SCORE=0.3`（跨语言通用查询实测 0.3~0.45，低于默认 0.5 会恒空）。

---

**验收结论**：Docker Compose 总控编排达标——一条命令启动四服务、网络互通、健康检查、模型挂载、环境变量注入、数据持久化均正确；全链路（含回调 HMAC 签名）已打通至 SUCCESS；bge-m3 检索（加载 + 编码 + 检索）实测通过。剩余项：真实 LLM（需 `DEEPSEEK_API_KEY`）待配置后复测。
