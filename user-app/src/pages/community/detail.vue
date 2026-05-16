<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import { createComment, likeComment, listComments, unlikeComment } from '@/api/comment'
import { createReport } from '@/api/operation'
import { favoritePost, getPost, likePost, unfavoritePost, unlikePost } from '@/api/post'
import { followUser, unfollowUser } from '@/api/user'
import type { CommentItem, PostItem } from '@/types/cook'
import { useAuthStore } from '@/stores/auth'
import { mediaIdsToUrls } from '@/utils/media'
import { formatTime, requireLogin } from '@/utils/format'

const postId = ref(0)
const detail = ref<PostItem>()
const comments = ref<CommentItem[]>([])
const commentText = ref('')
const authStore = useAuthStore()

const gradients = [
  'linear-gradient(135deg,#ff9a9e,#fecfef)',
  'linear-gradient(135deg,#ffecd2,#fcb69f)',
  'linear-gradient(135deg,#84fab0,#8fd3f4)',
  'linear-gradient(135deg,#fbc2eb,#a6c1ee)',
]

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

const authorName = computed(() => detail.value?.nickname || '匿名用户')
const recipeLabel = computed(() => {
  if (detail.value?.relatedRecipeTitle) return `关联菜谱：${detail.value.relatedRecipeTitle}`
  return detail.value?.relatedRecipeId ? `关联菜谱 #${detail.value.relatedRecipeId}` : ''
})
const locationLabel = computed(() => detail.value?.location || '')
const topics = computed(() => detail.value?.topicCodes || [])
const displayTime = computed(() => {
  const date = detail.value?.publishedAt || detail.value?.createdAt
  const time = formatTime(date)
  return time === '-' ? '刚刚' : time
})
const sourceLabel = computed(() => detail.value?.sourceType || '发布动态')
const imageUrls = computed(() => mediaIdsToUrls(detail.value?.mediaIds).slice(0, 3))
const canFollowAuthor = computed(() => Boolean(detail.value?.userId && detail.value.userId !== authStore.profile?.id))

const reportReasons = [
  { label: '垃圾营销', value: 'spam' },
  { label: '不实信息', value: 'misleading' },
  { label: '违规内容', value: 'violation' },
  { label: '其他原因', value: 'other' },
]

