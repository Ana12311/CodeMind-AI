# 系统架构

## 1. 整体架构

CodeMind AI 由三个独立服务组成，通过 HTTP 协作：

```
Frontend（React / Nginx）
    ↓ HTTP
CodeMind AI Backend（Java 17 / Spring Boot）
    ↓ HTTP（提交任务）
AI Services（Python / FastAPI）
    ↓ HTTP（结果回调）
CodeMind AI Backend（保存结果）
```

- **Frontend**：React 单页应用，负责交互与结果可视化（Monaco 高亮定位），Nginx 托管并反代 `/api`。
- **CodeMind AI Backend**：业务系统，对外提供 REST API，管理用户、权限、项目、文件、AI 任务与审查结果。
- **AI Services**：AI 执行系统，接收任务后异步执行 Agent 工作流 + RAG，完成后回调业务系统保存结果。

## 2. 服务职责

### CodeMind AI Backend

| 模块 | 职责 |
|------|------|
| `controller` | REST 接口层，含 Auth / Project / File / AiTask / AiReviewResult / User / Role / AiCallback |
| `service` + `service/impl` | 业务逻辑层 |
| `mapper` | MyBatis-Plus 数据访问层 |
| `entity` | 数据库实体（sys_user / sys_role / sys_user_role / project / code_file / ai_task / ai_review_result） |
| `security` | Spring Security + JWT 认证，HMAC 内部服务鉴权 |
| `client` | WebClient 客户端，调用 FastAPI |
| `config` | 配置类（Security、MyBatis-Plus、Redis、WebClient、AdminInitializer 等） |

核心职责：

- 用户认证与令牌管理（JWT access + refresh，Redis 存储刷新令牌与黑名单）。
- RBAC 权限控制（ADMIN / USER）。
- 项目、代码文件、AI 任务、审查结果的管理。
- 向 FastAPI 提交任务，接收并保存回调结果。

### AI Services

| 模块 | 职责 |
|------|------|
| `app/api` | FastAPI 路由（`/api/tasks`） |
| `app/agents` | Agent 工作流（Planner / Worker / Reviewer） |
| `app/rag` | RAG 系统（加载、切片、检索、代码上下文） |
| `app/embeddings` | Embedding 提供方（hashing / bge-m3） |
| `app/vectorstore` | 向量存储（内存 + JSON 持久化） |
| `app/services` | 任务编排、回调、LLM 服务 |
| `app/config` | 配置、日志、异常处理 |
| `app/prompts` | Prompt 模板（planner / worker / reviewer / code_review） |

核心职责：

- 接收任务，异步调度 Agent 工作流。
- RAG 检索增强，为 LLM 提供代码上下文。
- 调用 LLM 生成审查报告。
- 回调业务系统保存结果。

## 3. 模块划分

### 业务系统分层

```
Controller → Service → Mapper → MySQL
                 ↓
              Redis（令牌 / 黑名单）
                 ↓
              AiServiceClient（WebClient）→ FastAPI
```

### AI 服务分层

```
API → TaskService → AgentWorkflow（Planner → Worker → Reviewer）
                          ↓
                     LLMService（mock / deepseek）
                          ↓
                     RAG Pipeline / Code Review RAG
                          ↓
                     Embedding + VectorStore
```

## 4. 通信方式

### Java → Python（提交任务）

- `POST /api/tasks`，JSON 载荷：`{taskId, taskType, projectId, content}`。
- Java 侧 WebClient 调用，超时 10 秒，仅取 `status` 确认，不等待最终结果。
- 调用失败不影响业务，任务置为 FAILED。

### Python → Java（结果回调）

- `POST {CALLBACK_URL}`，默认 `http://localhost:8080/api/ai/task/callback`。
- 载荷：`{taskId, status, result}`。
- Java 侧 `AiCallbackController` 接收，更新任务状态并保存 `ai_review_result`。

### 内部接口鉴权

Java 侧暴露给 AI 服务 / 内部调用的接口使用 HMAC-SHA256 签名 + 时间戳防重放鉴权（`InternalAuthFilter`）：

- 请求头：`X-Timestamp`（epoch 毫秒）、`X-Signature`（hex 小写）。
- 时间戳窗口 5 分钟。
- 签名串：`METHOD + "\n" + (URI + query) + "\n" + timestamp + "\n" + body`。

涉及的内部接口：

- `GET /api/v1/ai-tasks/pending`
- `PUT /api/v1/ai-tasks/{id}/status`
- `PUT /api/v1/ai-tasks/{id}/result`
- `POST /api/v1/ai-reviews`
- `POST /api/ai/task/callback`

## 5. 数据流

用户提交审查任务的数据流：

1. 用户（JWT 认证）调用 `POST /api/v1/ai-tasks` 创建任务。
2. 后端落库（状态 WAITING），校验项目权限。
3. 后端 WebClient 提交 FastAPI。
4. FastAPI 返回 PROCESSING，后台线程池执行 Agent 工作流。
5. Agent 执行中通过 RAG 检索代码上下文，调用 LLM 分析。
6. 执行完成后 FastAPI 回调 Java。
7. Java 更新任务状态（SUCCESS / FAILED），保存 `ai_review_result`。
