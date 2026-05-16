import request from '@/utils/request'
import type { ApiResponse } from '@/types/common'
import type { Category, CategoryGroup } from '@/types/cook'

export function listAdminCategories() {
  return request.get<unknown, ApiResponse<CategoryGroup[]>>('/api/admin/v1/categories')
}

export function createCategory(payload: Record<string, unknown>) {
  return request.post<unknown, ApiResponse<Category>>('/api/admin/v1/categories', payload)
}

export function updateCategory(id: number, payload: Record<string, unknown>) {
  return request.put<unknown, ApiResponse<Category>>(`/api/admin/v1/categories/${id}`, payload)
}

export function deleteCategory(id: number) {
  return request.delete(`/api/admin/v1/categories/${id}`)
}
