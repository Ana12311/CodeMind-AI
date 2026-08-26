# React 前端初始化报告

## 一、工程概览

| 项 | 值 |
| --- | --- |
| 工程目录 | `frontend-react/` |
| 框架 | React 18 + TypeScript + Vite 5 |
| UI 组件库 | Ant Design 5 |
| 路由 | React Router 6 |
| HTTP 客户端 | Axios |
| 状态管理 | Zustand（persist 持久化） |
| 包管理器 | npm |
| 构建产物 | `dist/` |

## 二、项目结构

```
frontend-react/
├── index.html
├── package.json
├── package-lock.json
├── vite.config.ts          # 插件、@ 别名、/api 开发代理
├── tsconfig.json           # TS 编译配置（strict、@/* 路径映射）
├── .env.example            # 环境变量模板（VITE_API_BASE_URL）
├── .gitignore
└── src/
    ├── main.tsx            # 入口：ConfigProvider(zhCN) + App
    ├── App.tsx             # BrowserRouter 包装
    ├── index.css           # 全局样式重置
    ├── vite-env.d.ts       # Vite 客户端类型
    ├── api/
    │   └── request.ts      # Axios 实例 + 拦截器 + http 泛型方法
    ├── components/
    │   ├── RequireAuth.tsx # 路由守卫
    │   └── layout/
    │       └── MainLayout.tsx  # Header/Sider/Content 主布局
    ├── pages/
    │   ├── Login/          # 登录页（已实现表单 + 调登录接口）
    │   ├── Dashboard/      # 占位
    │   ├── Project/        # 占位
    │   ├── File/           # 占位
    │   ├── Task/           # 占位
    │   └── Result/         # 占位
    ├── router/
    │   └── index.tsx       # 路由表（useRoutes）
    ├── store/
    │   └── auth.ts         # 认证状态（token/user）
    ├── types/
    │   └── index.ts        # ApiResult / LoginRequest / UserInfo 等
    └── utils/
        └── index.ts        # getErrorMessage 等工具
```

## 三、依赖列表（实际安装版本）

### 运行时依赖

| 包 | 版本 | 用途 |
| --- | --- | --- |
| react | 18.3.1 | 核心 |
| react-dom | 18.3.1 | DOM 渲染 |
| antd | 5.29.3 | UI 组件库 |
| @ant-design/icons | 5.6.1 | 图标 |
| axios | 1.19.0 | HTTP 客户端 |
| react-router-dom | 6.30.6 | 路由 |
| zustand | 4.5.7 | 状态管理 |

### 开发依赖

| 包 | 版本 | 用途 |
| --- | --- | --- |
| typescript | 5.9.3 | 类型编译 |
| vite | 5.4.21 | 构建/开发服务器 |
| @vitejs/plugin-react | 4.7.0 | React 转换插件 |
| @types/react | 18.3.31 | React 类型 |
| @types/react-dom | 18.3.7 | ReactDOM 类型 |

## 四、关键设计

1. **Axios 统一封装**（`src/api/request.ts`）
   - `baseURL` 读取 `import.meta.env.VITE_API_BASE_URL`，默认 `/api`
   - 请求拦截器自动附加 `Authorization: Bearer <token>`
   - 响应拦截器捕获 401，清空登录态并跳转 `/login`
   - `http<T>()` 泛型方法统一拆 `Result<T>` 包装，`code !== 200` 抛错并提示

2. **认证状态**（`src/store/auth.ts`）
   - Zustand + `persist` 中间件，`localStorage` 持久化（key: `codemind-auth`）
   - 字段：`token`、`user`；方法：`setToken`、`setUser`、`logout`
   - 仅持久化数据字段，方法不序列化

3. **路由守卫**（`src/components/RequireAuth.tsx`）
   - 无 `token` 重定向 `/login`，业务路由嵌套在 `MainLayout` 下

4. **开发代理**（`vite.config.ts`）
   - `/api` → `http://localhost:8080`，避免开发跨域
   - 业务路径统一写 `/v1/...`，如 `POST /v1/auth/login`

## 五、启动方式

```bash
cd frontend-react

# 安装依赖
npm install

# 复制环境变量（如无特殊需求可跳过，默认 /api）
cp .env.example .env   # Windows: copy .env.example .env

# 启动开发服务器（http://localhost:5173）
npm run dev

# 生产构建（产物在 dist/）
npm run build

# 本地预览构建产物
npm run preview
```

前提：后端 Spring Boot（8080）与 AI 服务已启动。

## 六、后续开发计划

1. **登录页完善**：按后端 `LoginResponse` 实际字段补全用户信息展示（当前仅存 `username`）。
2. **仪表盘**：概览统计（项目数、任务数、成功率等）。
3. **项目管理**：项目列表 / 创建 / 删除，对接 `/v1/projects`。
4. **代码文件**：文件上传，对接 `/v1/files/upload`。
5. **AI 任务**：任务列表 / 创建（`taskType=CODE_REVIEW`），对接 `/v1/ai-tasks`。
6. **评审结果**：结果详情展示，对接任务详情 / 结果接口。
7. **路由权限细化**：按角色细化菜单与访问控制。
8. **代码分包**：antd 全量引入致 chunk >500kB，后续用 `manualChunks` 或按需引入优化。
