# React File + AI Task 模块验收报告

> 项目：CodeMind-AI / frontend-react
> 验收范围：File 代码文件管理模块 + AI Task 任务管理模块
> 验收方式：只检查、测试、出报告，未修改任何代码
> 结论：前端实现完整、接口与后端一致、构建通过。真实 AI 链路走到 `FAILED`，根因是后端/部署集成缺口（非前端缺陷）。

---

## A. File 模块完成情况

**工程结构**：页面层 / API 层 / 类型层职责分离，均存在。

| 层 | 文件 | 状态 |
|----|------|------|
| 页面 | `src/pages/File/index.tsx` | ✅ 完成 |
| API | `src/api/file.ts` | ✅ 完成 |
| 类型 | `src/types/file.ts` | ✅ 完成 |

**功能点核对**：

| 检查项 | 结果 | 说明 |
|--------|------|------|
| 上传成功 | ✅ | `Upload.Dragger` + 手动提交，`onUploadProgress` 刷新进度条，成功 `message.success` + 刷新列表 |
| 上传失败提示 | ✅ | `handleRequestError` 分类提示（网络异常/403/5xx/业务 message） |
| 文件列表刷新 | ✅ | 上传/删除成功后 `fetchList()` 重新拉取 |
| 删除文件 | ✅ | `Popconfirm` 确认 → `deleteFile` → 刷新 |
| 权限控制 | ✅ | 路由 `/file` 受 `RequireAuth` 保护；请求走拦截器自动带 `Bearer token`；后端 `@PreAuthorize("isAuthenticated()")` 二次校验 |

---

## B. Task 模块完成情况

**工程结构**：

| 层 | 文件 | 状态 |
|----|------|------|
| 页面 | `src/pages/Task/index.tsx` | ✅ 完成 |
| API | `src/api/task.ts` | ✅ 完成 |
| 类型 | `src/types/task.ts` | ✅ 完成 |
| 类型（结果） | `src/types/review.ts` | ✅ 完成 |

**功能点核对**：

| 检查项 | 结果 | 说明 |
|--------|------|------|
| 创建任务 | ✅ | 项目 + 任务类型（默认 `CODE_REVIEW`）+ 任务内容，调 `createTask` |
| 任务列表 | ✅ | 任务 ID / 项目名称 / 任务类型 / 状态 / 创建时间 / 完成时间 |
| 状态 Tag 颜色 | ✅ | 0 default / 1 processing / 2 success / 3 error |
| 失败原因展示 | ✅ | `errorMsg` 挂 `Tooltip` + 详情弹窗「失败原因」项 |
| 查看结果入口 | ✅ | `status===2 && resultId` 时显示「查看结果」→ `/result?id=` |

**关于「选择代码文件」步骤（五）**：后端 `TaskCreateRequest` 实际**无 `fileId` 字段**，任务按「项目」维度创建，FastAPI 读取项目代码目录整体评审。前端据此省略「选择代码文件」，遵循真实接口协议，未猜接口。

---

## C. API 一致性

逐条比对 `backend-java` Controller / DTO / VO 源码与前端 `api/*.ts`，全部一致。

### File

| 前端函数 | 后端映射 | HTTP | Request | Response |
|----------|----------|------|---------|----------|
| `uploadFile()` | `FileController.upload` | POST `/api/v1/files/upload` | multipart `file` + `projectId(Long)` | `Result<FileVO>` ✅ |
| `listFiles()` | `FileController.page` | GET `/api/v1/files` | `FileQueryRequest` | `Result<PageResult<FileVO>>` ✅ |
| `getFile()` | `FileController.detail` | GET `/api/v1/files/{id}` | — | `Result<FileVO>` ✅ |
| `deleteFile()` | `FileController.delete` | DELETE `/api/v1/files/{id}` | — | `Result<Void>` ✅ |

### AI Task

| 前端函数 | 后端映射 | HTTP | Request | Response |
|----------|----------|------|---------|----------|
| `createTask()` | `AiTaskController.create` | POST `/api/v1/ai-tasks` | `TaskCreateRequest` | `Result<TaskVO>` ✅ |
| `listTasks()` | `AiTaskController.page` | GET `/api/v1/ai-tasks` | `TaskQueryRequest` | `Result<PageResult<TaskVO>>` ✅ |
| `getTask()` | `AiTaskController.detail` | GET `/api/v1/ai-tasks/{id}` | — | `Result<TaskVO>` ✅ |
| `getTaskResult()` | `AiReviewResultController.detail` | GET `/api/v1/ai-reviews/{id}` | — | `Result<ReviewResultVO>` ✅ |

### DTO 字段映射

