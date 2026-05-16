import { request, uploadFile } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type {
  FeedbackItem,
  HomeData,
  MediaAsset,
  ReportItem,
  SearchResult,
  VideoMultipartInit,
  VideoMultipartSession,
} from '@/types/cook'

/**
 * 用户端运营相关接口封装。
 * 这里集中处理首页、搜索、反馈、举报和资源查询请求。
 */
export function getHomeData(params: Record<string, string> = {}) {
  const query = new URLSearchParams(params).toString()
  return request<HomeData>({ url: `/api/v1/home${query ? `?${query}` : ''}`, method: 'GET' })
}

export function searchAll(params: Record<string, string>) {
  return request<SearchResult>({ url: `/api/v1/search?${new URLSearchParams(params).toString()}`, method: 'GET' })
}

export function getHotKeywords(limit = '8') {
  return request<string[]>({ url: `/api/v1/search/hot?limit=${limit}`, method: 'GET' })
}

export function getSearchHistory() {
  return request<string[]>({ url: '/api/v1/search/history', method: 'GET' })
}

export function clearSearchHistory() {
  return request({ url: '/api/v1/search/history', method: 'DELETE' })
}

export function listMyFeedbacks(params: Record<string, string>) {
  return request<PageResult<FeedbackItem>>({ url: `/api/v1/feedbacks?${new URLSearchParams(params).toString()}`, method: 'GET' })
}

export function createFeedback(data: Record<string, unknown>) {
  return request<FeedbackItem>({ url: '/api/v1/feedbacks', method: 'POST', data })
}

export function listMyReports(params: Record<string, string>) {
  return request<PageResult<ReportItem>>({ url: `/api/v1/reports?${new URLSearchParams(params).toString()}`, method: 'GET' })
}

export function createReport(data: Record<string, unknown>) {
  return request<ReportItem>({ url: '/api/v1/reports', method: 'POST', data })
}

export function getMediaAsset(mediaId: number) {
  return request<MediaAsset>({ url: `/api/v1/uploads/${mediaId}`, method: 'GET' })
}

export function uploadImage(filePath: string) {
  return uploadFile<MediaAsset>({
    url: '/api/v1/uploads/images',
    filePath,
    errorMessage: '图片上传失败，请确认图片不超过 20MB',
  })
}

export function uploadVideo(filePath: string) {
  return uploadFile<MediaAsset>({
    url: '/api/v1/uploads/videos',
    filePath,
    errorMessage: '视频上传失败，请确认视频不超过 500MB',
  })
}

export function initVideoMultipartUpload(data: {
  fileName: string
  fileSize: number
  contentType?: string
  fingerprint: string
}) {
  return request<VideoMultipartInit>({
    url: '/api/v1/uploads/videos/multipart/init',
    method: 'POST',
    data,
  })
}

export function completeVideoMultipartUpload(data: { sessionId: string, objectKey?: string }) {
  return request<MediaAsset>({
    url: '/api/v1/uploads/videos/multipart/complete',
    method: 'POST',
    data,
  })
}

export function getVideoMultipartSession(sessionId: string) {
  return request<VideoMultipartSession>({
    url: `/api/v1/uploads/videos/multipart/${sessionId}`,
    method: 'GET',
  })
}

export function cancelVideoMultipartUpload(sessionId: string) {
  return request({
    url: `/api/v1/uploads/videos/multipart/${sessionId}/cancel`,
    method: 'POST',
  })
}

export function uploadAudio(filePath: string) {
  return uploadFile<MediaAsset>({ url: '/api/v1/uploads/audios', filePath })
}
