// 后端统一响应包装 Result<T>
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
}

// 登录请求体
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应 data 部分
export interface LoginResponse {
  accessToken: string
  refreshToken?: string
  tokenType?: string
  expiresIn?: number
}

// 当前登录用户信息
export interface UserInfo {
  id?: string | number
  username?: string
  nickname?: string
  avatar?: string
  [key: string]: unknown
}
