<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { chatStream, deleteConversation, getConversation, listConversations, listRecognitions, listRecommendedQuestions, recognizeFood } from '@/api/ai'
import { uploadImage } from '@/api/operation'
import type { AiChatResponse, AiConversation, AiMessage, AiRagSource, AiRecognition } from '@/types/cook'
import { requireLogin } from '@/utils/format'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'

const active = ref<'vision' | 'chat'>('vision')
const question = ref('')
const imageUrl = ref('')
const selectedImagePreview = ref('')
const HISTORY_PAGE_SIZE = 50
const TYPEWRITER_DELAY_MS = 28
const TYPEWRITER_PUNCTUATION_DELAY_MS = 85
const TYPEWRITER_LINE_BREAK_DELAY_MS = 110
type RecognitionImageSource = 'camera' | 'album'
interface ChatMessage extends AiMessage {
  streaming?: boolean
  failed?: boolean
}

interface ConversationGroup {
  key: string
  label: string
  items: AiConversation[]
}

const welcomeMessages: ChatMessage[] = [
  { id: 1, role: 'assistant', content: '你好，我是你的 AI 营养师。我可以根据目标、口味和身体状态给出饮食建议。' },
]

function createWelcomeMessages() {
  return welcomeMessages.map(item => ({ ...item }))
}

const conversations = ref<AiConversation[]>([])
const messages = ref<ChatMessage[]>(createWelcomeMessages())
const currentConversationId = ref<number>()
const recognitions = ref<AiRecognition[]>([])
const sending = ref(false)
const recognizing = ref(false)
const historyVisible = ref(false)
const historyKeyword = ref('')
const DEFAULT_QUICK_QUESTIONS = ['减脂食谱推荐', '增肌怎么吃', '糖尿病饮食']

const latestRecognition = computed(() => recognitions.value[0])
const quickQuestions = ref<string[]>([...DEFAULT_QUICK_QUESTIONS])
const filteredConversations = computed(() => {
  const keyword = historyKeyword.value.trim().toLowerCase()
  if (!keyword) return conversations.value
  return conversations.value.filter((item) => {
    return conversationTitle(item).toLowerCase().includes(keyword)
  })
})
const groupedConversations = computed<ConversationGroup[]>(() => {
  const groups: ConversationGroup[] = [
    { key: 'today', label: '今天', items: [] },
    { key: 'yesterday', label: '昨天', items: [] },
    { key: 'month', label: '30 天内', items: [] },
    { key: 'earlier', label: '更早', items: [] },
  ]
  filteredConversations.value.forEach((item) => {
    const target = groups.find(group => group.key === getConversationGroupKey(item)) || groups[3]
    target.items.push(item)
  })
  return groups.filter(group => group.items.length)
})

function hasToken() {
  return Boolean(uni.getStorageSync('cook_user_token'))
}

async function loadConversations() {
  if (!hasToken()) {
    conversations.value = []
    messages.value = createWelcomeMessages()
    recognitions.value = []
    currentConversationId.value = undefined
    return
  }
  try {
    const [conversationRes, recognitionRes] = await Promise.all([
      listConversations(1, HISTORY_PAGE_SIZE),
      listRecognitions(),
    ])
    conversations.value = conversationRes.data?.items || []
    recognitions.value = recognitionRes.data?.items || []
    if (currentConversationId.value && !conversations.value.some(item => item.conversationId === currentConversationId.value)) {
      startNewConversation(false)
    }
  }
  catch {
    conversations.value = []
    recognitions.value = []
    if (!currentConversationId.value) {
      messages.value = createWelcomeMessages()
    }
  }
}

async function loadRecommendedQuestions() {
  if (!hasToken()) {
    quickQuestions.value = [...DEFAULT_QUICK_QUESTIONS]
    return
  }
  try {
    const response = await listRecommendedQuestions()
    const items = (response.data || []).map(item => item.trim()).filter(Boolean)
    quickQuestions.value = items.length ? items.slice(0, 3) : [...DEFAULT_QUICK_QUESTIONS]
  }
  catch {
    quickQuestions.value = [...DEFAULT_QUICK_QUESTIONS]
  }
}