async function loadData() {
  if (!postId.value) {
    detail.value = undefined
    comments.value = []
    return
  }
  try {
    const [postRes, commentRes] = await Promise.all([
      getPost(postId.value),
      listComments('post', postId.value),
    ])
    detail.value = postRes.data
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
  await createComment({ targetType: 'post', targetId: detail.value.id, content: commentText.value })
  commentText.value = ''
  await loadData()
}

async function handleLike() {
  if (!requireLogin()) return
  if (!detail.value?.id) return
  if (detail.value.liked) {
    await unlikePost(detail.value.id)
  }
  else {
    await likePost(detail.value.id)
  }
  await loadData()
}

async function handleFavorite() {
  if (!requireLogin()) return
  if (!detail.value?.id) return
  if (detail.value.favorited) {
    await unfavoritePost(detail.value.id)
  }
  else {
    await favoritePost(detail.value.id)
  }
  await loadData()
}

async function handleFollowAuthor() {
  if (!requireLogin()) return
  if (!detail.value?.userId) return
  if (detail.value.authorFollowed) {
    await unfollowUser(detail.value.userId)
  }
  else {
    await followUser(detail.value.userId)
  }
  await loadData()
}

function openRecipe() {
  if (!detail.value?.relatedRecipeId) return
  uni.navigateTo({ url: `/pages/recipe/detail?id=${detail.value.relatedRecipeId}` })
}

function goBack() {
  uni.navigateBack()
}

function commentStyle(seed: number) {
  return { background: gradients[Math.abs(seed) % gradients.length] }
}

function reportTarget(targetType: 'post' | 'comment', targetId: number) {
  if (!requireLogin()) return
  uni.showActionSheet({
    itemList: reportReasons.map(item => item.label),
    success: async ({ tapIndex }) => {
      const reason = reportReasons[tapIndex]
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

function sharePost() {
  uni.showToast({ title: '后端暂未提供动态分享接口', icon: 'none' })
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

function formatCommentTime(value?: string) {
  const text = formatTime(value)
  return text === '-' ? '刚刚' : text
}

onLoad((options) => {
  postId.value = Number(options?.id || 0)
})

onMounted(loadData)
</script>

<template>
  <view class="page-body post-detail-page">
    <view class="detail-nav">
      <view class="detail-nav__back" @click="goBack">‹</view>
      <view class="detail-nav__title">动态详情</view>
      <view class="detail-nav__placeholder" />
    </view>

    <template v-if="detail">
      <view class="surface-card post-panel">
        <view class="post-header">
          <view class="post-header__avatar" :style="commentStyle(detail.id)">{{ detail.nickname?.slice(0, 1) || '厨' }}</view>
          <view class="post-header__body">
            <view class="post-header__name">{{ detail.nickname || '匿名用户' }}</view>
            <view class="post-header__time">{{ displayTime }} · {{ sourceLabel }}</view>
          </view>
          <view
            v-if="canFollowAuthor"
            class="post-header__follow"
            :class="{ active: detail.authorFollowed }"
            @click="handleFollowAuthor"
          >
            {{ detail.authorFollowed ? '已关注' : '+ 关注' }}
          </view>
        </view>

        <view class="post-content">{{ detail.content || '今天认真做饭，也认真记录。' }}</view>

        <view v-if="topics.length" class="topic-row">
          <text v-for="topic in topics" :key="topic" class="topic-chip">#{{ topic }}</text>
        </view>

        <view v-if="recipeLabel || locationLabel" class="info-tag-row">
          <view v-if="recipeLabel" class="info-tag recipe-tag" @click="openRecipe">
            <text>📖</text>
            <text>{{ recipeLabel }}</text>
            <text>›</text>
          </view>
          <view v-if="locationLabel" class="info-tag location-tag">
            <text>📍</text>
            <text>{{ locationLabel }}</text>
          </view>
        </view>

        <view v-if="imageUrls.length" class="post-images" :class="`post-images--${imageUrls.length}`">
          <image v-for="url in imageUrls" :key="url" class="post-image" :src="url" mode="aspectFill" />
        </view>

        <view class="post-stats">
          <text><text class="value">{{ detail.likeCount || 0 }}</text> 点赞</text>
          <text><text class="value">{{ detail.commentCount || commentTree.length }}</text> 评论</text>
          <text><text class="value">{{ detail.favoriteCount || 0 }}</text> 收藏</text>
        </view>
      </view>

      <view class="surface-card comment-panel">
        <view class="comment-panel__head">评论 <text class="comment-panel__count">({{ detail.commentCount || commentTree.length }})</text></view>
        <view class="comment-list">
          <view v-for="item in commentTree" :key="item.id" class="comment-item">
            <view class="comment-item__top">
              <view class="comment-item__avatar" :style="commentStyle(item.id)">{{ item.nickname?.slice(0, 1) || '厨' }}</view>
              <text class="comment-item__name">{{ item.nickname || '匿名用户' }}</text>
              <text class="comment-item__like" :class="{ active: item.liked }" @click="toggleCommentLike(item)">{{ item.liked ? '♥' : '♡' }} {{ item.likeCount || 0 }}</text>
              <text class="comment-item__time">{{ formatCommentTime(item.createdAt) }}</text>
            </view>
            <view class="comment-item__text">{{ item.content }}</view>
            <view v-for="reply in item.replies" :key="reply.id" class="reply-item">
              <view class="reply-item__top">
                <view class="reply-item__avatar" :style="commentStyle(reply.id)">{{ reply.nickname?.slice(0, 1) || '厨' }}</view>
                <text class="reply-item__name">{{ reply.nickname || '匿名用户' }}</text>
              </view>
              <view class="reply-item__text"><text class="reply-item__mention">@{{ item.nickname }}</text> {{ reply.content }}</view>
              <view class="reply-item__time">{{ formatCommentTime(reply.createdAt) }}</view>
            </view>
            <view class="comment-actions">
              <text @click="reportTarget('comment', item.id)">举报</text>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-bar">
        <input v-model="commentText" class="bottom-bar__input" placeholder="说点什么..." />
        <view class="bottom-bar__action" :class="{ active: detail.liked }" @click="handleLike">
          <text class="icon">{{ detail.liked ? '♥' : '♡' }}</text>
          <text>{{ detail.likeCount || 0 }}</text>
        </view>
        <view class="bottom-bar__action" :class="{ active: detail.favorited }" @click="handleFavorite">
          <text class="icon">{{ detail.favorited ? '★' : '☆' }}</text>
          <text>收藏</text>
        </view>
        <view class="bottom-bar__action" @click="sharePost">
          <text class="icon">🔗</text>
          <text>分享</text>
        </view>
      </view>

      <view class="report-link" @click="reportTarget('post', detail.id)">举报当前动态</view>
    </template>

    <EmptyState v-else title="动态不存在" description="可能已被删除或暂不可见。" />
  </view>
</template>

<style scoped lang="scss">
.post-detail-page {
  padding-bottom: calc(164rpx + env(safe-area-inset-bottom));
}

.detail-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 22rpx 28rpx;
  background: rgba(255, 252, 248, 0.92);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.detail-nav__back {
  font-size: 42rpx;
  line-height: 1;
  color: var(--app-text-soft);
}

.detail-nav__title {
  flex: 1;
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
}

.detail-nav__placeholder {
  width: 42rpx;
}

.post-panel,
.comment-panel {
  margin: 18rpx 28rpx 0;
  padding: 28rpx;
}

.post-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.post-header__avatar,
.comment-item__avatar,
.reply-item__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
}

.post-header__avatar {
  width: 78rpx;
  height: 78rpx;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 30rpx;
}

.post-header__body {
  flex: 1;
  min-width: 0;
}

.post-header__name {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
}

.post-header__time {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: var(--app-text-muted);
}

.post-header__follow {
  padding: 10rpx 22rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(232, 109, 47, 0.34);
  color: var(--app-primary);
  font-size: 22rpx;
}

.post-header__follow.active {
  border-color: rgba(15, 23, 42, 0.12);
  color: var(--app-text-muted);
}

.post-content {
  margin-top: 20rpx;
  color: var(--app-text);
  font-size: 26rpx;
  line-height: 1.8;
  white-space: pre-line;
}

.topic-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 14rpx;
}

.topic-chip {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(232, 109, 47, 0.1);
  color: var(--app-primary);
  font-size: 21rpx;
}

.info-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.info-tag {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 16rpx;
  border-radius: 16rpx;
  font-size: 22rpx;
}

.recipe-tag {
  background: var(--app-warning-soft);
  color: var(--app-warning);
}

.location-tag {
  background: rgba(45, 135, 104, 0.12);
  color: #2d8768;
}

.post-images {
  display: grid;
  gap: 8rpx;
  margin-top: 18rpx;
  overflow: hidden;
  border-radius: 18rpx;
}

.post-images--1 {
  grid-template-columns: 1fr;
}

.post-images--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.post-images--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.post-image {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 54rpx;
}

.post-images--1 .post-image {
  aspect-ratio: 16 / 9;
}

.post-stats {
  display: flex;
  gap: 20rpx;
  margin-top: 18rpx;
  padding: 18rpx 0 0;
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
  color: var(--app-text-muted);
  font-size: 23rpx;
}

.post-stats .value {
  color: var(--app-text);
  font-weight: 700;
}

.comment-panel__head {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--app-text);
}

.comment-panel__count {
  font-size: 24rpx;
  color: var(--app-text-muted);
  font-weight: 400;
}

.comment-list {
  display: grid;
  gap: 20rpx;
  margin-top: 20rpx;
}

.comment-item {
  padding-bottom: 20rpx;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.05);
}

.comment-item:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.comment-item__top {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.comment-item__avatar {
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 24rpx;
}

.comment-item__name {
  flex: 1;
  font-size: 24rpx;
  font-weight: 700;
  color: var(--app-text);
}

.comment-item__like,
.comment-item__time {
  font-size: 20rpx;
  color: var(--app-text-muted);
}

.comment-item__like.active {
  color: var(--app-danger);
}

.comment-item__text {
  padding-left: 68rpx;
  margin-top: 8rpx;
  color: var(--app-text);
  font-size: 24rpx;
  line-height: 1.7;
}

.reply-item {
  padding: 12rpx 0 0 68rpx;
}

.reply-item__top {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.reply-item__avatar {
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  font-size: 18rpx;
}

.reply-item__name {
  font-size: 22rpx;
  font-weight: 700;
  color: var(--app-text);
}

.reply-item__text {
  margin-top: 4rpx;
  color: var(--app-text-soft);
  font-size: 22rpx;
  line-height: 1.65;
}

.reply-item__mention {
  color: var(--app-primary);
}

.reply-item__time {
  margin-top: 4rpx;
  color: var(--app-text-muted);
  font-size: 18rpx;
}

.comment-actions {
  padding-left: 68rpx;
  margin-top: 8rpx;
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.bottom-bar {
  position: fixed;
  right: 50%;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 12rpx;
  width: calc(100% - 56rpx);
  max-width: 804rpx;
  padding: 12rpx 18rpx calc(12rpx + env(safe-area-inset-bottom));
  transform: translateX(50%);
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
  z-index: 40;
}

.bottom-bar__input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: var(--app-surface-muted);
  font-size: 24rpx;
}

.bottom-bar__action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rpx;
  min-width: 68rpx;
  color: var(--app-text-muted);
  font-size: 18rpx;
}

.bottom-bar__action.active {
  color: var(--app-danger);
}

.bottom-bar__action .icon {
  font-size: 32rpx;
}

.report-link {
  padding: 24rpx 28rpx 0;
  color: var(--app-danger);
  font-size: 24rpx;
}
</style>
