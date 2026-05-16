import { request } from '@/utils/request'
import type { CheckinItem, CheckinSummary } from '@/types/cook'

/**
 * 饮食打卡接口。
 * 打卡列表、统计、新增编辑删除和生成动态都从这里走。
 */
export function getCheckinSummary() {
  return request<CheckinSummary>({ url: '/api/v1/checkins/summary', method: 'GET' })
}

export function listCheckinsByMonth(date?: string) {
  const query = date ? `?date=${date}` : ''
  return request<CheckinItem[]>({ url: `/api/v1/checkins${query}`, method: 'GET' })
}

export function listCheckinsByDate(date: string) {
  return request<CheckinItem[]>({ url: `/api/v1/checkins/by-date?date=${date}`, method: 'GET' })
}

export function createCheckin(data: Record<string, unknown>) {
  return request<CheckinItem>({ url: '/api/v1/checkins', method: 'POST', data })
}

export function updateCheckin(id: number, data: Record<string, unknown>) {
  return request<CheckinItem>({ url: `/api/v1/checkins/${id}`, method: 'PUT', data })
}

export function deleteCheckin(id: number) {
  return request({ url: `/api/v1/checkins/${id}`, method: 'DELETE' })
}

export function generateCheckinPost(id: number) {
  return request<CheckinItem>({ url: `/api/v1/checkins/${id}/post`, method: 'POST' })
}