function switchMode(next: 'vision' | 'chat') {
  if (sending.value && next !== active.value) {
    uni.showToast({ title: 'AI 正在回复，稍后再切换', icon: 'none' })
    return
  }
  active.value = next
}

async function openConversation(id: number, force = false) {
  if (sending.value && !force) {
    uni.showToast({ title: 'AI 正在回复，稍后再切换', icon: 'none' })
    return false
  }
  if (!hasToken()) {
    return false
  }
  const previousConversationId = currentConversationId.value
  currentConversationId.value = id
  try {
    const response = await getConversation(id)
    messages.value = response.data?.messages || []
    return true
  }
  catch (error) {
    currentConversationId.value = previousConversationId
    uni.showToast({ title: error instanceof Error ? error.message : '会话加载失败', icon: 'none' })
    return false
  }
}

async function openConversationFromHistory(id: number) {
  const opened = await openConversation(id)
  if (!opened) return
  historyVisible.value = false
  scrollChatToBottom()
}

function openHistory() {
  if (sending.value) {
    uni.showToast({ title: 'AI 正在回复，稍后再查看历史', icon: 'none' })
    return
  }
  if (!requireLogin()) return
  active.value = 'chat'
  historyVisible.value = true
  loadConversations()
}

function closeHistory() {
  historyVisible.value = false
}

function startNewConversation(showToast = true) {
  if (sending.value) {
    uni.showToast({ title: 'AI 正在回复，稍后再新建', icon: 'none' })
    return
  }
  active.value = 'chat'
  currentConversationId.value = undefined
  question.value = ''
  messages.value = createWelcomeMessages()
  historyVisible.value = false
  if (showToast) {
    uni.showToast({ title: '已新建对话', icon: 'none' })
  }
}

function confirmDeleteConversation(item: AiConversation) {
  if (sending.value) {
    uni.showToast({ title: 'AI 正在回复，稍后再删除', icon: 'none' })
    return
  }
  uni.showModal({
    title: '删除会话',
    content: `确认删除“${conversationTitle(item)}”？删除后不可在历史列表中查看。`,
    confirmText: '删除',
    confirmColor: '#d35349',
    success: async ({ confirm }) => {
      if (!confirm) return
      if (sending.value) {
        uni.showToast({ title: 'AI 正在回复，稍后再删除', icon: 'none' })
        return
      }
      try {
        await deleteConversation(item.conversationId)
        if (currentConversationId.value === item.conversationId) {
          startNewConversation(false)
        }
        await loadConversations()
        uni.showToast({ title: '会话已删除', icon: 'none' })
      }
      catch (error) {
        uni.showToast({ title: error instanceof Error ? error.message : '删除失败', icon: 'none' })
      }
    },
  })
}

