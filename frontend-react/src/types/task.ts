// AI 任务返回对象（对应后端 TaskVO）
export interface Task {
  id: string
  projectId?: string
  taskType?: string
  // 状态码：0 待处理 1 处理中 2 成功 3 失败
  status?: number
  statusDesc?: string
  params?: string
  resultId?: string | null
  errorMsg?: string | null
  submitBy?: string
  startTime?: string | null
  endTime?: string | null
  createTime?: string
  updateTime?: string
}

// 分页查询任务（对应后端 TaskQueryRequest）
export interface TaskQuery {
  pageNum?: number
  pageSize?: number
  projectId?: string
  taskType?: string
  status?: number
}
