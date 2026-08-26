import { http } from './request'
import type { AxiosProgressEvent } from 'axios'
import type { PageResult } from '@/types'
import type { FileItem, FileQuery } from '@/types/file'

// 分页查询文件：GET /api/v1/files
export function listFiles(params: FileQuery = {}): Promise<PageResult<FileItem>> {
  return http<PageResult<FileItem>>({ method: 'get', url: '/v1/files', params })
}

// 上传文件：POST /api/v1/files/upload（multipart: file + projectId）
export function uploadFile(
  file: File,
  projectId: string,
  onProgress?: (percent: number) => void,
): Promise<FileItem> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('projectId', projectId)
  return http<FileItem>({
    method: 'post',
    url: '/v1/files/upload',
    data: formData,
    onUploadProgress: (e: AxiosProgressEvent) => {
      if (e.total && onProgress) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    },
  })
}

// 文件详情：GET /api/v1/files/{id}
export function getFile(id: string): Promise<FileItem> {
  return http<FileItem>({ method: 'get', url: `/v1/files/${id}` })
}

// 删除文件（逻辑删除）：DELETE /api/v1/files/{id}
export function deleteFile(id: string): Promise<void> {
  return http<void>({ method: 'delete', url: `/v1/files/${id}` })
}
