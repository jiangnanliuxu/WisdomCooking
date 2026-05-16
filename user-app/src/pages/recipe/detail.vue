<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import HlsVideoPlayer from '@/components/HlsVideoPlayer.vue'
import { createComment, likeComment, listComments, unlikeComment } from '@/api/comment'
import { createPrivateConversation } from '@/api/message'
import { createReport } from '@/api/operation'
import { favoriteRecipe, getRecipe, likeRecipe, shareRecipe, unfavoriteRecipe, unlikeRecipe } from '@/api/recipe'
import { followUser, unfollowUser } from '@/api/user'
import type { CommentItem, RecipeDetail } from '@/types/cook'
import { useAuthStore } from '@/stores/auth'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'
import { formatTime, requireLogin } from '@/utils/format'

const recipeId = ref(0)
const detail = ref<RecipeDetail>()
const comments = ref<CommentItem[]>([])
const commentText = ref('')
const showAllComments = ref(false)
const authStore = useAuthStore()

const gradients = [
  'linear-gradient(135deg,#ff9a9e,#fecfef)',
  'linear-gradient(135deg,#ffecd2,#fcb69f)',
  'linear-gradient(135deg,#a1c4fd,#c2e9fb)',
  'linear-gradient(135deg,#84fab0,#8fd3f4)',
]

const stepIcons = ['🦐', '🍝', '🧄', '♨️']
const commentPreviewCount = 3

const authorName = computed(() => detail.value?.authorNickname || '匿名作者')
const commentTotal = computed(() => detail.value?.commentCount || comments.value.length || 0)
const canFollowAuthor = computed(() => Boolean(detail.value?.authorId && detail.value.authorId !== authStore.profile?.id))

const videoData = computed(() => detail.value?.video || {})
const coverSrc = computed(() => mediaIdToRawUrl(detail.value?.coverMediaId))
const videoHlsSrc = computed(() => resolveAssetUrl(String(videoData.value.hlsUrl || videoData.value.m3u8Url || '')))
const videoMp4Src = computed(() => {
  const directUrl = resolveAssetUrl(String(videoData.value.playUrl || videoData.value.url || ''))
  return directUrl || mediaIdToRawUrl(Number(videoData.value.mediaId || 0))
})
const videoStatus = computed(() => String(videoData.value.status || ''))
const hasVideoAsset = computed(() => Boolean(videoData.value.mediaId || videoData.value.url || videoData.value.hlsUrl || videoData.value.m3u8Url))
const videoPlayable = computed(() => videoStatus.value === 'ready' && Boolean(videoHlsSrc.value))
const videoBlocked = computed(() => hasVideoAsset.value && !videoPlayable.value)
const videoBlockedText = computed(() => videoStatus.value === 'failed' ? '视频处理失败' : '视频处理中')
const videoPoster = computed(() => resolveAssetUrl(String(videoData.value.coverUrl || videoData.value.posterUrl || coverSrc.value || '')))
const videoCurrent = computed(() => String(videoData.value.currentTime || '00:00'))
const videoDuration = computed(() => String(videoData.value.duration || '00:00'))
const videoLabel = computed(() => String(videoData.value.label || '菜谱视频'))

const ingredientList = computed(() => detail.value?.ingredients || [])
const tipList = computed(() => detail.value?.tips || [])
const stepList = computed(() => detail.value?.steps || [])

const commentTree = computed(() => {
  const repliesMap = new Map<number, CommentItem[]>()
  comments.value.forEach((item) => {
    if (item.parentId) {
      const bucket = repliesMap.get(item.parentId) || []
      bucket.push(item)
      repliesMap.set(item.parentId, bucket)
    }
  })
  return comments.value
    .filter(item => !item.parentId)
    .map(item => ({
      ...item,
      replies: repliesMap.get(item.id) || [],
    }))
})

const displayedComments = computed(() => (
  showAllComments.value ? commentTree.value : commentTree.value.slice(0, commentPreviewCount)
))

const hasMoreComments = computed(() => commentTree.value.length > commentPreviewCount && !showAllComments.value)