async function sendQuestion(nextQuestion?: string) {
  const content = (nextQuestion || question.value).trim()
  if (!content) return
  if (!requireLogin()) return
  if (sending.value) return
  sending.value = true
  question.value = ''
  const timestamp = Date.now()
  const userMessage: ChatMessage = {
    id: -timestamp,
    role: 'user',
    content,
    createdAt: new Date().toISOString(),
  }
  const assistantMessage: ChatMessage = {
    id: -timestamp - 1,
    role: 'assistant',
    content: '',
    streaming: true,
  }
  messages.value = [...messages.value, userMessage, assistantMessage]
  const userMessageIndex = messages.value.length - 2
  const assistantMessageIndex = messages.value.length - 1
  scrollChatToBottom()

  const pendingCharacters: string[] = []
  let doneResponse: AiChatResponse | undefined
  let streamErrorMessage = ''
  let streamErrorPartial = false
  let streamClosed = false
  let typewriterIdleResolved = false
  let typewriterTimer: ReturnType<typeof setTimeout> | undefined
  let resolveTypewriterIdle: (() => void) | undefined
  const typewriterIdle = new Promise<void>((resolve) => {
    resolveTypewriterIdle = resolve
  })

  function getAssistantMessage() {
    return messages.value[assistantMessageIndex]
  }

  function getAssistantContent() {
    return getAssistantMessage()?.content || ''
  }

  function patchMessage(index: number, patch: Partial<ChatMessage>) {
    const current = messages.value[index]
    if (!current) return
    messages.value[index] = { ...current, ...patch }
  }

  function patchAssistantMessage(patch: Partial<ChatMessage>) {
    patchMessage(assistantMessageIndex, patch)
  }

  function maybeResolveTypewriterIdle() {
    if (!streamClosed || pendingCharacters.length || typewriterTimer || typewriterIdleResolved) {
      return
    }
    typewriterIdleResolved = true
    resolveTypewriterIdle?.()
  }

  function scheduleTypewriter() {
    if (typewriterTimer) return
    const character = pendingCharacters.shift()
    if (!character) {
      maybeResolveTypewriterIdle()
      return
    }
    typewriterTimer = setTimeout(() => {
      typewriterTimer = undefined
      patchAssistantMessage({ content: `${getAssistantContent()}${character}` })
      scrollChatToBottom()
      scheduleTypewriter()
    }, resolveTypewriterDelay(character))
  }

  function enqueueTypewriterText(text?: string) {
    if (!text) return
    pendingCharacters.push(...splitTypewriterCharacters(text))
    scheduleTypewriter()
  }

  function enqueueMissingDoneAnswer(answer?: string) {
    if (!answer) return
    const visibleAnswer = getAssistantContent() + pendingCharacters.join('')
    if (!visibleAnswer) {
      enqueueTypewriterText(answer)
      return
    }
    if (answer.startsWith(visibleAnswer) && answer.length > visibleAnswer.length) {
      enqueueTypewriterText(answer.slice(visibleAnswer.length))
    }
  }

  function closeTypewriterStream() {
    streamClosed = true
    maybeResolveTypewriterIdle()
  }

  try {
    await chatStream(
      {
        conversationId: currentConversationId.value,
        question: content,
        conversationType: 'diet_advice',
      },
      {
        onMeta: (meta) => {
          currentConversationId.value = meta.conversationId
          patchMessage(userMessageIndex, { id: meta.userMessageId })
        },
        onDelta: (delta) => {
          enqueueTypewriterText(delta)
        },
        onDone: (done) => {
          doneResponse = done
          currentConversationId.value = done.conversationId
          enqueueMissingDoneAnswer(done.answer)
          closeTypewriterStream()
        },
        onError: (error) => {
          streamErrorMessage = error.message || 'AI 回复失败，请稍后重试'
          streamErrorPartial = Boolean(error.partial)
          closeTypewriterStream()
        },
      },
    )
    closeTypewriterStream()
    await typewriterIdle

    if (doneResponse) {
      const normalizedAnswer = doneResponse.answer && getAssistantContent() !== doneResponse.answer
        ? doneResponse.answer
        : getAssistantContent()
      patchAssistantMessage({
        id: doneResponse.assistantMessageId,
        content: normalizedAnswer,
        ragHit: doneResponse.ragHit,
        sources: doneResponse.sources || [],
        fallbackReason: doneResponse.fallbackReason,
        streaming: false,
      })
      currentConversationId.value = doneResponse.conversationId
      await Promise.all([loadConversations(), loadRecommendedQuestions()])
    }
    else if (streamErrorMessage) {
      patchAssistantMessage({
        streaming: false,
        failed: true,
        content: getAssistantContent() || streamErrorMessage,
      })
      uni.showToast({ title: streamErrorPartial ? '回复中断，已保留部分内容' : 'AI 回复失败', icon: 'none' })
    }
    else {
      patchAssistantMessage({
        streaming: false,
        failed: true,
        content: getAssistantContent() || 'AI流式响应异常结束',
      })
      uni.showToast({ title: 'AI 回复失败', icon: 'none' })
    }
  }
  catch (error) {
    closeTypewriterStream()
    await typewriterIdle
    patchAssistantMessage({
      streaming: false,
      failed: true,
      content: getAssistantContent() || (error instanceof Error ? error.message : 'AI 回复失败，请稍后重试'),
    })
    uni.showToast({ title: 'AI 回复失败', icon: 'none' })
  }
  finally {
    if (typewriterTimer) {
      clearTimeout(typewriterTimer)
    }
    sending.value = false
  }
}

