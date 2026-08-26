// AI 审查结果返回对象（对应后端 ReviewResultVO）
export interface ReviewResult {
  id: string
  taskId?: string
  projectId?: string
  fileId?: string | null
  reviewType?: string
  severity?: string | null
  lineNo?: number | null
  summary?: string | null
  detail?: string
  status?: number
  createTime?: string
  updateTime?: string
}