async function loadData() {
  if (!recipeId.value) {
    detail.value = undefined
    comments.value = []
    return
  }
  try {
    const [recipeRes, commentRes] = await Promise.all([
      getRecipe(recipeId.value),
      listComments('recipe', recipeId.value),
    ])
    detail.value = recipeRes.data
    comments.value = commentRes.data?.items || []
  }
  catch {
    detail.value = undefined
    comments.value = []
  }
}

async function sendComment() {
  if (!commentText.value.trim()) return
  if (!requireLogin()) return
  if (!detail.value?.id) return
  await createComment({ targetType: 'recipe', targetId: detail.value.id, content: commentText.value })
  commentText.value = ''
  await loadData()
}

async function handleLike() {
  if (!requireLogin()) return
  if (!detail.value?.id) return
  if (detail.value.liked) {
    await unlikeRecipe(detail.value.id)
  }
  else {
    await likeRecipe(detail.value.id)
  }
  await loadData()
}

async function handleFavorite() {
  if (!requireLogin()) return
  if (!detail.value?.id) return
  if (detail.value.favorited) {
    await unfavoriteRecipe(detail.value.id)
  }
  else {
    await favoriteRecipe(detail.value.id)
  }
  await loadData()
}

async function handleFollowAuthor() {
  if (!requireLogin()) return
  if (!detail.value?.authorId) return
  if (detail.value.authorFollowed) {
    await unfollowUser(detail.value.authorId)
  }
  else {
    await followUser(detail.value.authorId)
  }
  await loadData()
}

async function openPrivateChat() {
  if (!requireLogin()) return
  if (!detail.value?.authorId) return
  const response = await createPrivateConversation({ targetUserId: detail.value.authorId })
  const conversationId = response.data?.id
  if (!conversationId) {
    uni.showToast({ title: '私聊会话创建失败', icon: 'none' })
    return
  }
  uni.navigateTo({
    url: `/pages/message/private?conversationId=${conversationId}&title=${encodeURIComponent(authorName.value)}`,
  })
}

async function toggleCommentLike(item: CommentItem) {
  if (!requireLogin()) return
  if (item.liked) {
    await unlikeComment(item.id)
  }
  else {
    await likeComment(item.id)
  }
  await loadData()
}

function showReplyBox(name?: string) {
  const mention = name ? `回复 ${name}` : '回复评论'
  uni.showToast({ title: mention, icon: 'none' })
}

function reportItem(targetType: 'recipe' | 'comment', targetId: number) {
  if (!requireLogin()) return
  const reasons = [
    { label: '垃圾营销', value: 'spam' },
    { label: '不实信息', value: 'misleading' },
    { label: '违规内容', value: 'violation' },
    { label: '其他原因', value: 'other' },
  ]
  uni.showActionSheet({
    itemList: reasons.map(item => item.label),
    success: async ({ tapIndex }) => {
      const reason = reasons[tapIndex]
      await createReport({
        targetType,
        targetId,
        reasonType: reason?.value || 'other',
        reason: reason?.label || '其他原因',
      })
      uni.showToast({ title: '举报已提交', icon: 'none' })
    },
  })
}

async function openShare() {
  if (!requireLogin()) return
  if (!detail.value?.id) return
  await shareRecipe(detail.value.id)
  uni.showToast({ title: '已记录分享', icon: 'none' })
}

function openSubmitWork() {
  if (!detail.value?.id) return
  uni.navigateTo({
    url: `/pages/community/form?relatedRecipeId=${detail.value.id}&sourceType=recipe`,
  })
}

function previewStepImage(url?: string) {
  if (!url) return
  uni.previewImage({ urls: [url] })
}

function itemText(item: Record<string, unknown>) {
  return String(item.name || item.title || item.text || item.desc || '')
}

function itemMeta(item: Record<string, unknown>) {
  return String(item.amount || item.desc || '')
}

function stepTitle(item: Record<string, unknown>, index: number) {
  return String(item.title || item.name || `步骤 ${index + 1}`)
}

function stepDescription(item: Record<string, unknown>) {
  return String(item.content || item.description || item.desc || item.text || '')
}

function stepImage(item: Record<string, unknown>) {
  return resolveAssetUrl(String(item.imageUrl || item.coverUrl || item.mediaUrl || ''))
}

function tipText(item: Record<string, unknown>) {
  return String(item.text || item.content || item.desc || '')
}

