import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { RecipeDetail, RecipeListItem } from '@/types/cook'

export function listAdminRecipes(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<RecipeListItem>>>('/api/admin/v1/recipes', { params })
}

export function getAdminRecipe(id: number) {
  return request.get<unknown, ApiResponse<RecipeDetail>>(`/api/admin/v1/recipes/${id}`)
}

export function deleteAdminRecipe(id: number) {
  return request.delete(`/api/admin/v1/recipes/${id}`)
}

export function onlineRecipe(id: number) {
  return request.post<unknown, ApiResponse<RecipeDetail>>(`/api/admin/v1/recipes/${id}/online`)
}

export function offlineRecipe(id: number) {
  return request.post<unknown, ApiResponse<RecipeDetail>>(`/api/admin/v1/recipes/${id}/offline`)
}

export function transcodeRecipe(id: number) {
  return request.post<unknown, ApiResponse<RecipeDetail>>(`/api/admin/v1/recipes/${id}/video/transcode`)
}

export function listRecipeAudits(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<RecipeListItem>>>('/api/admin/v1/recipe-audits', { params })
}

export function getRecipeAudit(id: number) {
  return request.get<unknown, ApiResponse<RecipeDetail>>(`/api/admin/v1/recipe-audits/${id}`)
}

export function approveRecipeAudit(id: number) {
  return request.post<unknown, ApiResponse<RecipeDetail>>(`/api/admin/v1/recipe-audits/${id}/approve`)
}

export function rejectRecipeAudit(id: number, reason: string) {
  return request.post<unknown, ApiResponse<RecipeDetail>>(`/api/admin/v1/recipe-audits/${id}/reject`, { reason })
}
