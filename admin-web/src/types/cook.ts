import type { PageResult } from './common'

export interface AiModel {
  id: number
  name: string
  modelType: 'chat' | 'vision' | string
  provider: string
  modelCode: string
  apiBaseUrl?: string
  configJson?: string
  systemPrompt?: string
  isDefault: boolean
  status: 'enabled' | 'disabled' | string
  lastTestStatus?: string
  lastTestLatencyMs?: number
  lastTestMessage?: string
  createdAt?: string
  updatedAt?: string
}

export interface AiModelForm {
  name: string
  modelType: string
  provider: string
  modelCode: string
  encryptedApiKey?: string
  apiBaseUrl?: string
  configJson?: string
  systemPrompt?: string
  isDefault?: boolean
  status?: string
}

export interface AiConversation {
  conversationId: number
  userId: number
  userNickname?: string
  userAvatarUrl?: string
  modelId?: number
  modelName?: string
  modelType?: string
  title?: string
  lastMessage?: string
  rounds?: number
  inputTokens?: number
  outputTokens?: number
  totalTokens?: number
  responseTimeMs?: number
  ragHit?: boolean
  flag?: string
  flagReason?: string
  createdAt?: string
  updatedAt?: string
}

export interface AiMessage {
  id: number
  conversationId: number
  userId?: number
  userNickname?: string
  userAvatarUrl?: string
  modelId?: number
  modelName?: string
  role: string
  content: string
  inputTokens?: number
  outputTokens?: number
  responseTimeMs?: number
  ragHit?: boolean
  ragSourcesJson?: string
  fallbackReason?: string
  flag?: string
  flagReason?: string
  createdAt?: string
}

export interface AiConversationDetail {
  summary: AiConversation
  messages: AiMessage[]
}

export interface AiRecognition {
  id: number
  userId?: number
  userNickname?: string
  userAvatarUrl?: string
  modelId?: number
  modelName?: string
  imageMediaId?: number
  imageUrl?: string
  status?: string
  recognizedName?: string
  confidence?: number
  calories?: number
  nutrition?: Record<string, unknown>
  suggestion?: string
  candidates?: unknown
  latencyMs?: number
  errorMessage?: string
  disclaimer?: string
  createdAt?: string
}

export interface AiRagSource {
  documentId?: number
  chunkId?: number
  fileName?: string
  title?: string
  score?: number
  snippet?: string
}

export interface AiKnowledgeDocument {
  id: number
  fileName: string
  originalName?: string
  relativePath: string
  fileHash?: string
  fileSize?: number
  fileType?: string
  title?: string
  status?: string
  chunkCount?: number
  lastIndexedAt?: string
  errorMessage?: string
  createdAt?: string
  updatedAt?: string
}

export interface AiKnowledgeIndexJob {
  id: number
  documentId?: number
  jobType?: string
  status?: string
  totalDocuments?: number
  indexedDocuments?: number
  failedDocuments?: number
  message?: string
  startedAt?: string
  finishedAt?: string
  createdAt?: string
}

export interface AiKnowledgeTextPayload {
  fileName: string
  content: string
}

export interface RecipeListItem {
  id: number
  authorId?: number
  authorNickname?: string
  versionId?: number
  versionNo?: number
  title: string
  coverMediaId?: number
  intro?: string
  categoryCode?: string
  difficulty?: string
  cookTime?: string
  serving?: string
  reviewStatus?: string
  publishStatus?: string
  versionStatus?: string
  rejectReason?: string
  likeCount?: number
  favoriteCount?: number
  commentCount?: number
  liked?: boolean
  favorited?: boolean
  createdAt?: string
  updatedAt?: string
}

export interface RecipeDetail extends RecipeListItem {
  currentVersionId?: number
  ingredients?: Array<Record<string, unknown>>
  steps?: Array<Record<string, unknown>>
  tips?: Array<Record<string, unknown>>
  video?: RecipeVideo
}

export interface RecipeVideo {
  mediaId?: number
  url?: string
  playUrl?: string
  hlsUrl?: string
  m3u8Url?: string
  coverUrl?: string
  posterUrl?: string
  originalName?: string
  status?: string
  duration?: string | number
  durationText?: string
  currentTime?: string
  label?: string
  errorMessage?: string
  sizeBytes?: number
}

export interface Category {
  id?: number
  code: string
  name: string
  icon?: string
  color?: string
  description?: string
  groupCode?: string
  sortNo?: number
  status?: string
  readonly?: boolean
  recipeCount?: number
}

export interface CategoryGroup {
  code: string
  name: string
  children: Category[]
}

export interface PostItem {
  id: number
  userId?: number
  nickname?: string
  avatarUrl?: string
  content?: string
  visibility?: string
  mediaIds?: number[]
  topicCodes?: string[]
  location?: string
  relatedRecipeId?: number
  sourceType?: string
  status?: string
  rejectReason?: string
  blockReason?: string
  blockAction?: string
  likeCount?: number
  favoriteCount?: number
  commentCount?: number
  liked?: boolean
  favorited?: boolean
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface PageState<T> extends PageResult<T> {}

export interface AdminDashboardSummary {
  totalUsers: number
  totalRecipes: number
  totalPosts: number
  pendingRecipeCount: number
  pendingPostCount: number
  processingFeedbackCount: number
  pendingReportCount: number
  onlineBannerCount: number
}

export interface TrendPoint {
  date: string
  value: number
}

export interface CategoryRatio {
  categoryCode: string
  categoryName: string
  count: number
}

export interface RecentOperationLog {
  id: number
  adminId?: number
  bizType?: string
  bizId?: number
  action?: string
  remark?: string
  createdAt?: string
}

export interface BannerItem {
  id: number
  title: string
  subtitle?: string
  imageMediaId?: number
  imageUrl?: string
  jumpType?: string
  jumpTarget?: string
  sortNo?: number
  status?: string
  clickCount?: number
  startAt?: string
  endAt?: string
  createdAt?: string
}

export interface MediaAsset {
  id: number
  ownerId?: number
  fileType?: string
  originalName?: string
  url?: string
  hlsUrl?: string
  status?: string
  sizeBytes?: number
  metadata?: Record<string, unknown>
  createdAt?: string
}

export interface AdminUser {
  id: number
  phone?: string
  nickname?: string
  avatarUrl?: string
  gender?: string
  region?: string
  bio?: string
  status?: string
  recipeCount?: number
  postCount?: number
  followerCount?: number
  createdAt?: string
}

export interface AdminGroup {
  id: number
  ownerId?: number
  ownerNickname?: string
  conversationId?: number
  name?: string
  avatarMediaId?: number
  avatarUrl?: string
  intro?: string
  notice?: string
  status?: string
  memberCount?: number
  messageCount?: number
  createdAt?: string
}

export interface FeedbackItem {
  id: number
  userId?: number
  userNickname?: string
  type?: string
  content?: string
  mediaIds?: number[]
  contact?: string
  status?: string
  replyContent?: string
  repliedAt?: string
  createdAt?: string
}
