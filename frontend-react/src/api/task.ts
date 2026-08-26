import { http } from './request'
import type { PageResult } from '@/types'
import type { Task, TaskQuery } from '@/types/task'

// 分页查询任务：GET /api/v1/ai-tasks
export function listTasks(params: TaskQuery = {}): Promise<PageResult<Task>> {
  return http<PageResult<Task>>({ method: 'get', url: '/v1/ai-tasks', params })
}
