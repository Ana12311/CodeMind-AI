import { http } from './request'
import type { PageResult } from '@/types'
import type { Project, CreateProjectRequest, ProjectQuery } from '@/types/project'

// 分页查询项目：GET /api/v1/projects
export function listProjects(params: ProjectQuery = {}): Promise<PageResult<Project>> {
  return http<PageResult<Project>>({ method: 'get', url: '/v1/projects', params })
}

// 创建项目：POST /api/v1/projects
export function createProject(data: CreateProjectRequest): Promise<Project> {
  return http<Project>({ method: 'post', url: '/v1/projects', data })
}

// 项目详情：GET /api/v1/projects/{id}
export function getProject(id: string): Promise<Project> {
  return http<Project>({ method: 'get', url: `/v1/projects/${id}` })
}

// 删除项目（逻辑删除）：DELETE /api/v1/projects/{id}
export function deleteProject(id: string): Promise<void> {
  return http<void>({ method: 'delete', url: `/v1/projects/${id}` })
}
