import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { CommentItem } from '@/types/cook'

export function listComments(targetType: string, targetId: number, page = 1, pageSize = 20) {
  return request<PageResult<CommentItem>>({
    url: `/api/v1/comments?target_type=${targetType}&target_id=${targetId}&page=${page}&pageSize=${pageSize}`,
    method: 'GET',
  })
}

export function createComment(data: { targetType: string; targetId: number; parentId?: number; content: string }) {
  return request<CommentItem>({ url: '/api/v1/comments', method: 'POST', data })
}

export function deleteComment(id: number) {
  return request({ url: `/api/v1/comments/${id}`, method: 'DELETE' })
}

export function likeComment(id: number) {
  return request({ url: `/api/v1/comments/${id}/like`, method: 'POST' })
}

export function unlikeComment(id: number) {
  return request({ url: `/api/v1/comments/${id}/like`, method: 'DELETE' })
}
