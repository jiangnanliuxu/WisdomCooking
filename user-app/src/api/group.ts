import { request } from '@/utils/request'
import type { GroupMember, GroupVo } from '@/types/cook'

export function createGroup(data: { name: string; intro?: string; notice?: string; memberIds?: number[] }) {
  return request<GroupVo>({ url: '/api/v1/groups', method: 'POST', data })
}

export function getGroup(id: number) {
  return request<GroupVo>({ url: `/api/v1/groups/${id}`, method: 'GET' })
}

export function listGroupMembers(id: number) {
  return request<GroupMember[]>({ url: `/api/v1/groups/${id}/members`, method: 'GET' })
}

export function inviteGroupMembers(id: number, data: { userIds: number[] }) {
  return request<GroupVo>({ url: `/api/v1/groups/${id}/invite`, method: 'POST', data })
}

export function leaveGroup(id: number) {
  return request({ url: `/api/v1/groups/${id}/leave`, method: 'POST' })
}