function formatCount(value?: number) {
  if (!value) return '0'
  if (value >= 10000) return `${(value / 10000).toFixed(1)}w`
  if (value >= 1000) return `${(value / 1000).toFixed(1)}k`
  return String(value)
}

function formatCommentTime(value?: string) {
  const text = formatTime(value)
  return text === '-' ? '刚刚' : text
}

function commentAvatarStyle(seed: number) {
  return { background: gradients[Math.abs(seed) % gradients.length] }
}

function stepVisual(index: number) {
  return {
    background: gradients[index % gradients.length],
    icon: stepIcons[index % stepIcons.length],
  }
}

function goBack() {
  uni.navigateBack()
}

onLoad((options) => {
  recipeId.value = Number(options?.id || 0)
})

onMounted(loadData)
</script>

<template>
  <view class="page-body recipe-detail-page">
    <view class="detail-nav">
      <view class="detail-nav__action" @click="goBack">‹</view>
      <view class="detail-nav__title">菜谱详情</view>
      <view class="detail-nav__share" @click="openShare">↗ 分享</view>
    </view>

    <template v-if="detail">
      <view class="video-hero surface-card">
        <HlsVideoPlayer
          v-if="videoPlayable"
          class="video-hero__player"
          :src="videoMp4Src"
          :hls-src="videoHlsSrc"
          :poster="videoPoster"
          :label="videoLabel"
          :status="videoStatus"
        />
        <image v-else-if="videoPoster" class="video-hero__media" :src="videoPoster" mode="aspectFill" />
        <view v-else class="video-hero__placeholder">🍳</view>
        <template v-if="!videoPlayable">
          <view v-if="videoBlocked" class="video-hero__processing">{{ videoBlockedText }}</view>
          <view v-else class="video-hero__play">▶</view>
          <view class="video-hero__label">{{ videoBlocked ? videoBlockedText : videoLabel }}</view>
        </template>
        <view v-if="!videoPlayable && !videoBlocked" class="video-hero__controls">
          <text>{{ videoCurrent }}</text>
          <view class="video-progress">
            <view class="video-progress__played" />
          </view>
          <text>{{ videoDuration }}</text>
          <text class="video-speed">1.0x</text>
          <text>⛶</text>
        </view>
      </view>

      <view class="recipe-panel surface-card">
        <view class="recipe-panel__title">{{ detail.title }}</view>
        <view class="recipe-panel__stats">
          <text>❤️ {{ formatCount(detail.likeCount) }} 点赞</text>
          <text>⭐ {{ formatCount(detail.favoriteCount) }} 收藏</text>
          <text>⏱ {{ detail.cookTime || '30分钟' }}</text>
          <text>📊 {{ detail.difficulty || '中等难度' }}</text>
        </view>
        <view class="author-row">
          <view class="author-row__avatar">👨‍🍳</view>
          <view class="author-row__body">
            <view class="author-row__name">{{ authorName }}</view>
            <view class="author-row__meta">{{ detail.categoryCode || '家常菜' }} · {{ detail.serving || '2人份' }}</view>
          </view>
          <view v-if="canFollowAuthor" class="author-row__actions">
            <view
              class="author-row__button author-row__button--follow"
              :class="{ active: detail.authorFollowed }"
              @click="handleFollowAuthor"
            >
              {{ detail.authorFollowed ? '已关注' : '+ 关注' }}
            </view>
            <view class="author-row__button author-row__button--chat" @click="openPrivateChat">
              私聊
            </view>
          </view>
        </view>
      </view>

      <view class="surface-card info-panel">
        <view class="section-label">📝 菜品简介</view>
        <view class="intro-text">{{ detail.intro || '暂无菜品简介' }}</view>

        <view class="section-label section-label--spaced">🥬 所需食材</view>
        <view class="ingredient-grid">
          <view v-for="(item, index) in ingredientList" :key="index" class="ingredient-grid__item">
            <text class="ingredient-grid__name">{{ itemText(item) }}</text>
            <text class="ingredient-grid__amount">{{ itemMeta(item) }}</text>
          </view>
        </view>
      </view>

      <view class="surface-card tips-panel">
        <view class="section-label">💡 烹饪小贴士</view>
        <view class="tips-list">
          <view v-for="(item, index) in tipList" :key="index" class="tips-list__item">
            <text class="tips-list__icon">💡</text>
            <text class="tips-list__text">{{ tipText(item) }}</text>
          </view>
        </view>
      </view>

      <view class="surface-card steps-panel">
        <view class="section-label">📖 图文步骤</view>
        <view class="step-flow">
          <view v-for="(item, index) in stepList" :key="index" class="step-card">
            <view class="step-card__index">{{ index + 1 }}</view>
            <view class="step-card__body">
              <image
                v-if="stepImage(item)"
                class="step-card__image"
                :src="stepImage(item)"
                mode="aspectFill"
                @click="previewStepImage(stepImage(item))"
              />
              <view
                v-else
                class="step-card__image step-card__image--placeholder"
                :style="{ background: stepVisual(index).background }"
              >
                {{ stepVisual(index).icon }}
              </view>
              <view class="step-card__title">{{ stepTitle(item, index) }}</view>
              <view class="step-card__text">{{ stepDescription(item) }}</view>
            </view>
          </view>
        </view>
      </view>

      <view class="surface-card comment-panel">
        <view class="comment-panel__header">
          <view class="section-label">💬 评论区</view>
          <view class="comment-panel__count">共 {{ commentTotal }} 条评论</view>
        </view>

        <view class="comment-input-bar">
          <view class="comment-input-bar__avatar">我</view>
          <view class="comment-input-bar__field">
            <input v-model="commentText" class="comment-input-bar__input" placeholder="说点什么吧..." />
          </view>
          <button class="comment-input-bar__send" @click="sendComment">发送</button>
        </view>

        <view class="comment-thread">
          <template v-if="displayedComments.length">
            <view v-for="item in displayedComments" :key="item.id" class="comment-thread__item">
              <view class="comment-avatar" :style="commentAvatarStyle(item.id)">{{ item.nickname?.slice(0, 1) || '厨' }}</view>
              <view class="comment-main">
                <view class="comment-main__head">
                  <text class="comment-main__name">{{ item.nickname || '匿名用户' }}</text>
                  <text class="comment-main__time">{{ formatCommentTime(item.createdAt) }}</text>
                </view>
                <view class="comment-main__content">{{ item.content }}</view>
                <view class="comment-main__actions">
                  <text class="comment-main__action" :class="{ active: item.liked }" @click="toggleCommentLike(item)">{{ item.liked ? '♥' : '♡' }} {{ item.likeCount || 0 }}</text>
                  <text class="comment-main__action" @click="showReplyBox(item.nickname)">💬 回复</text>
                  <text class="comment-main__action" @click="reportItem('comment', item.id)">举报</text>
                </view>

                <view v-for="reply in item.replies" :key="reply.id" class="reply-card">
                  <view class="reply-card__row">
                    <view class="reply-card__avatar" :style="commentAvatarStyle(reply.id)">{{ reply.nickname?.slice(0, 1) || '厨' }}</view>
                    <view class="reply-card__body">
                      <view class="reply-card__name">
                        {{ reply.nickname || '匿名用户' }}
                        <text v-if="reply.nickname === authorName" class="reply-card__badge">作者</text>
                      </view>
                      <view class="reply-card__text">{{ reply.content }}</view>
                      <view class="reply-card__time">{{ formatCommentTime(reply.createdAt) }}</view>
                    </view>
                  </view>
                </view>
              </view>
            </view>

            <view v-if="hasMoreComments" class="comment-more" @click="showAllComments = true">
              查看更多 {{ commentTree.length }} 条评论 ↓
            </view>
          </template>
          <EmptyState v-else title="还没有评论" description="成为第一个留言的人。" />
        </view>
      </view>

      <view class="bottom-bar">
        <view class="bottom-action" :class="{ active: detail.liked }" @click="handleLike">
          <text class="bottom-action__icon">{{ detail.liked ? '♥' : '♡' }}</text>
          <text>{{ formatCount(detail.likeCount) }}</text>
        </view>
        <view class="bottom-action" :class="{ active: detail.favorited }" @click="handleFavorite">
          <text class="bottom-action__icon">{{ detail.favorited ? '★' : '☆' }}</text>
          <text>收藏</text>
        </view>
        <view class="bottom-action">
          <text class="bottom-action__icon">💬</text>
          <text>评论</text>
        </view>
        <button class="submit-work" @click="openSubmitWork">📸 交作业</button>
      </view>

      <view class="report-entry" @click="reportItem('recipe', detail.id)">举报当前菜谱</view>
    </template>
    <EmptyState v-else title="菜谱不存在" description="可能已下线、删除，或你暂时没有查看权限。" />

  </view>
