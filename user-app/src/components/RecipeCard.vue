<script setup lang="ts">
import { computed } from 'vue'
import type { RecipeListItem } from '@/types/cook'
import { mediaIdToRawUrl } from '@/utils/media'

const props = withDefaults(defineProps<{
  item: RecipeListItem
  variant?: 'list' | 'masonry'
  index?: number
}>(), {
  variant: 'list',
  index: 0,
})

const gradients = [
  'linear-gradient(135deg,#ff9a9e,#fecfef)',
  'linear-gradient(135deg,#a1c4fd,#c2e9fb)',
  'linear-gradient(135deg,#fbc2eb,#a6c1ee)',
  'linear-gradient(135deg,#f6d365,#fda085)',
  'linear-gradient(135deg,#84fab0,#8fd3f4)',
  'linear-gradient(135deg,#fccb90,#d57eeb)',
]

const icons = ['🍳', '🥗', '🍰', '🍲', '🥘', '🌶️']

const accentIndex = computed(() => Math.abs((props.item.id || props.index) + props.index) % gradients.length)
const coverStyle = computed(() => ({
  background: gradients[accentIndex.value],
}))
const coverIcon = computed(() => icons[accentIndex.value])
const coverSrc = computed(() => mediaIdToRawUrl(props.item.coverMediaId))
const timeText = computed(() => props.item.cookTime || '30min')
const difficultyText = computed(() => props.item.difficulty || '家常')
const coverRatioClass = computed(() => {
  if (props.variant !== 'masonry') return ''
  const seed = Math.abs((props.item.id || props.index) + (props.item.title?.length || 0))
  return `recipe-cover--ratio-${seed % 3}`
})

function formatCount(value?: number) {
  if (!value) return '0'
  if (value >= 10000) return `${(value / 10000).toFixed(1)}w`
  if (value >= 1000) return `${(value / 1000).toFixed(1)}k`
  return String(value)
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}
</script>

<template>
  <view class="surface-card recipe-card" :class="`recipe-card--${variant}`" @click="openDetail(item.id)">
    <view class="recipe-cover" :class="coverRatioClass" :style="coverSrc ? undefined : coverStyle">
      <image v-if="coverSrc" class="recipe-cover__image" :src="coverSrc" mode="aspectFill" />
      <text v-else>{{ coverIcon }}</text>
    </view>
    <view class="recipe-info">
      <view class="recipe-title">{{ item.title }}</view>
      <view v-if="variant === 'list'" class="recipe-intro">{{ item.intro || '暂未填写简介' }}</view>
      <view class="recipe-meta">
        <text class="difficulty">{{ difficultyText }}</text>
        <text>⏱ {{ timeText }}</text>
        <text>♥ {{ formatCount(item.likeCount) }}</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.recipe-card {
  display: flex;
  overflow: hidden;
  border-radius: 26rpx;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.recipe-card:active {
  transform: scale(0.985);
}

.recipe-card--masonry {
  display: inline-block;
  width: 100%;
  margin-bottom: 18rpx;
  break-inside: avoid;
}

.recipe-cover {
  width: 188rpx;
  min-height: 188rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 60rpx;
}

.recipe-card--masonry .recipe-cover {
  width: 100%;
  min-height: 280rpx;
  aspect-ratio: 1 / 0.84;
  font-size: 72rpx;
}

.recipe-card--masonry .recipe-cover--ratio-1 {
  aspect-ratio: 1 / 1.16;
}

.recipe-card--masonry .recipe-cover--ratio-2 {
  aspect-ratio: 1 / 0.72;
}

.recipe-cover__image {
  width: 100%;
  height: 100%;
}

.recipe-info {
  flex: 1;
  min-width: 0;
  padding: 22rpx;
}

.recipe-card--masonry .recipe-info {
  padding: 18rpx 20rpx 20rpx;
}

.recipe-title {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--app-text);
  line-height: 1.38;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recipe-card--masonry .recipe-title {
  font-size: 27rpx;
  -webkit-line-clamp: 3;
}

.recipe-intro {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--app-text-soft);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recipe-meta {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 16rpx;
  font-size: 22rpx;
  color: var(--app-text-muted);
}

.difficulty {
  padding: 3rpx 12rpx;
  border-radius: 8rpx;
  background: var(--app-warning-soft);
  color: var(--app-warning);
  font-size: 20rpx;
}
</style>
