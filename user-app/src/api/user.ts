import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { UserInteraction, UserProfile, UserPublicProfile } from '@/types/cook'

export function getCurrentUser() {
  return request<UserProfile>({ url: '/api/v1/users/me', method: 'GET' })
}

export function updateCurrentUser(data: Record<string, unknown>) {
  return request<UserProfile>({ url: '/api/v1/users/me', method: 'PUT', data })
}

export function updateInterests(data: { interestTags: string[] }) {
  return request<UserProfile>({ url: '/api/v1/users/me/interests', method: 'PUT', data })
}

export function getUserPublicProfile(id: number) {
  return request<UserPublicProfile>({ url: `/api/v1/users/${id}/profile`, method: 'GET' })
}

export function listRecommendedUsers(limit = '8') {
  return request<UserPublicProfile[]>({ url: `/api/v1/users/recommended?limit=${limit}`, method: 'GET' })
}

export function listMyFollowers(params: Record<string, string>) {
  return request<PageResult<UserPublicProfile>>({ url: `/api/v1/users/me/followers?${new URLSearchParams(params).toString()}`, method: 'GET' })
}

export function listMyFollowing(params: Record<string, string>) {
  return request<PageResult<UserPublicProfile>>({ url: `/api/v1/users/me/following?${new URLSearchParams(params).toString()}`, method: 'GET' })
}

export function followUser(id: number) {
  return request({ url: `/api/v1/users/${id}/follow`, method: 'POST' })
}

export function unfollowUser(id: number) {
  return request({ url: `/api/v1/users/${id}/follow`, method: 'DELETE' })
}

export function listMyFavorites(params: Record<string, string>) {
  return request<PageResult<UserInteraction>>({ url: `/api/v1/users/me/favorites?${new URLSearchParams(params).toString()}`, method: 'GET' })
}

export function listMyLikes(params: Record<string, string>) {
  return request<PageResult<UserInteraction>>({ url: `/api/v1/users/me/likes?${new URLSearchParams(params).toString()}`, method: 'GET' })
}
