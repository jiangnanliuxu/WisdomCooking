import request from '@/utils/request'
import type { AdminUserInfo, ApiResponse, LoginToken } from '@/types/common'

export function getCaptcha() {
  return request.get<unknown, { captchaEnabled?: boolean; img?: string; uuid?: string }>('/code', {
    headers: { isToken: false },
  })
}

export function login(payload: { username: string; password: string; code?: string; uuid?: string }) {
  return request.post<unknown, ApiResponse<LoginToken>>('/auth/login', payload, {
    headers: { isToken: false },
  })
}

export function logout() {
  return request.delete('/auth/logout')
}

export function getAdminInfo() {
  return request.get<unknown, ApiResponse<never> & AdminUserInfo>('/system/user/getInfo')
}
