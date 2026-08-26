# 接口设计

统一响应结构 `Result<T>`：`{code, message, data, timestamp}`；分页结构 `PageResult<T>`：`{list, total, pageNum, pageSize}`。

鉴权方式：

- **用户接口**：JWT（`Authorization: Bearer <token>`）。
- **内部接口**：HMAC-SHA256 签名（`X-Timestamp` + `X-Signature`），供 AI 服务 / 内部调用。

## 1. 认证接口

前缀 `/api/v1/auth`。

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/register` | 公开 | 注册，默认绑定 USER 角色 |
| POST | `/login` | 公开 | 登录，返回 access + refresh token |
| POST | `/refresh` | 公开 | 刷新令牌 |
| POST | `/logout` | JWT | 登出，撤销刷新令牌 + access 黑名单 |
| GET | `/me` | JWT | 当前用户信息（含角色） |

## 2. 项目接口

前缀 `/api/v1/projects`，均需 JWT 认证。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/` | 创建项目 |
| GET | `/` | 分页查询项目 |
| GET | `/{id}` | 项目详情 |
| PUT | `/{id}` | 修改项目 |
| DELETE | `/{id}` | 删除项目（逻辑删除） |

## 3. 代码文件接口

前缀 `/api/v1/files`，均需 JWT 认证。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/upload` | 文件上传（multipart，含 projectId） |
| GET | `/` | 分页查询文件 |
| GET | `/{id}` | 文件详情 |
| GET | `/{id}/content` | 文件内容 / 下载 |
| DELETE | `/{id}` | 删除文件（逻辑删除） |

## 4. AI 任务接口

前缀 `/api/v1/ai-tasks`。

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/` | JWT | 创建 AI 任务 |
| GET | `/` | JWT | 分页查询任务 |
| GET | `/{id}` | JWT | 任务详情 |
| GET | `/pending` | HMAC | 拉取待处理任务 |
| PUT | `/{id}/status` | HMAC | 更新任务状态 |
| PUT | `/{id}/result` | HMAC | 保存任务结果 |

任务状态机：`WAITING(0) → PROCESSING(1) → SUCCESS(2) / FAILED(3)`。

## 5. AI 审查结果接口

前缀 `/api/v1/ai-reviews`。

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/` | HMAC | 保存审查结果 |
| GET | `/` | JWT | 分页查询结果 |
| GET | `/{id}` | JWT | 结果详情 |

## 6. AI 结果回调接口

前缀 `/api/ai/task`。

| 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|
| POST | `/callback` | HMAC | FastAPI 完成 AI 任务后回调，更新任务状态并保存结果 |

回调载荷：`{taskId, status, result}`，status 为 `SUCCESS` / `FAILED`。

## 7. 用户管理接口（仅管理员）

前缀 `/api/v1/users`，`@PreAuthorize("hasRole('ADMIN')")`。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 分页查询用户 |
| GET | `/{id}` | 用户详情 |
| POST | `/` | 新建用户 |
| PUT | `/{id}` | 修改用户 |
| DELETE | `/{id}` | 删除用户（逻辑删除） |
| PUT | `/{id}/roles` | 分配角色 |

## 8. 角色接口（仅管理员）

前缀 `/api/v1/roles`。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 角色列表 |

## 9. AI Service 接口

前缀 `/api`（FastAPI）。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/tasks` | 创建 AI 任务，返回 `{taskId, status, message}` |
| GET | `/health` | 健康检查，返回 `{status: ok}` |

创建任务请求载荷：`{taskId, taskType, projectId, content}`。

## 10. 权限与数据隔离

- 公开接口：注册、登录、刷新令牌、内部回调（HMAC 鉴权）。
- 其余接口需 JWT 认证。
- 用户 / 角色管理接口额外要求 ADMIN 角色。
- 数据隔离：普通用户查询任务 / 文件时，仅能看到本人负责项目下的数据；管理员可见全部。