</template>

<style scoped lang="scss">
.recipe-detail-page {
  padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
}

.detail-nav {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 28rpx;
  background: rgba(255, 252, 248, 0.92);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.detail-nav__action,
.detail-nav__share {
  min-width: 88rpx;
  color: var(--app-text-soft);
  font-size: 26rpx;
}

.detail-nav__action {
  font-size: 44rpx;
  line-height: 1;
}

.detail-nav__share {
  text-align: right;
}

.detail-nav__title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
}

.video-hero,
.recipe-panel,
.info-panel,
.tips-panel,
.steps-panel,
.comment-panel {
  margin: 18rpx 28rpx 0;
}

.video-hero {
  position: relative;
  height: 440rpx;
  background: #0f172a;
  overflow: hidden;
}

.video-hero__media,
.video-hero__player,
.video-hero__placeholder {
  width: 100%;
  height: 100%;
}

.video-hero__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f6a25a, #dc6b39);
  color: rgba(255, 255, 255, 0.92);
  font-size: 112rpx;
}

.video-hero__play {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 112rpx;
  height: 112rpx;
  margin: -56rpx 0 0 -56rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.26);
  color: #fff;
  font-size: 48rpx;
  line-height: 112rpx;
  text-align: center;
  backdrop-filter: blur(10rpx);
}

