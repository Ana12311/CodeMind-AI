import axios from 'axios'
import { message } from 'antd'
import type { ApiResult } from '@/types'

// 从任意错误对象中提取可读信息
export function getErrorMessage(error: unknown): string {
  if (typeof error === 'string') return error
  if (error instanceof Error) return error.message
  return '发生未知错误'
}

// 统一处理请求异常（业务错误已由 http 层提示，这里只兜底传输层错误）
export function handleRequestError(error: unknown, fallback = '操作失败'): void {
  if (!axios.isAxiosError(error)) return
  if (!error.response) {
    message.error('网络异常，请检查网络或后端服务')
    return
  }
  const status = error.response.status
  const msg = (error.response.data as ApiResult)?.message
  if (status === 401) return // 拦截器已统一登出跳转
  if (status === 403) message.error(msg || '无权限访问')
  else if (status >= 500) message.error(msg || '服务器内部错误')
  else message.error(msg || fallback)
}

// 将 ISO 时间（2026-08-26T12:54:54）转为可读格式（2026-08-26 12:54:54）
export function formatTime(value: string | null | undefined): string {
  if (!value) return ''
  return value.replace('T', ' ').replace(/\.\d+$/, '')
}

// 格式化字节数
export function formatFileSize(bytes: number | string | undefined): string {
  const n = Number(bytes)
  if (bytes === undefined || bytes === null || Number.isNaN(n)) return '-'
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  if (n < 1024 * 1024 * 1024) return `${(n / 1024 / 1024).toFixed(1)} MB`
  return `${(n / 1024 / 1024 / 1024).toFixed(1)} GB`
}
