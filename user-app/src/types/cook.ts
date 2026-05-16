export interface UserProfile {
  id: number
  phone?: string
  nickname?: string
  avatarUrl?: string
  gender?: string
  birthday?: string
  region?: string
  bio?: string
  status?: string
  interestTags?: string[]
  statsJson?: string
}

export interface AuthTokenVo {
  accessToken: string
  expiresIn: number
  user: UserProfile
}

export interface CategoryGroup {
  code: string
  name: string
  children: Array<{
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
  }>
}

export interface BannerItem {
  id: number
  title?: string
  subtitle?: string
  imageMediaId?: number
  imageUrl?: string
  jumpType?: string
  jumpTarget?: string
  sortNo?: number
  status?: string
}

export interface RecipeListItem {
  id: number
  authorId?: number
  authorNickname?: string
  coverMediaId?: number
  title: string
  intro?: string
  categoryCode?: string
  difficulty?: string
  cookTime?: string
  serving?: string
  reviewStatus?: string
  publishStatus?: string
  rejectReason?: string
  likeCount?: number
  favoriteCount?: number
  commentCount?: number
  liked?: boolean
  favorited?: boolean
  authorFollowed?: boolean
  updatedAt?: string
}

export interface RecipeDetail extends RecipeListItem {
  currentVersionId?: number
  versionId?: number
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

export interface CommentItem {
  id: number
  targetType?: string
  targetId?: number
  userId?: number
  nickname?: string
  avatarUrl?: string
  parentId?: number
  content?: string
  likeCount?: number
  liked?: boolean
  createdAt?: string
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
  relatedRecipeTitle?: string
  relatedRecipeCategoryCode?: string
  sourceType?: string
  status?: string
  rejectReason?: string
  blockReason?: string
  likeCount?: number
  favoriteCount?: number
  commentCount?: number
  liked?: boolean
  favorited?: boolean
  authorFollowed?: boolean
  createdAt?: string
  publishedAt?: string
}

export interface TopicItem {
  code: string
  name: string
}

export interface AiChatResponse {
  conversationId: number
  userMessageId: number
  assistantMessageId: number
  modelId?: number
  modelName?: string
  answer: string
  ragHit?: boolean
  sources?: AiRagSource[]
  fallbackReason?: string
  inputTokens?: number
  outputTokens?: number
  latencyMs?: number
  flag?: string
  disclaimer?: string
}

export interface AiRagSource {
  documentId?: number
  chunkId?: number
  fileName?: string
  title?: string
  score?: number
  snippet?: string
}

export interface AiConversation {
  conversationId: number
  title?: string
  lastMessage?: string
  modelName?: string
  updatedAt?: string
}

export interface AiMessage {
  id: number
  role: string
  content: string
  ragHit?: boolean
  ragSourcesJson?: string
  fallbackReason?: string
  sources?: AiRagSource[]
  createdAt?: string
}

export interface AiConversationDetail {
  summary: AiConversation
  messages: AiMessage[]
}

export interface AiRecognition {
  id: number
  imageUrl?: string
  status?: string
  recognizedName?: string
  confidence?: number
  calories?: number
  nutrition?: Record<string, unknown>
  suggestion?: string
  disclaimer?: string
  createdAt?: string
}

export interface ConversationItem {
  id: number
  type?: string
  targetId?: number
  title?: string
  avatarUrl?: string
  lastMessagePreview?: string
  unreadCount?: number
  lastMessageAt?: string
  muted?: boolean
  pinned?: boolean
}

export interface MessageItem {
  id: number
  conversationId?: number
  senderId?: number
  senderNickname?: string
  senderAvatarUrl?: string
  messageType?: string
  content?: string
  mediaUrl?: string
  createdAt?: string
}

export interface GroupVo {
  id: number
  ownerId?: number
  ownerNickname?: string
  conversationId?: number
  name?: string
  avatarMediaId?: number
  intro?: string
  notice?: string
  status?: string
  memberCount?: number
  messageCount?: number
  createdAt?: string
}

export interface GroupMember {
  userId: number
  nickname?: string
  avatarUrl?: string
  role?: string
  status?: string
}

export interface UserPublicProfile {
  id: number
  nickname?: string
  avatarUrl?: string
  bio?: string
  gender?: string
  region?: string
  status?: string
  recipeCount?: number
  postCount?: number
  followerCount?: number
  followingCount?: number
  followed?: boolean
  createdAt?: string
}

export interface UserInteraction {
  targetType?: string
  targetId?: number
  actionType?: string
  title?: string
  content?: string
  coverUrl?: string
  authorNickname?: string
  interactedAt?: string
}

export interface HomeData {
  banners: BannerItem[]
  categories: CategoryGroup[]
  recommendedRecipes: RecipeListItem[]
  recommendedUsers: UserPublicProfile[]
  hotKeywords: string[]
}

export interface SearchResult {
  keyword?: string
  recipes?: {
    page: number
    pageSize: number
    total: number
    items: RecipeListItem[]
  }
  users?: UserPublicProfile[]
  hotKeywords?: string[]
}

export interface CheckinSummary {
  totalCount?: number
  monthCount?: number
  streakDays?: number
  latestCheckinDate?: string
}

export interface CheckinItem {
  id: number
  userId?: number
  recipeId?: number
  recipeTitle?: string
  generatedPostId?: number
  checkinDate?: string
  content?: string
  mediaIds?: number[]
  source?: Record<string, unknown>
  createdAt?: string
}

export interface FeedbackItem {
  id: number
  type?: string
  content?: string
  contact?: string
  mediaIds?: number[]
  status?: string
  replyContent?: string
  repliedAt?: string
  userNickname?: string
  createdAt?: string
  updatedAt?: string
}

export interface ReportItem {
  id: number
  targetType?: string
  targetId?: number
  reasonType?: string
  reasonDetail?: string
  status?: string
  resultRemark?: string
  createdAt?: string
  updatedAt?: string
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

export interface OssStsToken {
  accessKeyId: string
  accessKeySecret: string
  securityToken: string
  expiration: string
  region: string
  endpoint: string
  bucket: string
}

export interface VideoMultipartInit {
  sessionId: string
  objectKey: string
  partSize: number
  checkpointKey: string
  expiresAt: string
  sts: OssStsToken
  status?: string
  media?: MediaAsset
}

export interface VideoMultipartSession {
  sessionId: string
  objectKey: string
  originalName?: string
  sizeBytes?: number
  fingerprint?: string
  status?: string
  mediaId?: number
  errorMessage?: string
  expiresAt?: string
  createdAt?: string
  updatedAt?: string
  media?: MediaAsset
}