function scrollChatToBottom() {
  nextTick(() => {
    uni.pageScrollTo({ scrollTop: 999999, duration: 80 })
  })
}

function conversationTitle(item: AiConversation) {
  return item.title || `会话 ${item.conversationId}`
}

function messageSources(item: ChatMessage): AiRagSource[] {
  if (item.sources?.length) return item.sources
  if (!item.ragSourcesJson) return []
  try {
    const parsed = JSON.parse(item.ragSourcesJson) as unknown
    return Array.isArray(parsed) ? parsed as AiRagSource[] : []
  }
  catch {
    return []
  }
}

function parseConversationDate(item: AiConversation) {
  const value = item.updatedAt || ''
  if (!value) return undefined
  const date = new Date(value.replace(/-/g, '/'))
  return Number.isNaN(date.getTime()) ? undefined : date
}

function getConversationGroupKey(item: AiConversation) {
  const date = parseConversationDate(item)
  if (!date) return 'earlier'
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const target = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  const diffDays = Math.floor((today.getTime() - target.getTime()) / 86400000)
  if (diffDays <= 0) return 'today'
  if (diffDays === 1) return 'yesterday'
  if (diffDays <= 30) return 'month'
  return 'earlier'
}

function splitTypewriterCharacters(content: string) {
  return Array.from(content)
}

function resolveTypewriterDelay(character: string) {
  if ("\n\r".includes(character)) {
    return TYPEWRITER_LINE_BREAK_DELAY_MS
  }
  if ("。！？!?；;，,、：:".includes(character)) {
    return TYPEWRITER_PUNCTUATION_DELAY_MS
  }
  return TYPEWRITER_DELAY_MS
}

async function runRecognition() {
  const url = imageUrl.value.trim()
  if (!url) {
    uni.showToast({ title: '请先填写图片 URL', icon: 'none' })
    return
  }
  if (!requireLogin()) return
  if (recognizing.value) return
  recognizing.value = true
  try {
    await recognizeFood({ imageUrl: url })
    imageUrl.value = ''
    selectedImagePreview.value = ''
    await loadConversations()
    uni.showToast({ title: '识别完成', icon: 'none' })
  }
  catch (error) {
    showRecognitionError(error)
  }
  finally {
    recognizing.value = false
  }
}

function chooseRecognitionImage(source: RecognitionImageSource) {
  if (!requireLogin()) return
  if (recognizing.value) return
  uni.chooseImage({
    count: 1,
    sourceType: [source],
    sizeType: ['compressed'],
    success: async ({ tempFilePaths }) => {
      const [filePath] = normalizeTempFilePaths(tempFilePaths)
      if (!filePath) return
      await recognizeLocalImage(filePath)
    },
    fail: (error) => {
      if (isChooseImageCancel(error)) return
      uni.showToast({
        title: source === 'camera' ? '拍照失败，请重试' : '选择图片失败，请重试',
        icon: 'none',
      })
    },
  })
}

async function recognizeLocalImage(filePath: string) {
  if (recognizing.value) return
  recognizing.value = true
  selectedImagePreview.value = filePath
  try {
    const response = await uploadImage(filePath)
    if (!response.data?.id) {
      throw new Error('上传图片失败')
    }
    const uploadedUrl = resolveAssetUrl(response.data.url || '') || mediaIdToRawUrl(response.data.id)
    if (!uploadedUrl) {
      throw new Error('图片地址为空')
    }
    await recognizeFood({
      imageMediaId: response.data.id,
      imageUrl: uploadedUrl,
    })
    imageUrl.value = ''
    await loadConversations()
    uni.showToast({ title: '识别完成', icon: 'none' })
  }
  catch (error) {
    showRecognitionError(error)
  }
  finally {
    recognizing.value = false
  }
}

function normalizeTempFilePaths(value?: string[] | string) {
  if (Array.isArray(value)) return value
  return value ? [value] : []
}

function isChooseImageCancel(error: unknown) {
  const errMsg = String((error as { errMsg?: string } | undefined)?.errMsg || '').toLowerCase()
  return errMsg.includes('cancel')
}

