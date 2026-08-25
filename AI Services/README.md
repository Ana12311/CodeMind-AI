# AI Execution Service（FastAPI）

FastAPI AI 任务执行服务：任务生命周期管理 + Agent 工作流 + RAG（bge-m3）+ 向量存储 + LLM 调用 + Spring Boot 回调。

## 本地运行

```bash
python -m venv .venv
.venv/Scripts/activate           # Windows；Linux/macOS 用 source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Docker 运行

```bash
docker compose up -d --build
docker compose logs -f
```

bge-m3 模型走外部目录挂载（`./models/bge-m3` → 容器内 `/models/bge-m3`），不打进镜像。

## 环境变量

配置由 `pydantic-settings` 读取，字段即环境变量名（大小写不敏感）。密钥只经 `-e` / `.env` 注入，禁止写死。

### 应用 / 服务

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `APP_NAME` | AI Execution Service | 应用名 |
| `APP_ENV` | development | 环境 |
| `LOG_LEVEL` | INFO | 日志级别 |
| `API_PREFIX` | /api | 接口前缀 |

### LLM

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `LLM_PROVIDER` | mock | mock / deepseek |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek 密钥 |
| `DEEPSEEK_BASE_URL` | https://api.deepseek.com | 兼容接口地址 |
| `DEEPSEEK_MODEL` | deepseek-chat | 模型名 |
| `PROMPTS_DIR` | 空 | Prompt 目录（空用 `app/prompts`） |

### RAG / Embedding

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `EMBEDDING_PROVIDER` | hashing | hashing / bge-m3 |
| `EMBEDDING_MODEL_NAME` | BAAI/bge-m3 | Docker 内用 `/models/bge-m3` |
| `EMBEDDING_DIM` | 256 | 仅 hashing 使用 |
| `CHUNK_SIZE` | 500 | 切片大小 |
| `CHUNK_OVERLAP` | 50 | 切片重叠 |
| `VECTORSTORE_PATH` | 空 | 向量库持久化路径（空=纯内存） |
| `RAG_DOCS_DIR` | docs | 启动入库文档目录 |
| `RAG_TOP_K` | 3 | 检索返回条数 |
| `RAG_MIN_SCORE` | 0.5 | 命中最低相似度阈值 |

### CODE_REVIEW 代码 RAG

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `CODE_REVIEW_DIR` | 空 | 代码入库目录（空=不启用） |
| `CODE_VECTORSTORE_PATH` | 空 | 代码向量库持久化路径 |
| `CODE_REVIEW_QUERY` | 分析代码中的安全问题… | 检索查询 |

### 回调（Spring Boot）

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `CALLBACK_URL` | 空 | 结果回调地址（空=不回调） |
| `CALLBACK_TIMEOUT` | 10.0 | 回调超时（秒） |
| `CALLBACK_RETRIES` | 3 | 失败重试次数 |
| `CALLBACK_RETRY_DELAY` | 1.0 | 重试间隔（秒） |
