export interface ApiResponse<T = unknown> {
  code: number
  msg?: string
  data?: T
  [key: string]: unknown
}

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  items: T[]
}