function showRecognitionError(error: unknown) {
  const message = error instanceof Error ? error.message : ''
  if (message === 'Upload failed' || message === 'Request failed' || message === 'Unauthorized') return
  uni.showToast({ title: message || '识别失败，请重试', icon: 'none' })
}

function nutritionValue(key: string, aliases: string[] = []) {
  const data = latestRecognition.value?.nutrition || {}
  const value = [key, ...aliases].map((item) => data[item]).find((item) => item !== undefined && item !== null)
  if (value === undefined || value === null || value === '') return '-'
  if (typeof value === 'number' || typeof value === 'string') return String(value)
  if (typeof value === 'object') {
    const record = value as Record<string, unknown>
    const grams = record.grams ?? record.g
    if (typeof grams === 'number' || typeof grams === 'string') return `${grams}g`
    const percent = record.percent
    if (typeof percent === 'number' || typeof percent === 'string') return `${percent}%`
  }
  return '-'
}

onMounted(() => {
  loadConversations()
  loadRecommendedQuestions()
})
</script>

<template>
  <view class="page-body ai-page">
    <view class="app-page-head">
      <view class="app-page-head__row">
        <view class="ai-head-main">
          <view v-if="active === 'chat'" class="ai-head-icon" :class="{ disabled: sending }" @click="openHistory">☰</view>
          <view>
            <view class="section-title">🤖 AI营养师</view>
            <view class="section-caption">识别热量、分析营养，也能聊饮食计划</view>
          </view>
        </view>
        <view v-if="active === 'chat'" class="ai-head-actions">
          <view class="ai-head-icon" :class="{ disabled: sending }" @click="startNewConversation()">＋</view>
        </view>
      </view>
    </view>

    <view class="segmented-tabs ai-tabs">
      <view class="segmented-tab" :class="{ active: active === 'vision', disabled: sending && active !== 'vision' }" @click="switchMode('vision')">📸 拍照识热量</view>
      <view class="segmented-tab" :class="{ active: active === 'chat', disabled: sending && active !== 'chat' }" @click="switchMode('chat')">💬 智能问答</view>
    </view>

    <view v-if="active === 'vision'" class="ai-panel">
      <view class="photo-section surface-card">
        <view class="photo-area">
          <image v-if="selectedImagePreview" class="photo-preview" :src="selectedImagePreview" mode="aspectFill" />
          <template v-else>
            <view class="camera-icon">⌘</view>
            <view class="photo-title">拍照或上传美食图片</view>
            <view class="photo-desc">AI 智能识别热量与营养结构</view>
          </template>
        </view>
        <view class="photo-actions">
          <button class="primary-button" :disabled="recognizing" @click="chooseRecognitionImage('camera')">{{ recognizing ? '识别中...' : '拍照识别' }}</button>
          <button class="ghost-button" :disabled="recognizing" @click="chooseRecognitionImage('album')">从相册选择</button>
        </view>
        <input
          v-model="imageUrl"
          class="form-control image-input"
          :disabled="recognizing"
          confirm-type="send"
          placeholder="本地 H5 可先填写可访问图片 URL"
          @confirm="runRecognition"
        />
      </view>

      <view v-if="latestRecognition" class="result-card surface-card">
        <view class="dish-name">🦐 {{ latestRecognition.recognizedName || '识别结果' }}</view>
        <view class="calorie">
          <text class="num">{{ latestRecognition.calories || 0 }}</text>
          <text class="unit">千卡 / 份</text>
        </view>
        <view class="nutrition-list">
          <view class="nutri-item">
            <text class="label">蛋白质</text>
            <view class="bar"><view class="fill protein" /></view>
            <text class="value">{{ nutritionValue('protein') }}</text>
          </view>
          <view class="nutri-item">
            <text class="label">脂肪</text>
            <view class="bar"><view class="fill fat" /></view>
            <text class="value">{{ nutritionValue('fat') }}</text>
          </view>
          <view class="nutri-item">
            <text class="label">碳水</text>
            <view class="bar"><view class="fill carb" /></view>
            <text class="value">{{ nutritionValue('carb', ['carbohydrate', 'carbs']) }}</text>
          </view>
          <view class="nutri-item">
            <text class="label">纤维</text>
            <view class="bar"><view class="fill fiber" /></view>
            <text class="value">{{ nutritionValue('fiber') }}</text>
          </view>
        </view>
        <view class="suggestion">{{ latestRecognition.suggestion || '建议结合食材分量和个人目标综合判断。' }}</view>
      </view>
      <EmptyState
        v-else
        :title="hasToken() ? '暂无识别记录' : '登录后查看识别记录'"
        :description="hasToken() ? '上传一张食物图片后，AI 会给出热量和营养建议。' : '识别结果和历史会按账号保存。'"
      />
      <view class="disclaimer">识别结果仅供参考，实际热量可能因食材和份量而异</view>
    </view>

    <view v-else class="chat-panel">
      <view class="chat-messages">
        <view
          v-for="item in messages"
          :key="item.id"
          class="msg"
          :class="[item.role === 'assistant' ? 'ai' : 'user', { streaming: item.streaming, failed: item.failed }]"
        >
          <view v-if="item.role === 'assistant'" class="ai-label">AI营养师</view>
          <view v-if="item.role === 'assistant' && item.streaming && !item.content" class="typing-dots">
            <view class="typing-dot" />
            <view class="typing-dot" />
            <view class="typing-dot" />
          </view>
          <text v-else>{{ item.content }}</text>
          <view v-if="item.role === 'assistant' && messageSources(item).length" class="rag-sources">
            <view class="rag-sources__title">参考来源</view>
            <view
              v-for="source in messageSources(item)"
              :key="`${source.documentId || 0}-${source.chunkId || 0}-${source.fileName || ''}`"
              class="rag-source"
            >
              <text class="rag-source__file">{{ source.title || source.fileName || '本地知识库' }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="chat-input-stack">
        <view class="quick-questions">
          <view
            v-for="item in quickQuestions"
            :key="item"
            class="quick-q"
            :class="{ disabled: sending }"
            @click="sendQuestion(item)"
          >
            {{ item }}
          </view>
        </view>
      </view>
      <view class="chat-input-bar">
        <input
          v-model="question"
          class="chat-input"
          :disabled="sending"
          :placeholder="sending ? 'AI 正在回复...' : '输入你的饮食问题...'"
          @confirm="sendQuestion()"
        />
        <button class="send-btn" :disabled="sending" @click="sendQuestion()">{{ sending ? '…' : '➤' }}</button>
      </view>
    </view>

    <view v-if="historyVisible" class="history-mask" @click="closeHistory" />
    <view class="history-drawer" :class="{ open: historyVisible }">
      <view class="history-drawer__head">
        <view>
          <view class="history-title">历史会话</view>
          <view class="history-caption">点击会话切换上下文</view>
        </view>
        <view class="history-close" @click="closeHistory">×</view>
      </view>
      <view class="history-search">
        <text class="history-search__icon">⌕</text>
        <input v-model="historyKeyword" placeholder="搜索会话标题..." />
      </view>
      <scroll-view scroll-y class="history-scroll">
        <template v-if="groupedConversations.length">
          <view v-for="group in groupedConversations" :key="group.key" class="history-group">
            <view class="history-group__label">{{ group.label }}</view>
            <view
              v-for="item in group.items"
              :key="item.conversationId"
              class="history-item"
              :class="{ active: item.conversationId === currentConversationId }"
              @click="openConversationFromHistory(item.conversationId)"
            >
              <view class="history-item__main">
                <view class="history-item__title">{{ conversationTitle(item) }}</view>
              </view>
              <view class="history-item__delete" :class="{ disabled: sending }" @click.stop="confirmDeleteConversation(item)">删除</view>
            </view>
          </view>
        </template>
        <EmptyState
          v-else
          :title="hasToken() ? '还没有历史会话' : '登录后查看历史会话'"
          :description="hasToken() ? '发起一次 AI 问答后，会自动保存到这里。' : '问答记录会随账号同步。'"
        />
      </scroll-view>
    </view>

    <AppTabBar current="ai" />
  </view>
</template>

<style scoped lang="scss">
.ai-page {
  display: flex;
  flex-direction: column;
}

.ai-tabs {
  position: sticky;
  top: 96rpx;
  z-index: 20;
  background: rgba(255, 252, 248, 0.9);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.ai-panel {
  display: grid;
  gap: 20rpx;
  padding: 24rpx 28rpx 28rpx;
}

.photo-section {
  padding: 30rpx;
}

.photo-area {
  min-height: 320rpx;
  border: 2rpx dashed rgba(232, 109, 47, 0.28);
  border-radius: 28rpx;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--app-surface-muted);
  text-align: center;
}

.photo-preview {
  width: 100%;
  height: 320rpx;
}

.camera-icon {
  width: 104rpx;
  height: 104rpx;
  border-radius: 34rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 54rpx;
  font-weight: 700;
}

.photo-title {
  margin-top: 22rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--app-text);
}

