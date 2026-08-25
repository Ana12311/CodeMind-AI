# AI 执行流程

描述从任务创建到结果回调的完整 AI 执行链路。

## 1. 总览

```
任务创建（Java）
    ↓ HTTP POST /api/tasks
FastAPI 接收，立即返回 PROCESSING
    ↓ 后台线程池异步执行
Agent 工作流（Planner → Worker → Reviewer）
    ↓ CODE_REVIEW 任务注入代码上下文
Retriever 检索
    ↓
Embedding（hashing / bge-m3）
    ↓
Vector Store 相似度检索
    ↓
LLM 分析
    ↓ HTTP 回调
Callback 回调 Java
    ↓
Java 保存结果
```

## 2. 任务创建

- 入口：`POST /api/tasks`（`app/api/task.py` → `TaskService.create_task`）。
- 请求载荷：`{taskId, taskType, projectId, content}`。
- `TaskService.create_task`：
  1. 记录状态 PROCESSING。
  2. 提交到 `ThreadPoolExecutor(max_workers=4)` 后台执行。
  3. 立即返回 `{taskId, status: "PROCESSING", message: "任务已受理"}`。

Java 侧 `AiTaskServiceImpl.createTask` 落库后调用 `AiServiceClient.submitTask`，超时 10 秒，仅取状态确认；失败则任务置 FAILED。

## 3. Agent 工作流

`AgentWorkflow.run` 为固定三步，线性、无自主循环：

```
Planner.plan(task, task_type)          → Plan（有序子步骤）
    ↓
Worker.execute(step, task) × N         → StepResult 列表
    ↓
Reviewer.review(task, results, context) → ReviewResult（approved / summary / issues）
    ↓
status = completed | rejected
```

### Planner

- 将任务拆解为 3-5 个有序子步骤。
- Prompt：`app/prompts/planner.yaml`，输出 JSON `{"steps": [{id, description}]}`。
- 解析容错：JSON 解析失败回退到按行 / 编号解析；仍失败则退化为单步。

### Worker

- 逐个执行子步骤。
- 优先匹配 `ToolRegistry` 注册的工具（当前默认无工具注册，走 LLM 路径）。
- LLM 路径：先通过 RAG 检索上下文，再调用 LLM 生成执行结果。
- Prompt：`app/prompts/worker.yaml`，注入 `{task, step, context}`。

### Reviewer

- 审核所有执行结果，产出结构化审查报告。
- Prompt：`app/prompts/reviewer.yaml`，注入代码上下文，要求只基于真实检索到的代码片段分析，禁止编造问题。
- 输出：`{approved, summary, issues: [{file, line, level, problem, suggestion}]}`，级别 P0 / P1 / P2。
- `approved` 表示审查流程是否完整产出结论，与代码是否存在问题无关。

## 4. Retriever（检索）

`Retriever.search(query, top_k)`：

1. 查询文本 → Embedding（`embed_query`）。
2. Vector Store 余弦相似度检索，返回 top_k 命中。
3. 过滤 `score < min_score` 的命中。

Worker 检索的 query 为 `"{task}\n{step}"`；检索失败降级为空上下文，不中断任务。

## 5. Embedding

提供方（`app/embeddings/embedding.py`）：

- **HashingEmbeddingProvider**（默认）：特征哈希，词袋 + 符号哈希 + L2 归一化，离线可用。
- **SentenceTransformerEmbeddingProvider**：bge-m3 语义模型，支持中文 ↔ 英文代码跨语言检索。

按 provider + 模型 + 维度单例缓存，避免重复加载模型。

## 6. Vector Store

`app/vectorstore/vectorstore.py`：

- 进程内向量存储，余弦相似度检索。
- 支持 JSON 文件持久化（`save` / `load`），非外部向量数据库服务。
- 未配置持久化路径时纯内存。

## 7. LLM

`LLMService` 为模型调用统一入口，上层只能通过它调用模型。

提供方（`app/services/llm_service/providers.py`）：

- **MockProvider**：占位提供方，不调用真实模型，回显 Prompt，用于本地联调。
- **OpenAICompatibleProvider**：OpenAI 兼容接口，覆盖 DeepSeek、通义千问、Moonshot、Ollama 等。

Prompt 模板从 `app/prompts` 目录按名称加载（`.yaml` / `.txt` / `.md`）。

## 8. 代码上下文（CODE_REVIEW）

当 `taskType == CODE_REVIEW` 时，`TaskService._build_code_context` 通过 `CodeReviewContextBuilder` 构建代码上下文：

1. `CodeLoader` 读取项目代码目录（支持 .java / .py / .js / .ts 等，跳过构建目录）。
2. `CodeSplitter` 按 class / method 结构切分代码块，携带文件名、类名、方法名、行号元数据。
3. Embedding 向量化后入库（可持久化）。
4. 按配置的 `CODE_REVIEW_QUERY`（默认“分析代码中的安全问题、设计问题、性能问题”）检索命中代码块。
5. 检索结果拼成上下文文本，注入 Reviewer 与 Worker。

代码入库失败时降级为无代码上下文，任务继续执行。

## 9. Callback（回调）

`TaskService._execute` 完成工作流后：

- 状态映射：`completed → SUCCESS`，`rejected → FAILED`（审核不通过按 FAILED 处理，结论在 result.review 中）。
- 载荷：`{taskId, status, result}`，result 内含 plan / step_results / review，并追加 `projectId`。
- `CallbackService.send` 通过 httpx POST 到 `callback_url`，支持重试（默认 3 次，间隔 1 秒）。
- 未配置 `callback_url` 时跳过回调，视为成功。

Java 侧 `AiCallbackController.callback` 接收后：

- 仅处理中的任务可接收回调。
- SUCCESS：保存 `ai_review_result`，任务置 SUCCESS，关联 `result_id`。
- FAILED：任务置 FAILED，`error_msg` 记录结果。

## 10. 任务状态

Java 侧状态机（`TaskStatus`）：

```
WAITING(0) → PROCESSING(1) → SUCCESS(2) / FAILED(3)
```

合法流转：

- WAITING → PROCESSING / FAILED
- PROCESSING → SUCCESS / FAILED

非法流转会被 `AiTaskServiceImpl.updateStatus` 拒绝。
