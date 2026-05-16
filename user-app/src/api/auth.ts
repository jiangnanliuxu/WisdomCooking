import { request } from '@/utils/request'
import type { AuthTokenVo } from '@/types/cook'
import type { ApiResponse } from '@/types/common'

export function sendCode(data: { phone: string; scene: string }) {
  return request<Record<string, unknown>>({
    url: '/api/v1/auth/codes',
    method: 'POST',
    data,
  })
}

export function login(data: { phone: string; password?: string; code?: string }) {
  return request<AuthTokenVo>({
    url: '/api/v1/auth/login',
    method: 'POST',
    data,
  })
}

export function register(data: { phone: string; password: string; confirmPassword: string; nickname: string }) {
  return request<AuthTokenVo>({
    url: '/api/v1/auth/register',
    method: 'POST',
    data,
  })
}

export function resetPassword(data: { phone: string; code: string; password: string; confirmPassword: string }) {
  return request<ApiResponse>({
    url: '/api/v1/auth/password/reset',
    method: 'POST',
    data,
  })
}

export function logout() {
  return request({
    url: '/api/v1/auth/logout',
    method: 'POST',
  })
}
