// 登录请求体（对应后端 LoginRequest DTO）
export interface LoginRequest {
  username: string
  password: string
}

// 登录响应 data（对应后端 LoginResponse VO）
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  // 后端 Long 序列化为字符串，如 "7200"
  expiresIn: string
}

// 注册请求体（对应后端 RegisterRequest DTO）
export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  email?: string
  phone?: string
}

// 用户信息（对应后端 UserInfoVO）
export interface User {
  // 雪花 ID，后端序列化为字符串
  id: string
  username: string
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  roles?: string[]
}
