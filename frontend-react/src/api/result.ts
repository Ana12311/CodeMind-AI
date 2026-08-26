import { http } from './request'
import type { PageResult } from '@/types'
import type { ReviewResult } from '@/types/review'

// 分页查询审查结果：GET /api/v1/ai-reviews（支持 taskId / projectId 过滤）
export interface ReviewResultQuery {
  pageNum?: number
  pageSize?: number
  projectId?: string
  taskId?: string
  reviewType?: string
  severity?: string
}

export function listReviewResults(params: ReviewResultQuery = {}): Promise<PageResult<ReviewResult>> {
  return http<PageResult<ReviewResult>>({ method: 'get', url: '/v1/ai-reviews', params })
}

// 审查结果详情：GET /api/v1/ai-reviews/{id}
export function getReviewResult(id: string): Promise<ReviewResult> {
  return http<ReviewResult>({ method: 'get', url: `/v1/ai-reviews/${id}` })
}
