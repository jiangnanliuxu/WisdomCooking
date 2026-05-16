import { getUserToken } from '@/utils/auth'
import { composeApiUrl, request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { AiChatResponse, AiConversation, AiConversationDetail, AiRecognition } from '@/types/cook'

export function chat(data: { conversationId?: number; question: string; conversationType?: string }) {
  return request<AiChatResponse>({ url: '/api/v1/ai/chat', method: 'POST', data })
}

export interface AiChatStreamMeta {
  conversationId: number
  userMessageId: number
  modelId?: number
  modelName?: string
}

export interface AiChatStreamError {
  message?: string
  assistantMessageId?: number
  partial?: boolean
}

export interface AiChatStreamHandlers {
  onMeta?: (data: AiChatStreamMeta) => void
  onDelta?: (content: string) => void
  onDone?: (data: AiChatResponse) => void
  onError?: (data: AiChatStreamError) => void
}

interface SseMessage {
  event: string
  data: string
}

interface StreamErrorPayload {
  code?: number
  msg?: string
  message?: string
  error?: string
}

export async function chatStream(
  data: { conversationId?: number; question: string; conversationType?: string },
  handlers: AiChatStreamHandlers,
) {
  const response = await fetch(composeApiUrl('/api/v1/ai/chat/stream'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(getUserToken() ? { Authorization: `Bearer ${getUserToken()}` } : {}),
    },
    body: JSON.stringify(data),
  })

  const contentType = response.headers.get('content-type') || ''
  if (!response.ok || !contentType.toLowerCase().includes('text/event-stream')) {
    throw new Error(await readStreamError(response))
  }
  if (!response.body) {
    throw new Error('当前浏览器不支持流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let streamEnded = false

  function handleMessage(message: SseMessage) {
    if (!message.data) return
    const payload = JSON.parse(message.data)
    if (message.event === 'meta') {
      handlers.onMeta?.(payload as AiChatStreamMeta)
      return
    }
    if (message.event === 'delta') {
      handlers.onDelta?.(String(payload.content || ''))
      return
    }
    if (message.event === 'done') {
      streamEnded = true
      handlers.onDone?.(payload as AiChatResponse)
      return
    }
    if (message.event === 'error') {
      streamEnded = true
      handlers.onError?.(payload as AiChatStreamError)
    }
  }

  function flushBuffer(final = false) {
    buffer = buffer.replace(/\r\n/g, '\n')
    const chunks = buffer.split('\n\n')
    buffer = final ? '' : chunks.pop() || ''
    chunks.forEach((chunk) => {
      const message = parseSseMessage(chunk)
      if (message) handleMessage(message)
    })
    if (final && buffer.trim()) {
      const message = parseSseMessage(buffer)
      if (message) handleMessage(message)
    }
  }

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      buffer += decoder.decode()
      flushBuffer(true)
      break
    }
    buffer += decoder.decode(value, { stream: true })
    flushBuffer()
  }

  if (!streamEnded) {
    throw new Error('AI流式响应异常结束')
  }
}

function parseSseMessage(chunk: string): SseMessage | null {
  const lines = chunk.split('\n')
  let event = 'message'
  const dataLines: string[] = []
  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      event = line.slice('event:'.length).trim()
    }
    else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart())
    }
  })
  if (!dataLines.length) return null
  return { event, data: dataLines.join('\n') }
}

async function readStreamError(response: Response) {
  const text = await response.text()
  if (!text) {
    return `AI流式请求失败：${response.status}`
  }
  try {
    const payload = JSON.parse(text) as StreamErrorPayload
    return payload.msg || payload.message || payload.error || text
  }
  catch {
    return text
  }
}

export function listConversations(page = 1, pageSize = 20) {
  return request<PageResult<AiConversation>>({ url: `/api/v1/ai/conversations?page=${page}&pageSize=${pageSize}`, method: 'GET' })
}

export function getConversation(id: number) {
  return request<AiConversationDetail>({ url: `/api/v1/ai/conversations/${id}`, method: 'GET' })
}

export function deleteConversation(id: number) {
  return request({ url: `/api/v1/ai/conversations/${id}`, method: 'DELETE' })
}

export function listRecommendedQuestions() {
  return request<string[]>({ url: '/api/v1/ai/recommended-questions', method: 'GET' })
}

export function recognizeFood(data: { imageUrl: string; imageMediaId?: number }) {
  return request<AiRecognition>({ url: '/api/v1/ai/recognize-food', method: 'POST', data })
}

export function listRecognitions(status = '', page = 1, pageSize = 20) {
  return request<PageResult<AiRecognition>>({ url: `/api/v1/ai/recognitions?status=${status}&page=${page}&pageSize=${pageSize}`, method: 'GET' })
}
