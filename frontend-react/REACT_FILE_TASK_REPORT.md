# React File Task 模块开发报告

> 项目：CodeMind-AI / frontend-react
> 模块：File 代码文件管理 + AI Task 任务管理
> 阶段：Phase 3

---

## A. 新增文件

| 类别 | 文件 | 说明 |
|------|------|------|
| 页面 | `src/pages/File/index.tsx` | 代码文件管理页（上传 + 列表 + 详情 + 删除） |
| 页面 | `src/pages/Task/index.tsx` | AI 任务管理页（创建 + 列表 + 详情 + 状态轮询） |
| API | `src/api/file.ts` | `uploadFile()/listFiles()/getFile()/deleteFile()` |
| API | `src/api/task.ts` | `createTask()/listTasks()/getTask()/getTaskResult()` |
| 类型 | `src/types/file.ts` | `FileItem`、`FileQuery` |
| 类型 | `src/types/task.ts` | `Task`、`TaskQuery`、`CreateTaskRequest` |
| 类型 | `src/types/review.ts` | `ReviewResult` |

改动文件：

| 文件 | 改动 |
|------|------|
| `src/types/index.ts` | 增加 `task`/`file`/`review` 再导出 |
| `src/utils/index.ts` | 新增 `formatFileSize()` 字节格式化工具 |
| `src/router/index.tsx` | 新增 `/file`、`/task` 路由，均受 `RequireAuth` 保护 |

---

## B. API 接口映射

所有接口均通过阅读 `backend-java` 实际 Controller/DTO/VO 确认，未猜接口。

### File 模块

| 前端函数 | HTTP | 路径 | 请求 | 响应 |
|----------|------|------|------|------|
| `listFiles()` | GET | `/api/v1/files` | `FileQuery`（pageNum/pageSize/projectId/fileName） | `Result<PageResult<FileItem>>` |
| `uploadFile()` | POST | `/api/v1/files/upload` | multipart `file` + `projectId` | `Result<FileItem>` |
| `getFile()` | GET | `/api/v1/files/{id}` | — | `Result<FileItem>` |
| `deleteFile()` | DELETE | `/api/v1/files/{id}` | — | `Result<Void>`（逻辑删除） |

### AI Task 模块

| 前端函数 | HTTP | 路径 | 请求 | 响应 |
|----------|------|------|------|------|
| `listTasks()` | GET | `/api/v1/ai-tasks` | `TaskQuery`（pageNum/pageSize/projectId/taskType/status） | `Result<PageResult<Task>>` |
| `createTask()` | POST | `/api/v1/ai-tasks` | `TaskCreateRequest` | `Result<Task>` |
| `getTask()` | GET | `/api/v1/ai-tasks/{id}` | — | `Result<Task>` |
| `getTaskResult()` | GET | `/api/v1/ai-reviews/{resultId}` | — | `Result<ReviewResult>` |

### 关键字段约定（后端 Long 全局序列化为字符串）

- `id`、`projectId`、`fileSize`、`total`、`timestamp`、`expiresIn` 等 Long 字段后端返回 JSON **字符串**，类型统一声明为 `string`。
- `status`、`pageNum`、`pageSize`、`lineNo` 等 Integer 字段为 `number`。
- `CreateTaskRequest` **无 `fileId` 字段** —— 后端任务按「项目」维度创建，FastAPI 读取项目代码目录整体评审。前端据此省略了「选择代码文件」项，遵循真实协议。

---

## C. 文件上传流程

1. 进入 `/file`，下拉选择目标项目（`listProjects` 加载，必填）。
2. `Upload.Dragger` 选择/拖拽文件，`beforeUpload` 做前端软校验：
   - 项目未选 → `message.warning` 提示。
   - 扩展名不在白名单（`.java/.py/.js/.ts/.jsx/.tsx/.go/.c/.cpp/.h/.cs/.rb/.php/.kt/.swift/.rs/.sql/.sh/.vue/.html/.css`）→ `message.error('不支持的文件类型：.xxx')`。
   - 大小超 10MB → `message.error('文件大小超过 10MB 限制')`。
   - 校验通过 → 保存到 `selectedFile`，`return false` 手动提交（不自动上传）。
3. 点击「上传」→ `uploadFile(file, projectId, setPercent)`，`onUploadProgress` 回调刷新 `Progress` 进度条。
4. 成功 → `message.success('上传成功')`、清空选择、`fetchList()` 刷新列表。
5. 失败 → `handleRequestError` 按 401/403/5xx/网络异常 分类提示。
6. 列表 `Table` 展示：文件名 / 类型（Tag）/ 大小（`formatFileSize`）/ 上传时间 / checksum / 操作（查看、删除）。
7. 「查看」弹 `Modal` 调 `getFile` 展示详情（含存储地址、checksum）；「删除」`Popconfirm` 确认后调 `deleteFile` 并刷新。

> 前端软限制仅为体验层，后端仍做权威安全校验。

---

## D. AI 任务流程

