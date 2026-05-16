import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { CategoryGroup, RecipeDetail, RecipeListItem } from '@/types/cook'

export function listRecipes(params: Record<string, unknown>) {
  return request<PageResult<RecipeListItem>>({ url: `/api/v1/recipes?${new URLSearchParams(params as Record<string, string>).toString()}`, method: 'GET' })
}

export function getRecipe(id: number) {
  return request<RecipeDetail>({ url: `/api/v1/recipes/${id}`, method: 'GET' })
}

export function createRecipe(data: Record<string, unknown>) {
  return request<RecipeDetail>({ url: '/api/v1/recipes', method: 'POST', data })
}

export function updateRecipe(id: number, data: Record<string, unknown>) {
  return request<RecipeDetail>({ url: `/api/v1/recipes/${id}`, method: 'PUT', data })
}

export function submitRecipe(id: number) {
  return request<RecipeDetail>({ url: `/api/v1/recipes/${id}/submit`, method: 'POST' })
}

export function withdrawRecipe(id: number) {
  return request<RecipeDetail>({ url: `/api/v1/recipes/${id}/withdraw`, method: 'POST' })
}

export function deleteRecipe(id: number) {
  return request({ url: `/api/v1/recipes/${id}`, method: 'DELETE' })
}

export function listMyRecipes(params: Record<string, unknown>) {
  return request<PageResult<RecipeListItem>>({ url: `/api/v1/users/me/recipes?${new URLSearchParams(params as Record<string, string>).toString()}`, method: 'GET' })
}

export function listCategories() {
  return request<CategoryGroup[]>({ url: '/api/v1/categories', method: 'GET' })
}

export function likeRecipe(id: number) {
  return request({ url: `/api/v1/recipes/${id}/like`, method: 'POST' })
}

export function unlikeRecipe(id: number) {
  return request({ url: `/api/v1/recipes/${id}/like`, method: 'DELETE' })
}

export function favoriteRecipe(id: number) {
  return request({ url: `/api/v1/recipes/${id}/favorite`, method: 'POST' })
}

export function unfavoriteRecipe(id: number) {
  return request({ url: `/api/v1/recipes/${id}/favorite`, method: 'DELETE' })
}

export function shareRecipe(id: number) {
  return request({ url: `/api/v1/recipes/${id}/share`, method: 'POST' })
}
