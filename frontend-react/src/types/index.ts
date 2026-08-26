// 后端统一响应包装 Result<T>
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  // 后端 Long 序列化为字符串，如 "1787721770315"
  timestamp: string
}

// 后端分页响应体 PageResult<T>
export interface PageResult<T> {
  records: T[]
  // 后端 Long 序列化为字符串，如 "3"
  total: string
  pageNum: number
  pageSize: number
}

export * from './user'
export * from './project'
export * from './task'
export * from './file'
export * from './review'
