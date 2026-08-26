import { http } from './request'
import type { PageResult } from '@/types'
import type { Task, TaskQuery, CreateTaskRequest } from '@/types/task'
import type { ReviewResult } from '@/types/review'

// 分页查询任务：GET /api/v1/ai-tasks
export function listTasks(params: TaskQuery = {}): Promise<PageResult<Task>> {
  return http<PageResult<Task>>({ method: 'get', url: '/v1/ai-tasks', params })
}

// 创建任务：POST /api/v1/ai-tasks
export function createTask(data: CreateTaskRequest): Promise<Task> {
  return http<Task>({ method: 'post', url: '/v1/ai-tasks', data })
}

// 任务详情：GET /api/v1/ai-tasks/{id}
export function getTask(id: string): Promise<Task> {
  return http<Task>({ method: 'get', url: `/v1/ai-tasks/${id}` })
}

// 任务关联的审查结果：task.resultId → GET /api/v1/ai-reviews/{resultId}
export function getTaskResult(resultId: string): Promise<ReviewResult> {
  return http<ReviewResult>({ method: 'get', url: `/v1/ai-reviews/${resultId}` })
}
