import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/common'
import type {
  AiConversation,
  AiConversationDetail,
  AiKnowledgeDocument,
  AiKnowledgeIndexJob,
  AiKnowledgeTextPayload,
  AiModel,
  AiModelForm,
  AiRecognition,
} from '@/types/cook'

export function listAiModels(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<AiModel>>>('/api/admin/v1/ai/models', { params })
}

export function createAiModel(payload: AiModelForm) {
  return request.post<unknown, ApiResponse<AiModel>>('/api/admin/v1/ai/models', payload)
}

export function updateAiModel(id: number, payload: AiModelForm) {
  return request.put<unknown, ApiResponse<AiModel>>(`/api/admin/v1/ai/models/${id}`, payload)
}

export function enableAiModel(id: number) {
  return request.post<unknown, ApiResponse<AiModel>>(`/api/admin/v1/ai/models/${id}/enable`)
}

export function testAiModel(id: number) {
  return request.post(`/api/admin/v1/ai/models/${id}/test`)
}

export function saveAiPrompt(id: number, payload: { systemPrompt?: string; fewShotExamplesJson?: string }) {
  return request.put<unknown, ApiResponse<AiModel>>(`/api/admin/v1/ai/models/${id}/prompt`, payload)
}

export function listConversationLogs(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<AiConversation>>>('/api/admin/v1/ai/conversation-logs', { params })
}

export function getConversationLog(id: number) {
  return request.get<unknown, ApiResponse<AiConversationDetail>>(`/api/admin/v1/ai/conversation-logs/${id}`)
}

export function markConversation(id: number, payload: { flag: string; reason?: string }) {
  return request.post<unknown, ApiResponse<AiConversationDetail>>(`/api/admin/v1/ai/conversation-logs/${id}/mark`, payload)
}

export function listRecognitionLogs(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<AiRecognition>>>('/api/admin/v1/ai/recognition-logs', { params })
}

export function getRecognitionLog(id: number) {
  return request.get<unknown, ApiResponse<AiRecognition>>(`/api/admin/v1/ai/recognition-logs/${id}`)
}

export function deleteRecognitionLog(id: number) {
  return request.delete(`/api/admin/v1/ai/recognition-logs/${id}`)
}

export function listKnowledgeDocuments(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<AiKnowledgeDocument>>>('/api/admin/v1/ai/knowledge/documents', { params })
}

export function createKnowledgeText(payload: AiKnowledgeTextPayload) {
  return request.post<unknown, ApiResponse<AiKnowledgeDocument>>('/api/admin/v1/ai/knowledge/documents/text', payload)
}

export function uploadKnowledgeDocument(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, ApiResponse<AiKnowledgeDocument>>('/api/admin/v1/ai/knowledge/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function scanKnowledgeDocuments() {
  return request.post<unknown, ApiResponse<AiKnowledgeIndexJob>>('/api/admin/v1/ai/knowledge/documents/scan')
}

export function reindexKnowledgeDocument(id: number) {
  return request.post<unknown, ApiResponse<AiKnowledgeIndexJob>>(`/api/admin/v1/ai/knowledge/documents/${id}/reindex`)
}

export function offlineKnowledgeDocument(id: number) {
  return request.put<unknown, ApiResponse<AiKnowledgeDocument>>(`/api/admin/v1/ai/knowledge/documents/${id}/offline`)
}

export function listKnowledgeJobs(params: Record<string, unknown>) {
  return request.get<unknown, ApiResponse<PageResult<AiKnowledgeIndexJob>>>('/api/admin/v1/ai/knowledge/jobs', { params })
}
