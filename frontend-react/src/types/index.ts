// 后端统一响应包装 Result<T>
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  // 后端 Long 序列化为字符串（如 "1787721770315"）
  timestamp: string
}

export * from './user'
