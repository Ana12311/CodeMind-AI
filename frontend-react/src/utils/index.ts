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

// 将后端时间转为北京时间显示。后端 LocalDateTime 按容器 UTC 时钟生成（无时区标记），
// 这里按 UTC 解析后加 8 小时得北京时间，并去掉毫秒。
// 例：2026-08-26T13:02:26 -> 2026-08-26 21:02:26
export function formatTime(value: string | null | undefined): string {
  if (!value) return ''
  const date = new Date(`${value}Z`)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').replace(/\.\d+$/, '')
  }
  return new Date(date.getTime() + 8 * 60 * 60 * 1000)
    .toISOString()
    .slice(0, 19)
    .replace('T', ' ')
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
