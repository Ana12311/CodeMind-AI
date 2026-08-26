import { http } from './request'
import type { PageResult } from '@/types'
import type { FileItem, FileQuery } from '@/types/file'

// 分页查询文件：GET /api/v1/files
export function listFiles(params: FileQuery = {}): Promise<PageResult<FileItem>> {
  return http<PageResult<FileItem>>({ method: 'get', url: '/v1/files', params })
}
