export interface ApiResponse<T = unknown> {
  code: number
  msg?: string
  data?: T
  [key: string]: unknown
}

export interface PageQuery {
  page?: number
  pageSize?: number
}

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  items: T[]
}

export interface LoginToken {
  access_token: string
  expires_in: number
}

export interface AdminUserInfo {
  user: {
    userId: number
    userName: string
    nickName: string
    avatar?: string
  }
  roles: string[]
  permissions: string[]
}