.photo-desc {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: var(--app-text-muted);
}

.photo-actions {
  display: flex;
  gap: 18rpx;
  margin-top: 24rpx;
}

.photo-actions button {
  flex: 1;
}

.photo-actions button[disabled] {
  opacity: 0.72;
}

.image-input {
  margin-top: 18rpx;
}

.result-card {
  padding: 28rpx;
}

.dish-name {
  font-size: 32rpx;
  font-weight: 800;
  color: var(--app-text);
}

.calorie {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-top: 18rpx;
}

.num {
  font-size: 64rpx;
  font-weight: 900;
  color: var(--app-primary);
}

.unit {
  color: var(--app-text-muted);
  font-size: 24rpx;
}

.nutrition-list {
  display: grid;
  gap: 16rpx;
  margin-top: 20rpx;
}

.nutri-item {
  display: flex;
  align-items: center;
  gap: 14rpx;
  font-size: 24rpx;
}

.label {
  width: 86rpx;
  color: var(--app-text-soft);
}

.bar {
  flex: 1;
  height: 14rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: #eef0f3;
}

.fill {
  height: 100%;
  border-radius: 999rpx;
}

.protein { width: 65%; background: linear-gradient(90deg, #e86d2f, #ff9b54); }
.fat { width: 35%; background: linear-gradient(90deg, #b77822, #ffd666); }
.carb { width: 45%; background: linear-gradient(90deg, #4477da, #69c0ff); }
.fiber { width: 20%; background: linear-gradient(90deg, #2d8768, #95de64); }

.value {
  width: 78rpx;
  text-align: right;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.suggestion {
  margin-top: 22rpx;
  padding: 18rpx;
  border-radius: 20rpx;
  background: var(--app-success-soft);
  color: var(--app-success);
  font-size: 24rpx;
  line-height: 1.65;
}

.disclaimer {
  color: var(--app-text-muted);
  text-align: center;
  font-size: 22rpx;
}

.chat-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding: 24rpx 28rpx 230rpx;
}

.msg {
  max-width: 82%;
  padding: 20rpx 24rpx;
  border-radius: 28rpx;
  font-size: 26rpx;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg.ai {
  align-self: flex-start;
  background: #fff;
  border-bottom-left-radius: 8rpx;
  box-shadow: var(--app-shadow-sm);
}

.msg.user {
  align-self: flex-end;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  border-bottom-right-radius: 8rpx;
}

.msg.streaming {
  opacity: 0.86;
}

.typing-dots {
  min-width: 76rpx;
  height: 34rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.typing-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: var(--app-primary);
  opacity: 0.35;
  animation: typing-bounce 1.05s ease-in-out infinite;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes typing-bounce {
  0%, 80%, 100% {
    opacity: 0.35;
    transform: translateY(0) scale(0.78);
  }
  40% {
    opacity: 1;
    transform: translateY(-5rpx) scale(1);
  }
}

.msg.failed {
  border: 1rpx solid rgba(211, 83, 73, 0.24);
}

.rag-sources {
  display: grid;
  gap: 10rpx;
  margin-top: 16rpx;
  padding-top: 14rpx;
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.rag-sources__title {
  color: var(--app-text-soft);
  font-size: 21rpx;
  font-weight: 700;
}

.rag-source {
  display: grid;
  gap: 6rpx;
  padding: 12rpx 14rpx;
  border-radius: 16rpx;
  background: var(--app-surface-muted);
}

.rag-source__file {
  color: var(--app-text);
  font-size: 22rpx;
  font-weight: 700;
}

.ai-label {
  margin-bottom: 8rpx;
  font-size: 21rpx;
  color: var(--app-text-muted);
}

.chat-input-stack {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(202rpx + env(safe-area-inset-bottom));
  z-index: 59;
  max-width: 860rpx;
  margin: 0 auto;
  padding: 0 28rpx;
  background: rgba(255, 252, 248, 0.92);
  backdrop-filter: blur(18rpx);
}

.quick-questions {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  padding: 12rpx 0 14rpx;
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.quick-q {
  padding: 10rpx 18rpx;
  border: 1rpx solid rgba(232, 109, 47, 0.34);
  border-radius: 999rpx;
  color: var(--app-primary);
  background: #fff;
  font-size: 23rpx;
}

.quick-q.disabled {
  opacity: 0.5;
}

.chat-input-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(112rpx + env(safe-area-inset-bottom));
  z-index: 60;
  max-width: 860rpx;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 14rpx 28rpx;
  background: rgba(255, 252, 248, 0.92);
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.chat-input {
  flex: 1;
  min-height: 72rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: var(--app-surface-muted);
  font-size: 25rpx;
}

.send-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 28rpx;
  line-height: 72rpx;
}

.send-btn[disabled] {
  opacity: 0.72;
}

.segmented-tab.disabled {
  opacity: 0.5;
}

.history-mask {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(15, 23, 42, 0.28);
  backdrop-filter: blur(4rpx);
}

.history-drawer {
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  width: min(620rpx, 86vw);
  padding: calc(30rpx + env(safe-area-inset-top)) 24rpx calc(30rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  background: rgba(255, 252, 248, 0.98);
  box-shadow: 18rpx 0 44rpx rgba(15, 23, 42, 0.16);
  transform: translateX(-104%);
  transition: transform 0.22s ease;
}

.history-drawer.open {
  transform: translateX(0);
}

.history-drawer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.history-title {
  color: var(--app-text);
  font-size: 32rpx;
  font-weight: 800;
}

.history-caption {
  margin-top: 6rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.history-close {
  flex: 0 0 64rpx;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--app-text-muted);
  background: var(--app-surface-muted);
  font-size: 38rpx;
  line-height: 1;
}

.history-search {
  margin-top: 24rpx;
  height: 72rpx;
  padding: 0 22rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  border-radius: 22rpx;
  background: var(--app-surface-muted);
  border: 1rpx solid rgba(15, 23, 42, 0.06);
}

.history-search__icon {
  color: var(--app-text-soft);
  font-size: 28rpx;
}

.history-search input {
  flex: 1;
  min-width: 0;
  font-size: 24rpx;
}

.history-scroll {
  flex: 1;
  min-height: 0;
  height: calc(100vh - 220rpx - env(safe-area-inset-top) - env(safe-area-inset-bottom));
  margin-top: 20rpx;
}

.history-group {
  display: grid;
  gap: 12rpx;
  padding-bottom: 22rpx;
}

.history-group__label {
  color: var(--app-text-soft);
  font-size: 21rpx;
  font-weight: 700;
}

.history-item {
  min-height: 92rpx;
  padding: 18rpx;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  border-radius: 22rpx;
  background: #fff;
  border: 1rpx solid rgba(15, 23, 42, 0.06);
  box-shadow: var(--app-shadow-sm);
}

.history-item.active {
  border-color: rgba(232, 109, 47, 0.44);
  background: rgba(232, 109, 47, 0.08);
}

.history-item__main {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
  overflow: hidden;
}

.history-item__title {
  display: block;
  max-width: 100%;
  color: var(--app-text);
  font-size: 25rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item__delete {
  flex: 0 0 auto;
  min-width: 92rpx;
  padding: 10rpx 18rpx;
  box-sizing: border-box;
  border-radius: 999rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--app-danger);
  background: var(--app-danger-soft);
  border: 1rpx solid rgba(211, 83, 73, 0.22);
  font-size: 22rpx;
  font-weight: 700;
  line-height: 1.2;
  text-align: center;
}

.history-item__delete.disabled {
  opacity: 0.48;
}
</style>
