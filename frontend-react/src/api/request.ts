import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { message } from 'antd'
import { useAuthStore } from '@/store/auth'
import type { ApiResult } from '@/types'

// baseURL 读取环境变量，默认 /api（配合 vite 开发代理）
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'

const request = axios.create({
  baseURL,
  timeout: 30000,
})

// 请求拦截：自动附加 JWT Token
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: AxiosError) => Promise.reject(error),
)

// 响应拦截：统一拆包 + 401 跳转登录
request.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResult>) => {
    const status = error.response?.status
    if (status === 401) {
      useAuthStore.getState().logout()
      // 避免在登录页重复跳转
      if (window.location.pathname !== '/login') {
        message.warning('登录已过期，请重新登录')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

// 泛型请求：直接返回后端 data 字段
export async function http<T = unknown>(
  config: AxiosRequestConfig,
): Promise<T> {
  const response = await request.request<ApiResult<T>>(config)
  const result = response.data
  if (result.code !== 200) {
    message.error(result.message || '请求失败')
    throw new Error(result.message || '请求失败')
  }
  return result.data
}

export default request
