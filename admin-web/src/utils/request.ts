import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResponse } from '@/types/common'
import { getAdminToken, removeAdminToken } from './auth'

const service = axios.create({
  timeout: 15000,
})

const gatewayBaseURL = import.meta.env.VITE_GATEWAY_API_BASE || import.meta.env.VITE_ADMIN_API_BASE || ''
const cookBaseURL = import.meta.env.VITE_COOK_API_BASE || ''

function resolveBaseURL(url?: string) {
  if (url?.startsWith('/api/')) {
    return cookBaseURL
  }
  return gatewayBaseURL
}

function handleUnauthorized() {
  removeAdminToken()
  if (router.currentRoute.value.path !== '/login') {
    router.replace({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
  }
}

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const headers = config.headers || {}
  const isToken = headers.isToken === false
  const token = getAdminToken()

  if (!config.baseURL) {
    config.baseURL = resolveBaseURL(config.url)
  }

  if (!isToken && token) {
    headers.Authorization = `Bearer ${token}`
  }

  config.headers = headers
  return config
})

service.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse
    const code = payload.code ?? 200

    if (code === 401) {
      ElMessage.error(payload.msg || '登录状态已失效')
      handleUnauthorized()
      return Promise.reject(new Error(payload.msg || 'Unauthorized'))
    }

    if (code !== 200) {
      ElMessage.error(payload.msg || '请求失败')
      return Promise.reject(new Error(payload.msg || 'Request failed'))
    }

    return payload as never
  },
  (error: AxiosError) => {
    const message = error.message.includes('timeout')
      ? '请求超时，请稍后重试'
      : '接口请求失败，请检查网关或后端服务'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default service
