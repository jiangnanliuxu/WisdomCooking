import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { PostItem } from '@/types/cook'

export function listPostAudits(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<PostItem>>>('/api/admin/v1/post-audits', { params })
}

export function getPostAudit(id: number) {
  return request.get<unknown, ApiResponse<PostItem>>(`/api/admin/v1/post-audits/${id}`)
}

export function approvePostAudit(id: number) {
  return request.post<unknown, ApiResponse<PostItem>>(`/api/admin/v1/post-audits/${id}/approve`)
}

export function blockPostAudit(id: number, payload: { reason: string; action?: string }) {
  return request.post<unknown, ApiResponse<PostItem>>(`/api/admin/v1/post-audits/${id}/block`, payload)
}

export function restorePostAudit(id: number) {
  return request.post<unknown, ApiResponse<PostItem>>(`/api/admin/v1/post-audits/${id}/restore`)
}
