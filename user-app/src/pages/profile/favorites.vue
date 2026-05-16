<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { unfavoritePost } from '@/api/post'
import { unfavoriteRecipe } from '@/api/recipe'
import { listMyFavorites } from '@/api/user'
import type { UserInteraction } from '@/types/cook'
import { resolveAssetUrl } from '@/utils/media'
import { requireLogin } from '@/utils/format'

const targetType = ref<'recipe' | 'post'>('recipe')
const rows = ref<UserInteraction[]>([])

const recipeGradients = [
  'linear-gradient(135deg,#ff9a9e,#fecfef)',
  'linear-gradient(135deg,#f6d365,#fda085)',
  'linear-gradient(135deg,#84fab0,#8fd3f4)',
]
const recipeIcons = ['🐟', '🍲', '🥗']
const postGradients = [
  'linear-gradient(135deg,#fccb90,#d57eeb)',
  'linear-gradient(135deg,#fbc2eb,#a6c1ee)',
]

const displayRows = computed(() => rows.value)

async function loadData() {
  if (!requireLogin()) return
  const response = await listMyFavorites({ targetType: targetType.value, page: '1', pageSize: '20' })
  rows.value = response.data?.items || []
}

function goBack() {
  uni.navigateBack()
}

function openItem(item: UserInteraction) {
  if (item.targetType === 'recipe' && item.targetId) {
    uni.navigateTo({ url: `/pages/recipe/detail?id=${item.targetId}` })
    return
  }
  if (item.targetType === 'post' && item.targetId) {
    uni.navigateTo({ url: `/pages/community/detail?id=${item.targetId}` })
  }
}

function coverSrc(item: UserInteraction) {
  return resolveAssetUrl(item.coverUrl || '')
}

async function cancelFavorite(item: UserInteraction) {
  if (!item.targetId) return
  if (item.targetType === 'recipe') {
    await unfavoriteRecipe(item.targetId)
  }
  else if (item.targetType === 'post') {
    await unfavoritePost(item.targetId)
  }
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <view class="page-body favorite-page">
    <view class="page-nav">
      <view class="page-nav__back" @click="goBack">‹</view>
      <view class="page-nav__title">我的收藏</view>
      <view class="page-nav__count">共{{ displayRows.length }}个</view>
    </view>

    <view class="tab-row">
      <view class="tab-row__item" :class="{ active: targetType === 'recipe' }" @click="targetType = 'recipe'; loadData()">菜谱</view>
      <view class="tab-row__item" :class="{ active: targetType === 'post' }" @click="targetType = 'post'; loadData()">动态</view>
    </view>

    <view v-if="displayRows.length" class="card-list">
      <template v-if="targetType === 'recipe'">
        <view v-for="(item, index) in displayRows" :key="item.targetId || index" class="surface-card recipe-card" @click="openItem(item)">
          <view class="recipe-card__thumb" :style="coverSrc(item) ? undefined : { background: recipeGradients[index % recipeGradients.length] }">
            <image v-if="coverSrc(item)" class="recipe-card__cover" :src="coverSrc(item)" mode="aspectFill" />
            <template v-else>{{ recipeIcons[index % recipeIcons.length] }}</template>
          </view>
          <view class="recipe-card__info">
            <view>
              <view class="recipe-card__name">{{ item.title || '未命名菜谱' }}</view>
              <view class="recipe-card__desc">{{ item.content || '暂未填写简介' }}</view>
            </view>
            <view class="recipe-card__bottom">
              <text class="recipe-card__author">👨‍🍳 {{ item.authorNickname || '未知作者' }}</text>
              <text class="recipe-card__action" @click.stop="cancelFavorite(item)">取消收藏</text>
            </view>
          </view>
        </view>
      </template>

      <template v-else>
        <view v-for="(item, index) in displayRows" :key="item.targetId || index" class="surface-card feed-card" @click="openItem(item)">
          <view class="feed-card__header">
            <view class="feed-card__avatar" :style="{ background: postGradients[index % postGradients.length] }">{{ item.authorNickname?.slice(0, 1) || '厨' }}</view>
            <view>
              <view class="feed-card__user">{{ item.authorNickname || '未知作者' }}</view>
              <view class="feed-card__time">已收藏</view>
            </view>
          </view>
          <view class="feed-card__text">{{ item.content || item.title || '未命名动态' }}</view>
          <view v-if="coverSrc(item)" class="feed-card__images">
            <image class="feed-card__image" :src="coverSrc(item)" mode="aspectFill" />
          </view>
          <view class="feed-card__footer">
            <text class="feed-card__favorite">⭐ 已收藏</text>
            <text class="feed-card__cancel" @click.stop="cancelFavorite(item)">取消收藏</text>
          </view>
        </view>
      </template>
    </view>

    <EmptyState v-else title="还没有收藏内容" description="看到喜欢的菜谱或动态后，点一下收藏就会显示在这里。" />
  </view>
</template>

<style scoped lang="scss">
.favorite-page {
  padding-bottom: 28rpx;
}

.page-nav {
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

.page-nav__back {
  font-size: 42rpx;
  color: var(--app-text-soft);
}

.page-nav__title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
}

.page-nav__count {
  margin-left: auto;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.tab-row {
  display: flex;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.tab-row__item {
  flex: 1;
  position: relative;
  padding: 22rpx 0;
  text-align: center;
  color: var(--app-text-muted);
  font-size: 24rpx;
}

.tab-row__item.active {
  color: var(--app-primary);
  font-weight: 700;
}

.tab-row__item.active::after {
  content: '';
  position: absolute;
  right: 30%;
  bottom: 0;
  left: 30%;
  height: 6rpx;
  border-radius: 999rpx;
  background: var(--app-primary);
}

.card-list {
  display: grid;
  gap: 18rpx;
  padding: 18rpx 28rpx 0;
}

.recipe-card {
  display: flex;
  overflow: hidden;
}

.recipe-card__thumb {
  width: 210rpx;
  min-height: 180rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 62rpx;
}

.recipe-card__cover {
  width: 100%;
  height: 100%;
}

.recipe-card__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 22rpx;
}

.recipe-card__name {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
}

.recipe-card__desc {
  margin-top: 8rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
  line-height: 1.7;
}

.recipe-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-top: 14rpx;
}

.recipe-card__author {
  color: var(--app-text-soft);
  font-size: 22rpx;
}

.recipe-card__action {
  padding: 6rpx 14rpx;
  border: 1rpx solid rgba(232, 109, 47, 0.3);
  border-radius: 999rpx;
  color: var(--app-primary);
  font-size: 20rpx;
}

.feed-card {
  padding: 24rpx;
}

.feed-card__header {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.feed-card__avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}

.feed-card__user {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--app-text);
}

.feed-card__time {
  margin-top: 4rpx;
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.feed-card__text {
  margin-top: 16rpx;
  color: var(--app-text);
  font-size: 24rpx;
  line-height: 1.7;
}

.feed-card__images {
  margin-top: 14rpx;
}

.feed-card__image {
  height: 220rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 56rpx;
}

.feed-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14rpx;
}

.feed-card__favorite,
.feed-card__cancel {
  font-size: 22rpx;
}

.feed-card__favorite {
  color: var(--app-warning);
}

.feed-card__cancel {
  color: var(--app-primary);
}
</style>