.video-hero__processing {
  position: absolute;
  top: 50%;
  left: 50%;
  min-width: 220rpx;
  padding: 18rpx 28rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.7);
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  text-align: center;
  transform: translate(-50%, -50%);
  backdrop-filter: blur(10rpx);
}

.video-hero__label {
  position: absolute;
  top: 22rpx;
  right: 22rpx;
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  background: rgba(0, 0, 0, 0.42);
  color: #fff;
  font-size: 20rpx;
}

.video-hero__controls {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding: 18rpx 20rpx;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0), rgba(15, 23, 42, 0.68));
  color: #fff;
  font-size: 20rpx;
}

.video-progress {
  flex: 1;
  height: 6rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.34);
  overflow: hidden;
}

.video-progress__played {
  width: 35%;
  height: 100%;
  background: var(--app-primary);
}

.video-speed {
  padding: 2rpx 10rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.5);
  border-radius: 8rpx;
}

.recipe-panel {
  padding: 28rpx;
}

.recipe-panel__title {
  font-size: 40rpx;
  font-weight: 800;
  color: var(--app-text);
}

.recipe-panel__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 14rpx;
  color: var(--app-text-muted);
  font-size: 24rpx;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 22rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.author-row__avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff9a9e, #fecfef);
  color: #fff;
  font-size: 30rpx;
}

.author-row__body {
  flex: 1;
  min-width: 0;
}

.author-row__name {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
}

.author-row__meta {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--app-text-muted);
}

.author-row__actions {
  display: flex;
  flex-shrink: 0;
  gap: 10rpx;
}

.author-row__button {
  padding: 10rpx 24rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  line-height: 1.2;
}

.author-row__button--follow {
  border: 1rpx solid rgba(232, 109, 47, 0.34);
  color: var(--app-primary);
}

.author-row__button--follow.active {
  border-color: rgba(15, 23, 42, 0.12);
  color: var(--app-text-muted);
}

.author-row__button--chat {
  background: var(--app-primary);
  color: #fff;
}

.info-panel,
.tips-panel,
.steps-panel,
.comment-panel {
  padding: 28rpx;
}

.section-label {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
}

.section-label--spaced {
  margin-top: 28rpx;
}

.intro-text {
  margin-top: 16rpx;
  color: var(--app-text-soft);
  font-size: 26rpx;
  line-height: 1.76;
}

.ingredient-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 18rpx;
}

.ingredient-grid__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
  padding: 16rpx 18rpx;
  border-radius: 18rpx;
  background: var(--app-surface-muted);
}

.ingredient-grid__name {
  color: var(--app-text);
  font-size: 24rpx;
}

