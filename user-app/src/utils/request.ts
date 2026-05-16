import type { ApiResponse } from '@/types/common'
import { clearUserToken, getUserToken } from './auth'

interface RequestOptions<T> extends Omit<UniNamespace.RequestOptions, 'data'> {
  data?: UniNamespace.RequestOptions['data']
  showNetworkErrorToast?: boolean
}

export function composeApiUrl(url: string) {
  const base = import.meta.env.VITE_USER_API_BASE || ''
  return `${base}${url}`
}

/**
 * uni-app 统一请求封装。
 * 用户端接口全部走网关，登录失效时清空 token 并回到登录页。
 */
export function request<T>(options: RequestOptions<T>) {
  return new Promise<ApiResponse<T>>((resolve, reject) => {
    uni.request({
      ...options,
      url: composeApiUrl(options.url || ''),
      header: {
        'Content-Type': 'application/json',
        ...(options.header || {}),
        ...(getUserToken() ? { Authorization: `Bearer ${getUserToken()}` } : {}),
      },
      success: (response) => {
        const payload = response.data as ApiResponse<T>
        const code = payload.code ?? 200
        if (code === 401) {
          clearUserToken()
          uni.showToast({ title: payload.msg || '请先登录', icon: 'none' })
          uni.navigateTo({ url: '/pages/auth/login' })
          reject(new Error(payload.msg || 'Unauthorized'))
          return
        }
        if (code !== 200) {
          uni.showToast({ title: payload.msg || '请求失败', icon: 'none' })
          reject(new Error(payload.msg || 'Request failed'))
          return
        }
        resolve(payload)
      },
      fail: (error) => {
        if (options.showNetworkErrorToast || import.meta.env.PROD) {
          uni.showToast({ title: '接口请求失败', icon: 'none' })
        }
        reject(error)
      },
    })
  })
}

interface UploadFileOptions<T> {
  url: string
  filePath: string
  name?: string
  formData?: Record<string, unknown>
  errorMessage?: string
}

export function uploadFile<T>({ url, filePath, name = 'file', formData, errorMessage }: UploadFileOptions<T>) {
  return new Promise<ApiResponse<T>>((resolve, reject) => {
    uni.uploadFile({
      url: composeApiUrl(url),
      filePath,
      name,
      formData,
      header: {
        ...(getUserToken() ? { Authorization: `Bearer ${getUserToken()}` } : {}),
      },
      success: (response) => {
        try {
          const payload = JSON.parse(response.data || '{}') as ApiResponse<T>
          const code = payload.code ?? 200
          if (code === 401) {
            clearUserToken()
            uni.showToast({ title: payload.msg || '请先登录', icon: 'none' })
            uni.navigateTo({ url: '/pages/auth/login' })
            reject(new Error(payload.msg || 'Unauthorized'))
            return
          }
          if (code !== 200) {
            uni.showToast({ title: payload.msg || errorMessage || '上传失败', icon: 'none' })
            reject(new Error(payload.msg || 'Upload failed'))
            return
          }
          resolve(payload)
        }
        catch (error) {
          uni.showToast({ title: errorMessage || '上传失败', icon: 'none' })
          reject(error)
        }
      },
      fail: (error) => {
        uni.showToast({ title: errorMessage || '上传失败', icon: 'none' })
        reject(error)
      },
    })
  })
}
