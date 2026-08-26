// 项目返回对象（对应后端 ProjectVO）
export interface Project {
  // 雪花 ID，后端序列化为字符串
  id: string
  name: string
  description?: string | null
  ownerId?: string
  language?: string
  repoUrl?: string | null
  // 状态：1 进行中，0 归档
  status?: number
  createTime?: string
  updateTime?: string
  createBy?: string
  updateBy?: string
}

// 创建项目请求（对应后端 ProjectCreateRequest）
export interface CreateProjectRequest {
  name: string
  description?: string
  language?: string
  repoUrl?: string
  status?: number
}

// 分页查询项目（对应后端 ProjectQueryRequest）
export interface ProjectQuery {
  pageNum?: number
  pageSize?: number
  name?: string
  status?: number
  ownerId?: string
}
