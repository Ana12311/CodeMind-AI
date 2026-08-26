# Dashboard 与 Project 模块开发报告

范围：`CodeMind-AI/frontend-react`
约束遵守：未改 Spring Boot / API 协议 / 数据库 / 前端基础架构，仅实现页面 + API 调用 + 交互。

---

## A. 新增 / 修改文件

### 新增

| 文件 | 说明 |
| --- | --- |
| `src/types/project.ts` | `Project` / `CreateProjectRequest` / `ProjectQuery` |
| `src/types/task.ts` | `Task` / `TaskQuery` |
| `src/types/file.ts` | `FileItem` / `FileQuery` |
| `src/api/project.ts` | `listProjects` / `createProject` / `getProject` / `deleteProject` |
| `src/api/task.ts` | `listTasks` |
| `src/api/file.ts` | `listFiles` |
| `src/components/Dashboard/StatCard.tsx` | 统计卡片组件 |
| `src/components/Dashboard/RecentTaskTable.tsx` | 最近任务表格组件 |

### 修改

| 文件 | 说明 |
| --- | --- |
| `src/types/index.ts` | 增加 `PageResult<T>`，re-export project/task/file |
| `src/utils/index.ts` | 增加 `handleRequestError` 统一异常处理 |
| `src/pages/Dashboard/index.tsx` | 重写为真实统计首页 |
| `src/pages/Project/index.tsx` | 重写为完整 CRUD 页面 |

---

## B. API 映射（由真实 Controller 确认）

| 功能 | 接口 | 前端方法 |
| --- | --- | --- |
| 分页项目 | `GET /api/v1/projects` | `listProjects` |
| 创建项目 | `POST /api/v1/projects` | `createProject` |
| 项目详情 | `GET /api/v1/projects/{id}` | `getProject` |
| 删除项目 | `DELETE /api/v1/projects/{id}` | `deleteProject` |
| 分页任务 | `GET /api/v1/ai-tasks` | `listTasks` |
| 分页文件 | `GET /api/v1/files` | `listFiles` |

响应 `Result<PageResult<T>>`：`{ records, total, pageNum, pageSize }`。
关键：后端 `Long` 序列化为字符串（实测 `total:"3"`、`id` 等），前端 `total`/`id` 等字段均按 `string` 处理，展示时 `Number()` 转换。

---

## C. 页面功能

### Dashboard（统计区 + 最近记录）

- 我的项目数量：`listProjects(pageSize=1).total`
- AI 任务数量：`listTasks(pageSize=1).total`
- 成功任务数量：`listTasks(pageSize=1, status=2).total`
- 最近审查记录：`listTasks(pageSize=5).records`，`RecentTaskTable` 展示（类型/状态/时间）
- `Promise.all` 并发请求，`StatCard` 组件复用，含 Loading

### Project 项目管理

- 列表：`Table` 展示名称/描述/状态/创建时间/更新时间/操作，分页 + Loading + Empty
- 创建：`Modal + Form`（name 必填，description/language/repoUrl 可选），提交后刷新列表
- 详情：`getProject` + `listFiles` + `listTasks` 并发，展示基本信息 + 代码文件数量 + AI 任务数量
- 删除：`Popconfirm` 二次确认，逻辑删除，成功后刷新

### 异常处理（`handleRequestError`）

| 场景 | 处理 |
| --- | --- |
| 401 | 拦截器统一登出跳转（页面不再重复提示） |
| 403 | `无权限访问` |
| 5xx | `服务器内部错误`（或后端 message） |
| 网络错误 | `网络异常，请检查网络或后端服务` |
| 业务错误（code!=200） | `http` 层已提示，页面不重复提示 |

---

## D. 测试结果（真实后端冒烟）

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 登录后 Dashboard 统计 | projects=3 / tasks=3 / success=2，数据正常 ✅ |
| 2 | 项目列表加载 | `total` 正确返回，分页/name 过滤生效 ✅ |
| 3 | 创建项目 | 返回新 id，`total` 0→1，DB 新增 ✅ |
| 4 | 项目详情 | 基本信息 + 文件数/任务数正确（新项目 files=0/tasks=0）✅ |
| 5 | 删除项目 | HTTP 200，`total` 1→0，列表刷新 ✅ |
| 6 | 刷新页面 JWT 有效 | `persist` localStorage（登录模块已验证）✅ |

构建：`tsc && vite build` 零 TS 错误，3120 模块。

> 注：浏览器 E2E 未跑（本机 Playwright 缺 Chrome 通道）。以上为真实后端 API 冒烟 + 构建验证。

---

## E. 遗留问题

1. **chunk 体积**：引入 Table/Descriptions/Statistic 后主包 1.1MB（gzip 354KB），后续用 `manualChunks` 或按需引入分包。
2. **refreshToken 未用**：登录已返回 `refreshToken`，但 store 未持久化，token 过期后无静默续期（当前靠 401 跳登录）。
3. **项目详情无聚合接口**：文件数/任务数靠 `listFiles`/`listTasks` 按 projectId 计算，数据量大时建议后端加聚合（不动后端，已按现有接口实现）。
4. **删除后分页边界**：删除末页唯一记录后可能短暂空页，未做自动回退上一页。