- `TaskCreateRequest`：`projectId(Long,NotNull)` / `taskType(String,NotBlank)` / `params` / `content` —— 前端 `CreateTaskRequest` 四字段完全一致，无 `fileId`。
- `TaskQueryRequest`：`pageNum/pageSize/projectId/taskType/status` —— 前端 `TaskQuery` 一致。
- `FileQueryRequest`：`pageNum/pageSize/projectId/fileName` —— 前端 `FileQuery` 一致。
- `TaskVO` / `FileVO` / `ReviewResultVO` 字段与前端 `Task` / `FileItem` / `ReviewResult` 一一对应。
- Long 全局序列化为字符串：`id/projectId/fileSize/total/timestamp` 等前端均声明 `string`，`status/pageNum/pageSize/lineNo` 声明 `number`。✅

---

## D. 真实 AI 链路结果

curl 打真实后端，完整流程（登录 → 建项目 → 上传代码 → 建任务 → 等待 FastAPI → 回调）：

| 步骤 | 结果 |
|------|------|
| 登录 | ✅ token 获取成功（212 字符） |
| 创建项目 | ✅ `projectId=2092490585960202242` |
| 上传 `AccCheck.java` | ✅ `code=200`，`fileId=2092490586253803522` |
| 文件列表筛选 | ✅ `total=1`，`AccCheck.java` |
| 创建 CODE_REVIEW 任务 | ✅ `status=1 处理中`（FastAPI 立即受理） |
| 轮询 | ⚠️ t=3s `status=3 失败`，`resultId=None` |
| 失败原因 | `{"projectId":...,"error":"无可用代码上下文：代码目录为空或检索无命中，已终止评审"}` |

**结论**：前端全链路调用正确，任务状态流 `PROCESSING → FAILED` 由 FastAPI 回调真实写入，React 侧会正确展示「失败」Tag + 失败原因。未走到 `SUCCESS` 的根因是**后端集成缺口**（见 G-1），非前端缺陷。

---

## E. 状态轮询结果

`src/pages/Task/index.tsx` 自调度 `setTimeout` 链实现：

- ✅ `WAITING/PROCESSING` 存在时每 3s 自动 `fetchList()`。
- ✅ 间隔合理（3s）。
- ✅ 全部 `SUCCESS/FAILED`（或无进行中任务）时不再调度，停止轮询。
- ✅ 组件卸载 cleanup 置 `cancelled` + `clearTimeout`，不泄漏 timer。
- ✅ 无进行中任务时零请求，禁止无限轮询。

---

## F. 构建结果

```
> tsc && vite build
✓ 3120 modules transformed.
✓ built in 9.56s
```

- TypeScript 零错误。
- 全仓 `grep '\bany\b'` 无匹配 —— 无 `any` 滥用。
- 唯一警告：主 chunk 1.17MB（gzip 374KB）超 500KB，属 P2 优化项（可 `manualChunks` 拆包）。

---

## G. 遗留问题（分级）

### P1（必须修复，后端/部署侧，非前端）

**G-1 上传文件与 FastAPI 代码上下文目录脱节。**
`FileController` 上传的文件进入 Spring Boot 自身存储（`LocalStorageServiceImpl`），与 FastAPI 的 `CODE_REVIEW` 上下文目录（`Settings.code_review_dir`，见 `AI Services/app/config/config.py` 与 `app/services/task_service.py::_build_code_context`）是两个独立路径，无共享挂载。FastAPI 建 CODE_REVIEW 任务时只读 `code_review_dir`，找不到任何代码，`_execute` 抛 `无可用代码上下文…`，任务必 `FAILED`。
- 影响：前端「上传文件 → 立即评审」路径无法走到 `SUCCESS`。
- 修复方向（后端/部署）：把上传文件同步/挂载到 `code_review_dir`，或让 FastAPI 按 `projectId` 读取 Spring Boot 文件存储。
- 前端已正确：如实展示 `FAILED` 状态与 `errorMsg`，无需改动。

### P2（优化，非阻断）

- **G-2 构建 chunk 超 500KB**：`manualChunks` 拆 antd/echarts 等可减小首屏体积。
- **G-3 浏览器 E2E 未跑**：Playwright 缺 Chrome channel（`Chromium distribution 'chrome' is not found`），当前以 API 冒烟 + `npm run build` 代替，后续 `npx playwright install chrome` 可补 UI 级验收。
- **G-4 需求「选择代码文件」与后端协议不符**：后端 `TaskCreateRequest` 无 `fileId`，任务按项目维度创建。前端已按真实接口省略文件选择；若产品确需「文件级评审」，属后端协议变更（超出前端职责）。
- **G-5 Result 页渲染**：`/result?id=` 已路由，「查看结果」跳转可用；Result 页数据渲染属后续模块范围。

---

## 附：安全自查

- 无硬编码密钥/token/密码；token 经 `request.ts` 拦截器统一注入 `Authorization: Bearer <token>`。
- 上传白名单（扩展名）+ 10MB 前端软限制仅为体验层，权威校验由后端 `FileService` 负责，未绕过后端安全校验。
- 401 → 统一登出跳转登录；403/5xx/网络异常 → 分类 `message` 提示。
