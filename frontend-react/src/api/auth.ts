import { http } from './request'
import type { LoginRequest, LoginResponse, User } from '@/types/user'

// 登录：POST /api/v1/auth/login
export function login(data: LoginRequest): Promise<LoginResponse> {
  return http<LoginResponse>({
    method: 'post',
    url: '/v1/auth/login',
    data,
  })
}

// 当前用户信息：GET /api/v1/auth/me（需 Authorization 头）
export function fetchCurrentUser(): Promise<User> {
  return http<User>({
    method: 'get',
    url: '/v1/auth/me',
  })
}