.ingredient-grid__amount {
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.tips-list {
  display: grid;
  gap: 14rpx;
  margin-top: 18rpx;
}

.tips-list__item {
  display: flex;
  gap: 10rpx;
  color: var(--app-text-soft);
  font-size: 24rpx;
  line-height: 1.7;
}

.tips-list__icon {
  flex-shrink: 0;
}

.step-flow {
  display: grid;
  gap: 24rpx;
  margin-top: 22rpx;
}

.step-card {
  display: flex;
  gap: 16rpx;
}

.step-card__index {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 52rpx;
  text-align: center;
}

.step-card__body {
  flex: 1;
  min-width: 0;
}

.step-card__image {
  width: 100%;
  height: 260rpx;
  border-radius: 18rpx;
  margin-bottom: 12rpx;
}

.step-card__image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 68rpx;
}

.step-card__title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
}

.step-card__text {
  margin-top: 8rpx;
  color: var(--app-text-soft);
  font-size: 24rpx;
  line-height: 1.72;
}

.comment-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.comment-panel__count {
  font-size: 22rpx;
  color: var(--app-text-muted);
}

.comment-input-bar {
  display: flex;
  align-items: center;
  gap: 14rpx;
  margin-top: 18rpx;
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.comment-input-bar__avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
}

.comment-input-bar__field {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: var(--app-surface-muted);
}

.comment-input-bar__input {
  width: 100%;
  height: 72rpx;
  font-size: 24rpx;
}

.comment-input-bar__send {
  height: 64rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: var(--app-primary);
  color: #fff;
  font-size: 24rpx;
  line-height: 64rpx;
}

.comment-thread {
  display: grid;
  gap: 20rpx;
  margin-top: 20rpx;
}

.comment-thread__item {
  display: flex;
  gap: 14rpx;
}

.comment-avatar,
.reply-card__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
}

.comment-avatar {
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 26rpx;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-main__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.comment-main__name {
  font-size: 25rpx;
  font-weight: 700;
  color: var(--app-text);
}

.comment-main__time {
  font-size: 20rpx;
  color: var(--app-text-muted);
}

.comment-main__content {
  margin-top: 8rpx;
  color: var(--app-text);
  font-size: 25rpx;
  line-height: 1.7;
}

.comment-main__actions {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 10rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.comment-main__action.active {
  color: var(--app-danger);
}

.reply-card {
  margin-top: 12rpx;
  padding: 12rpx 14rpx;
  border-radius: 16rpx;
  background: var(--app-surface-muted);
}

.reply-card__row {
  display: flex;
  gap: 10rpx;
}

.reply-card__avatar {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 18rpx;
}

.reply-card__body {
  flex: 1;
  min-width: 0;
}

.reply-card__name {
  font-size: 22rpx;
  font-weight: 700;
  color: var(--app-text);
}

.reply-card__badge {
  margin-left: 8rpx;
  padding: 2rpx 8rpx;
  border-radius: 8rpx;
  background: var(--app-warning-soft);
  color: var(--app-primary);
  font-size: 18rpx;
}

.reply-card__text {
  margin-top: 4rpx;
  color: var(--app-text-soft);
  font-size: 22rpx;
  line-height: 1.62;
}

.reply-card__time {
  margin-top: 4rpx;
  color: var(--app-text-muted);
  font-size: 18rpx;
}

.comment-more {
  padding: 10rpx 0 4rpx;
  color: var(--app-primary);
  font-size: 24rpx;
  text-align: center;
}

.bottom-bar {
  position: fixed;
  right: 50%;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 18rpx;
  width: calc(100% - 56rpx);
  max-width: 804rpx;
  padding: 14rpx 18rpx calc(14rpx + env(safe-area-inset-bottom));
  transform: translateX(50%);
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
  box-shadow: 0 -10rpx 24rpx rgba(15, 23, 42, 0.05);
  z-index: 50;
}

.bottom-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  min-width: 76rpx;
  color: var(--app-text-muted);
  font-size: 18rpx;
}

.bottom-action.active {
  color: var(--app-danger);
}

.bottom-action__icon {
  font-size: 34rpx;
}

.submit-work {
  flex: 1;
  height: 80rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 80rpx;
}

.report-entry {
  padding: 24rpx 28rpx 0;
  color: var(--app-danger);
  font-size: 24rpx;
}
</style>
