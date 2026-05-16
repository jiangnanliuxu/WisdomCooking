<script setup lang="ts">
import { computed } from 'vue'
import type { PostItem } from '@/types/cook'
import { useAuthStore } from '@/stores/auth'
import { mediaIdsToUrls } from '@/utils/media'

const props = withDefaults(defineProps<{
  item: PostItem
  index?: number
}>(), {
  index: 0,
})

const emit = defineEmits<{
  like: [id: number]
  favorite: [id: number]
  follow: [item: PostItem]
}>()

const authStore = useAuthStore()

const gradients = [
  'linear-gradient(135deg,#ff9a9e,#fecfef)',
  'linear-gradient(135deg,#84fab0,#8fd3f4)',
  'linear-gradient(135deg,#fbc2eb,#a6c1ee)',
  'linear-gradient(135deg,#a1c4fd,#c2e9fb)',
]

const accent = computed(() => gradients[Math.abs((props.item.id || props.index) + props.index) % gradients.length])
const topics = computed(() => props.item.topicCodes || [])
const imageUrls = computed(() => mediaIdsToUrls(props.item.mediaIds).slice(0, 3))
const displayTime = computed(() => props.item.publishedAt || props.item.createdAt || '刚刚')
const canFollowAuthor = computed(() => Boolean(props.item.userId && props.item.userId !== authStore.profile?.id))
const recipeLabel = computed(() => {
  if (props.item.relatedRecipeTitle) return `关联菜谱：${props.item.relatedRecipeTitle}`
  return props.item.relatedRecipeId ? `关联菜谱 #${props.item.relatedRecipeId}` : ''
})
const locationLabel = computed(() => props.item.location || '')

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/community/detail?id=${id}` })
}

function openRecipe() {
  if (!props.item.relatedRecipeId) return
  uni.navigateTo({ url: `/pages/recipe/detail?id=${props.item.relatedRecipeId}` })
}
</script>

<template>
  <view class="surface-card post-card" @click="openDetail(item.id)">
    <view class="post-header">
      <view class="post-avatar" :style="{ background: accent }">{{ item.nickname?.slice(0, 1) || '厨' }}</view>
      <view class="post-user">
        <view class="post-name">{{ item.nickname || '匿名用户' }}</view>
        <view class="post-time">{{ displayTime }}</view>
      </view>
      <view v-if="canFollowAuthor" class="follow-btn" :class="{ active: item.authorFollowed }" @click.stop="emit('follow', item)">
        {{ item.authorFollowed ? '已关注' : '+ 关注' }}
      </view>
    </view>

    <view class="post-content">{{ item.content || '-' }}</view>
    <view v-if="recipeLabel || locationLabel" class="tag-row">
      <view v-if="recipeLabel" class="recipe-tag" @click.stop="openRecipe">📖 {{ recipeLabel }}</view>
      <view v-if="locationLabel" class="location-tag">📍 {{ locationLabel }}</view>
    </view>
    <view v-if="imageUrls.length" class="post-images" :class="`post-images--${imageUrls.length}`">
      <image v-for="url in imageUrls" :key="url" class="post-image" :src="url" mode="aspectFill" />
    </view>
    <view class="post-meta">
      <text v-for="topic in topics.slice(0, 2)" :key="topic">#{{ topic }}</text>
    </view>
    <view class="post-actions">
      <view class="post-action" :class="{ liked: item.liked }" @click.stop="emit('like', item.id)">
        <text>{{ item.liked ? '♥' : '♡' }}</text>
        <text>{{ item.likeCount || 0 }}</text>
      </view>
      <view class="post-action">
        <text>□</text>
        <text>{{ item.commentCount || 0 }}</text>
      </view>
      <view class="post-action" :class="{ liked: item.favorited }" @click.stop="emit('favorite', item.id)">
        <text>{{ item.favorited ? '★' : '☆' }}</text>
        <text>收藏</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.post-card {
  padding: 28rpx;
}

.post-header {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.post-avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.post-user {
  flex: 1;
  min-width: 0;
}

.post-name {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
}

.post-time {
  margin-top: 4rpx;
  font-size: 21rpx;
  color: var(--app-text-muted);
}

.follow-btn {
  padding: 8rpx 20rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(232, 109, 47, 0.34);
  color: var(--app-primary);
  font-size: 22rpx;
}

.follow-btn.active {
  border-color: rgba(15, 23, 42, 0.12);
  color: var(--app-text-muted);
}

.post-content {
  margin-top: 20rpx;
  font-size: 26rpx;
  line-height: 1.8;
  color: var(--app-text);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 12rpx;
}

.recipe-tag,
.location-tag {
  display: inline-flex;
  align-items: center;
  padding: 6rpx 16rpx;
  border-radius: 10rpx;
  font-size: 22rpx;
}

.recipe-tag {
  background: var(--app-warning-soft);
  color: var(--app-warning);
}

.location-tag {
  background: rgba(45, 135, 104, 0.1);
  color: var(--app-success);
}

.post-images {
  display: grid;
  gap: 8rpx;
  margin-top: 16rpx;
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
  min-height: 156rpx;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 46rpx;
}

.post-meta {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: var(--app-text-muted);
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 34rpx;
  padding-top: 18rpx;
  margin-top: 18rpx;
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.post-action {
  display: flex;
  align-items: center;
  gap: 8rpx;
  color: var(--app-text-muted);
  font-size: 24rpx;
}

.post-action.liked {
  color: var(--app-danger);
}
</style>
