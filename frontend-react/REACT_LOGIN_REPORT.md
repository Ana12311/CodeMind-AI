# React Login 模块开发报告

范围：`CodeMind-AI/frontend-react`
约束遵守：未修改 Spring Boot / 后端接口协议 / JWT 设计 / Axios 封装，仅实现前端。

---

## A. 修改文件

| 文件 | 类型 | 说明 |
| --- | --- | --- |
| `src/types/user.ts` | 新增 | `LoginRequest` / `LoginResponse` / `User` |
| `src/types/index.ts` | 修改 | `ApiResult.timestamp` 改为 `string`，并 re-export user 类型 |
| `src/api/auth.ts` | 新增 | `login()` / `fetchCurrentUser()` |
| `src/store/auth.ts` | 修改 | 增加 `login()`，`user` 类型换 `User` |
| `src/pages/Login/index.tsx` | 重写 | 完整登录表单 + 流程 + 异常处理 |

未改动：`request.ts`（Axios 封装）、`RequireAuth.tsx`、`MainLayout.tsx`、路由。

---

## B. API 调用流程

后端接口（由 `AuthController.java` 确认，非猜测）：

```
POST /api/v1/auth/login
  Request : { username, password }            // LoginRequest DTO
  Response: Result<LoginResponse>             // code=200 时 data 含 token
  LoginResponse: { accessToken, refreshToken, tokenType, expiresIn }

GET  /api/v1/auth/me
  Response: Result<UserInfoVO>
  UserInfoVO: { id, username, nickname, email, phone, avatar, status, roles }
```

前端调用链：

```
Login 表单 onFinish
  → api/auth.ts login(values)
  → request.ts http<T> (baseURL /api + Bearer token + Result 拆包)
  → POST /api/v1/auth/login
  → 返回 accessToken
  → store.setToken(accessToken)          // 使后续 /me 携带鉴权头
  → api/auth.ts fetchCurrentUser()       // 拉取完整用户信息
  → store.setUser(me)                    // 覆盖占位用户
  → navigate('/dashboard')
```

**类型关键发现**：后端将 `Long` 统一序列化为字符串（Jackson 雪花 ID 策略）。实测：

- `expiresIn` 实际为 `"7200"`（字符串，非 number）
- `timestamp` 实际为 `"1787721770315"`（字符串）
- `User.id` 实际为 `"2092467600066830338"`（雪花 ID 字符串）

已据此将 `expiresIn: string`、`timestamp: string`、`User.id: string` 对齐真实协议。

---

## C. JWT 保存方式

- 保存位置：Zustand `auth` store（`useAuthStore`），字段 `token` / `user`
- 持久化：`zustand/middleware persist`，`localStorage` key `codemind-auth`
- 请求注入：`request.ts` 请求拦截器读取 `useAuthStore.getState().token`，附加 `Authorization: Bearer <token>`
- 登出：`logout()` 清空 `token` / `user`，`RequireAuth` 检测无 token 自动重定向 `/login`
- 401 处理：响应拦截器捕获 HTTP 401 → `logout()` + 跳转 `/login`

`refreshToken` 当前未持久化（后续接入 `/v1/auth/refresh` 时补充）。

---

## D. 路由保护结果

| 场景 | 行为 |
| --- | --- |
| 未登录访问 `/dashboard` | `RequireAuth` 无 token → `<Navigate to="/login" state={{from}} replace />` |
| 登录成功 | `navigate('/dashboard', { replace: true })`，进入 Dashboard |
| 登录态过期（任意接口 401） | 拦截器 `logout()` + 跳 `/login` |

---

## E. 测试结果

### 用例 1：正确账号密码 ✅

```
POST /api/v1/auth/login {username:admin, password:******}
HTTP 200
{"code":200,"data":{"accessToken":"<JWT>","refreshToken":"<JWT>",
 "tokenType":"Bearer","expiresIn":"7200"},"message":"success",...}
```
→ 返回 JWT，`code=200`，前端保存 token 并跳转 `/dashboard`。

### 用例 2：错误密码 ✅

```
POST /api/v1/auth/login {username:admin, password:wrongpass123}
HTTP 401
{"code":401,"data":null,"message":"用户名或密码错误",...}
```
→ 前端捕获 `401`，读取 `response.data.message`，`message.error('用户名或密码错误')`。

前端异常分支覆盖：
- HTTP 401（密码错误）→ 显示后端 message
- 无 response（网络错误）→ `message.error('网络异常，请检查后端服务是否启动')`
- 其它 HTTP 错误 → 显示后端 message

### 用例 3：刷新页面 token 仍存在 ✅（代码级验证）

`persist` 中间件将 `token`/`user` 写入 `localStorage`（key `codemind-auth`），刷新后 store 自动 rehydrate，`RequireAuth` 读到 token 保持登录态。

> 注：浏览器 E2E 未执行（本机 Playwright 缺 Chrome 通道，需 `npx playwright install chrome`）。
> 用例 1/2 通过真实后端冒烟验证，用例 3 通过 persist 配置 + 构建验证。

### 构建验证 ✅

```
tsc && vite build  → 3114 modules transformed, ✓ built，零 TS 错误
```

---

## 总结

登录模块完成：接口契约、JWT 保存、路由保护、三类异常处理均落地，类型与真实后端序列化对齐，构建通过。可进入业务页面开发。
