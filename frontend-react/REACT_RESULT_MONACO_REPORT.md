# React AI Result + Monaco 模块开发报告

> 项目：CodeMind-AI / frontend-react
> 模块：AI Result 结果展示 + Monaco 代码查看器
> 约束遵守：未修改 Spring Boot / FastAPI / AI 结果数据结构 / 已有模块（仅 Task 页「查看结果」跳转参数按需求调整）

---

## A. 新增文件

| 类别 | 文件 | 说明 |
|------|------|------|
| 页面 | `src/pages/Result/index.tsx` | AI 审查结果展示页（任务信息 + 代码查看 + 问题列表） |
| 组件 | `src/components/CodeViewer/index.tsx` | Monaco 只读代码查看器 |
| API | `src/api/result.ts` | `getReviewResult()` / `listReviewResults()` |
| 类型 | `src/types/result.ts` | `Issue` / `ReviewDetail` 等 detail JSON 结构 + 解析工具 |

改动文件：

| 文件 | 改动 |
|------|------|
| `src/pages/Task/index.tsx` | 「查看结果」跳转由 `/result?id=` 改为 `/result?taskId=` |
| `src/types/index.ts` | 增加 `export * from './result'` |
| `package.json` | 新增依赖 `@monaco-editor/react`、`monaco-editor` |

---

## B. API 接口映射

全部接口阅读 `backend-java` Controller / DTO / VO 确认，未猜接口。

| 前端函数 | 后端映射 | HTTP | Request | Response |
|----------|----------|------|---------|----------|
| `getReviewResult(id)` | `AiReviewResultController.detail` | GET `/api/v1/ai-reviews/{id}` | — | `Result<ReviewResultVO>` |
| `listReviewResults(params)` | `AiReviewResultController.page` | GET `/api/v1/ai-reviews` | `ReviewResultQueryRequest`（pageNum/pageSize/projectId/taskId/reviewType/severity） | `Result<PageResult<ReviewResultVO>>` |

### 需求 vs 真实接口（重要说明）

需求清单要求 `getResult(taskId)` / `getReviewResult(taskId)` / `listIssues(taskId)`，但真实后端**没有**这三类端点：

- **无按 taskId 直查结果的端点**：仅 `GET /ai-reviews/{id}`（按结果 ID）与 `GET /ai-reviews`（分页，支持 `taskId` 过滤）。前端以 `listReviewResults({ taskId, pageSize:1 })` 取首条实现「按任务取结果」。
- **无 `listIssues` 端点**：问题列表并非独立接口，而是内嵌在 `ReviewResultVO.detail`（JSON 字符串）中，前端解析 `detail` 得到 `review.issues[]`。
- 约束「禁止修改 AI 结果数据结构」，故前端按真实 `detail` 结构渲染。

### detail JSON 真实结构（已实测确认）

`ReviewResultVO.detail` 为 FastAPI `WorkflowResult` 序列化字符串：

```json
{
  "task_id": 123,
  "status": "completed",
  "plan": { "steps": [{"id":1,"description":"..."}] },
  "step_results": [{"step":{"id":1,"description":"..."},"output":"..."}],
  "review": {
    "approved": true,
    "summary": "...",
    "issues": [
      {"file":"UserService.java","line":"2","level":"P1","problem":"...","suggestion":"..."}
    ]
  },
  "projectId": "..."
}
```

实测：`detail` 顶层键 `['task_id','status','plan','step_results','review','projectId']`；`review.issues[]` 每项 `file/line/level/problem/suggestion`，`level` 取值 P0/P1/P2，`line` 为**字符串**（如 "2"/"7"）。

---

## C. Monaco 集成方式

- 依赖：`@monaco-editor/react@4.7.0` + `monaco-editor@0.56.0`。
- **自托管（不依赖 CDN）**：`loader.config({ monaco })` 绑定本地 `monaco-editor` 包。
- **Worker**：`import editorWorker from 'monaco-editor/editor/editor.worker?worker'`，`self.MonacoEnvironment = { getWorker: () => new editorWorker() }` 提供基础 worker。
  - 注意：`monaco-editor@0.56` 的 `package.json` exports 映射为 `"./*": "./esm/vs/*.js"`，故 import 路径须写 `monaco-editor/editor/editor.worker`（而非 `monaco-editor/esm/vs/editor/editor.worker`），否则 Rollup 解析失败。
- **只读**：`readOnly: true` + `domReadOnly: true`，禁在线编辑。
- **高亮/行号/滚动/高度**：`lineNumbers: 'on'`、`automaticLayout: true`、`minimap` 关闭、`scrollBeyondLastLine: false`，`height` 由父级传入适配布局。
- **语言自动识别**：`detectLanguage(fileName)` 按扩展名映射（java/py/js/ts/go/c/cpp/cs/kotlin/rust/sql/…），未知回退 `plaintext`。

---

## D. Result 展示逻辑

路由 `/result`（受 `RequireAuth` 保护），支持 `?taskId=` 与 `?id=`（结果 ID，向后兼容）。

页面结构：

