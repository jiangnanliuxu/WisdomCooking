import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import type {
  AdminDashboardSummary,
  AdminGroup,
  AdminUser,
  BannerItem,
  CategoryRatio,
  FeedbackItem,
  MediaAsset,
  TrendPoint,
  RecentOperationLog,
} from '@/types/cook'

export function getDashboardSummary() {
  return request.get<unknown, ApiResponse<AdminDashboardSummary>>('/api/admin/v1/dashboard/summary')
}

export function getUserGrowth() {
  return request.get<unknown, ApiResponse<TrendPoint[]>>('/api/admin/v1/dashboard/user-growth')
}

export function getRecipeCategoryRatio() {
  return request.get<unknown, ApiResponse<CategoryRatio[]>>('/api/admin/v1/dashboard/recipe-category-ratio')
}

export function listRecentOperationLogs(limit = 10) {
  return request.get<unknown, ApiResponse<RecentOperationLog[]>>('/api/admin/v1/operation-logs/recent', {
    params: { limit },
  })
}

export function listBanners(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<BannerItem>>>('/api/admin/v1/banners', { params })
}

export function createBanner(payload: Record<string, unknown>) {
  return request.post<unknown, ApiResponse<BannerItem>>('/api/admin/v1/banners', payload)
}

export function updateBanner(id: number, payload: Record<string, unknown>) {
  return request.put<unknown, ApiResponse<BannerItem>>(`/api/admin/v1/banners/${id}`, payload)
}

export function deleteBanner(id: number) {
  return request.delete(`/api/admin/v1/banners/${id}`)
}

export function onlineBanner(id: number) {
  return request.post(`/api/admin/v1/banners/${id}/online`)
}

export function offlineBanner(id: number) {
  return request.post(`/api/admin/v1/banners/${id}/offline`)
}

export function moveBanner(id: number, sortNo: number) {
  return request.post(`/api/admin/v1/banners/${id}/move`, { sortNo })
}

export function listMediaAssets(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<MediaAsset>>>('/api/admin/v1/media-assets', { params })
}

export function getMediaAsset(id: number) {
  return request.get<unknown, ApiResponse<MediaAsset>>(`/api/admin/v1/media-assets/${id}`)
}

export function uploadAdminImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, ApiResponse<MediaAsset>>('/api/admin/v1/media-assets/images', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function listAdminUsers(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<AdminUser>>>('/api/admin/v1/users', { params })
}

export function getAdminUser(id: number) {
  return request.get<unknown, ApiResponse<AdminUser>>(`/api/admin/v1/users/${id}`)
}

export function muteUser(id: number, reason?: string) {
  return request.post(`/api/admin/v1/users/${id}/mute`, null, { params: { reason } })
}

export function banUser(id: number, reason?: string) {
  return request.post(`/api/admin/v1/users/${id}/ban`, null, { params: { reason } })
}

export function unblockUser(id: number, reason?: string) {
  return request.post(`/api/admin/v1/users/${id}/unblock`, null, { params: { reason } })
}

export function listAdminGroups(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<AdminGroup>>>('/api/admin/v1/groups', { params })
}

export function getAdminGroup(id: number) {
  return request.get<unknown, ApiResponse<AdminGroup>>(`/api/admin/v1/groups/${id}`)
}

export function dissolveGroup(id: number, reason?: string) {
  return request.post(`/api/admin/v1/groups/${id}/dissolve`, null, { params: { reason } })
}

export function listFeedbacks(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<FeedbackItem>>>('/api/admin/v1/feedbacks', { params })
}

export function getFeedback(id: number) {
  return request.get<unknown, ApiResponse<FeedbackItem>>(`/api/admin/v1/feedbacks/${id}`)
}
