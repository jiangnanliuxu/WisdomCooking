import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { PostItem, TopicItem } from '@/types/cook'

export function listPosts(params: Record<string, unknown>) {
  return request<PageResult<PostItem>>({ url: `/api/v1/posts?${new URLSearchParams(params as Record<string, string>).toString()}`, method: 'GET' })
}

export function getPost(id: number) {
  return request<PostItem>({ url: `/api/v1/posts/${id}`, method: 'GET' })
}

export function createPost(data: Record<string, unknown>) {
  return request<PostItem>({ url: '/api/v1/posts', method: 'POST', data })
}

export function updatePost(id: number, data: Record<string, unknown>) {
  return request<PostItem>({ url: `/api/v1/posts/${id}`, method: 'PUT', data })
}

export function submitPost(id: number) {
  return request<PostItem>({ url: `/api/v1/posts/${id}/submit`, method: 'POST' })
}

export function withdrawPost(id: number) {
  return request<PostItem>({ url: `/api/v1/posts/${id}/withdraw`, method: 'POST' })
}

export function deletePost(id: number) {
  return request({ url: `/api/v1/posts/${id}`, method: 'DELETE' })
}

export function listMyPosts(params: Record<string, unknown>) {
  return request<PageResult<PostItem>>({ url: `/api/v1/users/me/posts?${new URLSearchParams(params as Record<string, string>).toString()}`, method: 'GET' })
}

export function listTopics() {
  return request<TopicItem[]>({ url: '/api/v1/topics', method: 'GET' })
}

export function likePost(id: number) {
  return request({ url: `/api/v1/posts/${id}/like`, method: 'POST' })
}

export function unlikePost(id: number) {
  return request({ url: `/api/v1/posts/${id}/like`, method: 'DELETE' })
}

export function favoritePost(id: number) {
  return request({ url: `/api/v1/posts/${id}/favorite`, method: 'POST' })
}

export function unfavoritePost(id: number) {
  return request({ url: `/api/v1/posts/${id}/favorite`, method: 'DELETE' })
}