1. **顶部任务信息卡**：任务 ID / 项目名称（`listProjects` 映射）/ 状态 Tag（0 等待/1 处理中/2 成功/3 失败）/ 执行时间；有 `review.summary` 时展示评审结论 Alert。
2. **主体左右布局**：左侧「代码」+ Monaco CodeViewer；右侧「AI 问题」Collapse 列表。
3. **问题展示**：`level` Tag（P0 红 / P1 橙 / P2 金）、`file:line`（code 样式）、`problem`（问题）、`suggestion`（建议）、「定位到代码」按钮。

### 状态处理

| 状态 | 展示 |
|------|------|
| 加载中 | `Spin` |
| 缺少 taskId/id 参数 | `Result` warning「缺少参数」 |
| 接口错误 | `Result` error「加载失败」 |
| 任务 FAILED（status=3） | `Result` error + `task.errorMsg` |
| 无结果记录 | `Result` info「暂无结果」 |
| 有结果 | 完整左右布局 |

数据流：`?taskId=` → `listReviewResults({taskId, pageSize:1})` 取结果 + `getTask(taskId)` 取任务 → `parseReviewDetail(detail)` 解析问题列表。

---

## E. 代码定位实现

- 问题项「定位到代码」按钮：`parseLineNumber(issue.line)` 从字符串行号（"2"/"45-50"）提取首个数字，非法则禁用按钮。
- 点击后 `setActiveLine(n)`，`CodeViewer` 内部 `useEffect` 监听 `activeLine` 变化：
  ```ts
  editor.setPosition({ lineNumber: activeLine, column: 1 })
  editor.revealLineInCenter(activeLine)
  ```
- 组件卸载由 React 管理，无手动 listener 泄漏。

> 当前后端未提供文件内容接口，CodeViewer 处于空内容占位态，定位机制已就绪，待代码可加载后即可联动。

---

## F. 测试结果

**构建**：`tsc && vite build` 通过，TypeScript 零错误（`✓ built in 39.22s`）。全仓新增代码无 `any`。

**真实后端数据校验**（curl + 显式 UTF-8 解析）：

| 检查 | 结果 |
|------|------|
| `GET /ai-reviews?taskId=…&pageSize=1` | ✅ `code=200`，`total=1`，命中 `reviewType=CODE_REVIEW`、`status=1` |
| `GET /ai-reviews/{id}` | ✅ 返回 `ReviewResultVO`，`severity/lineNo/summary` 为 null（回调创建结果仅填 detail） |
| `detail` 结构 | ✅ 顶层键 `task_id/status/plan/step_results/review/projectId`，与 `ReviewDetail` 类型一致 |
| `review.issues[]` | ✅ 2 条，`file=UserService.java`、`line=2/7`、`level=P1`、`problem/suggestion` 齐全 |

结论：Result 页数据解析与真实 `detail` 结构完全吻合，问题列表渲染字段正确。真实链路（上传 → CODE_REVIEW → SUCCESS → 查看结果）中，SUCCESS 仅对已预置代码目录的项目成立（见 G），Result 页已用真实 SUCCESS 结果验证。

**未跑浏览器 E2E**：Playwright 缺 Chrome channel（同上阶段），以 API 冒烟 + 构建代替。

---

## G. 遗留问题（分级）

### P1（后端/部署侧，非前端）

**G-1 后端无文件内容读取接口。**
`FileController` 仅 `upload/page/detail/delete`，`FileVO` 无 `content`；`CodeFile` 实体虽有 `content` 字段但无 Controller 暴露；`StorageService.load(key)` 仅内部使用。故 Monaco 无法加载代码原文，左侧代码区当前为空占位。
- 影响：需求「看到代码 → 看到 AI 分析结果」中的「看到代码」无法实现。
- 修复方向（后端）：新增文件内容/预览接口（如 `GET /api/v1/files/{id}/content`），前端即可接入 CodeViewer。
- 前端已就绪：CodeViewer 组件 + 语言识别 + 定位联动均已实现，仅差内容数据源。

### P2（优化）

- **G-2 Monaco 体积大**：主 chunk 增至 5.25MB（gzip 1.44MB）。建议 `manualChunks` 拆 monaco，或 `/result` 路由懒加载（`React.lazy` + `dynamic import`）。
- **G-3 需求字段与实际数据不符**：需求示例「HIGH / SQL Injection / lineNumber」与真实数据「level=P0|P1|P2 / file / line(字符串) / problem / suggestion」不一致，且**无「问题类型」分类字段**。前端按真实数据渲染，未臆造字段。
- **G-4 需求 API 命名与实际不符**：`getResult(taskId)`/`listIssues(taskId)` 无对应端点，已用 `listReviewResults(taskId 过滤)` + `detail` 客户端解析等价实现（见 B）。
- **G-5 浏览器 E2E 未跑**：`npx playwright install chrome` 后可补 UI 验收。

---

## 附：安全自查

- 无硬编码密钥/token/密码；接口统一走 `request.ts` 拦截器携带 `Bearer token`。
- Monaco 自托管本地包，无 CDN 外链依赖，无运行时外部请求。
- 未引入任何 `dangerouslySetInnerHTML` 或 eval；`detail` 仅经 `JSON.parse` 解析后按字段渲染文本，无 XSS 注入面。