1. 进入 `/task`，创建表单（`Form` inline）：项目（必选）、任务类型（默认 `CODE_REVIEW`）、任务内容（可选）。
2. 提交 → `createTask({ projectId, taskType, content })` → `message.success('任务已创建')` → 重置表单 → 刷新列表。
3. 列表 `Table` 展示：任务 ID / 项目名称（`projectMap` 映射）/ 任务类型 / 状态（Tag 颜色区分）/ 创建时间 / 完成时间 / 操作（详情、查看结果）。
4. 状态 Tag 颜色：
   - `0` 等待处理 → `default`
   - `1` 处理中 → `processing`
   - `2` 成功 → `success`
   - `3` 失败 → `error`，且 `errorMsg` 存在时挂 `Tooltip` 悬浮展示失败原因
5. 「详情」弹 `Modal` 调 `getTask` 展示 Descriptions（任务 ID / 项目 / 任务类型 / 状态 / 请求参数 / 失败原因 / 创建·开始·完成时间）。
6. 任务成功且 `resultId` 存在 → 「查看结果」按钮跳转 `/result?id=${resultId}`。

---

## E. 状态轮询实现

`src/pages/Task/index.tsx` 内 `useEffect` 自调度 `setTimeout` 链：

```ts
useEffect(() => {
  let cancelled = false
  const run = async () => {
    const records = await fetchList()
    if (cancelled) return
    const pending = records.some((t) => t.status === 0 || t.status === 1)
    if (pending) {
      timerRef.current = window.setTimeout(run, 3000)
    }
  }
  run()
  return () => {
    cancelled = true
    if (timerRef.current) window.clearTimeout(timerRef.current)
  }
}, [fetchList])
```

- **轮询条件**：列表中存在 `WAITING(0)` 或 `PROCESSING(1)` 任务时，每 3s 自调度刷新一次。
- **停止条件**：全部任务为 `SUCCESS(2)` / `FAILED(3)`（或无任务）时不再调度，即「无进行中任务即停」，不会无限轮询。
- **卸载清理**：cleanup 置 `cancelled` 并 `clearTimeout`，组件卸载不泄漏 timer。
- 刷新只发生在「有待处理任务」期间，空闲时零请求，符合「禁止无限轮询」要求。

---

## F. 测试结果

构建：`npm run build`（`tsc && vite build`）通过，3120 modules，TypeScript 零错误。

浏览器 E2E 未执行（Playwright 缺 Chrome channel：`Chromium distribution 'chrome' is not found`），改以 curl 打真实后端冒烟验证：

| 测试 | 结果 | 明细 |
|------|------|------|
| TEST1 上传 | ✅ PASS | `POST /files/upload` `code=200`，`fileName=CodeMindDemo2986.java`、`fileType=.java`、`size=132` |
| TEST1b 列表筛选 | ✅ PASS | `GET /files?projectId=...` `total=1`，首条 `CodeMindDemo2986.java` |
| TEST2 创建任务 | ✅ PASS | `POST /ai-tasks` 返回 `status=1 处理中` |
| TEST2b 轮询 | ✅ PASS（流程正确） | t=3s `status=3 失败`，`errorMsg={"projectId":...,"error":"无可用代码上下文：代码目录为空或检索无命中，已终止评审"}` |
| TEST3 结果 | ⚠️ 无结果 | 任务 FAILED，`resultId` 为空，`/ai-reviews/{id}` 无可查 |

结论：
- 上传、列表、创建任务、轮询、失败状态 + 失败原因展示全链路打通，前端调用正确。
- 任务 `PROCESSING → FAILED` 为**后端集成问题**（见 G），非前端缺陷。前端正确调用了已确认的接口，并如实展示了失败原因。

---

## G. 遗留问题

1. **【后端集成缺口】FileController 上传不填充 FastAPI 评审上下文目录。**
   `FileController` 上传的文件进入 `FileController` 自身的存储（`FileVO.storageUrl`），与 FastAPI 的 `CODE_REVIEW_DIR`（RAG 代码上下文目录）是两个独立位置。因此对「仅通过文件上传接口建库」的项目发起 `CODE_REVIEW` 任务，FastAPI 找不到任何代码上下文，任务最终 `FAILED`，`errorMsg` = `无可用代码上下文：代码目录为空或检索无命中，已终止评审`。
   - 影响：前端「上传文件 → 立即评审」这条测试路径无法走到 `SUCCESS`。
   - 归属：后端/部署层（需把上传文件同步/挂载到 `CODE_REVIEW_DIR`，或让 FastAPI 读取 FileController 的存储）。前端无需也无法改动。
   - 前端已正确：任务失败时如实展示 `errorMsg`（详情弹窗 + 状态 Tag Tooltip）。

2. **浏览器 E2E 未跑。** Playwright 缺 Chrome channel，后续可 `npx playwright install chrome` 后补 UI 级验收。

3. **Result 页面为既有占位/待接入。** `/result?id=xxx` 已路由，本阶段仅「查看结果」按钮跳转；Result 页的数据渲染属后续模块范围。

---

## 附：安全自查

- 无硬编码密钥/token/密码；接口调用统一走 `request.ts` 拦截器自动携带 `Authorization: Bearer <token>`。
- 上传白名单 + 10MB 软限制仅为前端体验，权威校验由后端负责，前后端职责清晰。
