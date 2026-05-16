import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { ConversationItem, MessageItem } from '@/types/cook'

export function listConversations(type = '', page = 1, pageSize = 20) {
  return request<PageResult<ConversationItem>>({ url: `/api/v1/conversations?type=${type}&page=${page}&pageSize=${pageSize}`, method: 'GET' })
}

export function createPrivateConversation(data: { targetUserId: number }) {
  return request<ConversationItem>({ url: '/api/v1/conversations/private', method: 'POST', data })
}

export function listNotifications(page = 1, pageSize = 20) {
  return request<PageResult<MessageItem>>({ url: `/api/v1/notifications?page=${page}&pageSize=${pageSize}`, method: 'GET' })
}

export function listMessages(id: number, page = 1, pageSize = 50) {
  return request<PageResult<MessageItem>>({ url: `/api/v1/conversations/${id}/messages?page=${page}&pageSize=${pageSize}`, method: 'GET' })
}

export function sendMessage(id: number, data: { messageType: string; content?: string; mediaUrl?: string }) {
  return request<MessageItem>({ url: `/api/v1/conversations/${id}/messages`, method: 'POST', data })
}

export function markRead(id: number) {
  return request({ url: `/api/v1/conversations/${id}/read`, method: 'POST' })
}

export function updateConversationSettings(id: number, data: { muted?: boolean; pinned?: boolean }) {
  return request<ConversationItem>({ url: `/api/v1/conversations/${id}/settings`, method: 'PUT', data })
}
