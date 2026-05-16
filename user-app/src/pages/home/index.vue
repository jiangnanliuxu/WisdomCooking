<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import RecipeCard from '@/components/RecipeCard.vue'
import { getHomeData } from '@/api/operation'
import type { BannerItem, HomeData, RecipeListItem } from '@/types/cook'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'

const loading = ref(false)
const homeData = ref<HomeData>({
  banners: [],
  categories: [],
  recommendedRecipes: [],
  recommendedUsers: [],
  hotKeywords: [],
})
const bannerIndex = ref(0)
const dragStartX = ref(0)
const dragOffset = ref(0)
const isDragging = ref(false)
let bannerTimer: ReturnType<typeof setInterval> | undefined

const homeCategories = [
  { name: '川菜', icon: '🌶️', code: 'sichuan' },
  { name: '粤菜', icon: '🥘', code: 'cantonese' },
  { name: '烘焙', icon: '🍞', code: 'baking' },
  { name: '减脂餐', icon: '🥗', keyword: '减脂餐' },
  { name: '家常菜', icon: '🍳', keyword: '家常菜' },
  { name: '海鲜', icon: '🦐', keyword: '海鲜' },
  { name: '甜品', icon: '🍰', code: 'dessert' },
  { name: '汤品', icon: '🍲', keyword: '汤品' },
  { name: '快手菜', icon: '⚡', keyword: '快手菜' },
]

interface BannerSlide extends BannerItem {
  placeholder?: boolean
  accentClass?: string
}

const placeholderBanners: BannerSlide[] = [
  {
    id: -1,
    title: '轮播内容待配置',
    subtitle: '后台暂未配置足够轮播图，先看看今日推荐菜谱。',
    placeholder: true,
    accentClass: 'banner-slide--orange',
  },
  {
    id: -2,
    title: '发现下厨灵感',
    subtitle: '浏览分类、达人和社区动态，找到适合今天的一餐。',
    placeholder: true,
    accentClass: 'banner-slide--green',
  },
  {
    id: -3,
    title: '记录你的餐桌',
    subtitle: '发布动态或菜谱，把好味道分享给更多人。',
    placeholder: true,
    accentClass: 'banner-slide--blue',
  },
]

const displayRecipes = computed<RecipeListItem[]>(() => {
  return homeData.value.recommendedRecipes
})

const bannerSlides = computed<BannerSlide[]>(() => {
  const realBanners = homeData.value.banners.map((item, index) => ({
    ...item,
    accentClass: `banner-slide--${index % 3}`,
  }))

  if (realBanners.length >= 2) {
    return realBanners
  }

  return [...realBanners, ...placeholderBanners].slice(0, 3)
})

const bannerTrackStyle = computed(() => ({
  transform: `translateX(calc(${-bannerIndex.value * 100}% + ${dragOffset.value}px))`,
}))

function getBannerImage(item: BannerSlide) {
  return resolveAssetUrl(item.imageUrl || '') || mediaIdToRawUrl(item.imageMediaId)
}

function stopAutoSlide() {
  if (!bannerTimer) return
  clearInterval(bannerTimer)
  bannerTimer = undefined
}

function startAutoSlide() {
  stopAutoSlide()
  if (bannerSlides.value.length <= 1) return
  bannerTimer = setInterval(() => {
    goBanner(1)
  }, 3500)
}

function goBanner(offset: number) {
  const total = bannerSlides.value.length
  if (!total) return
  bannerIndex.value = (bannerIndex.value + offset + total) % total
  dragOffset.value = 0
}

function getPointX(event: MouseEvent | TouchEvent) {
  if ('touches' in event) {
    return event.touches[0]?.clientX ?? event.changedTouches[0]?.clientX ?? 0
  }
  return event.clientX
}

function startBannerDrag(event: MouseEvent | TouchEvent) {
  if (bannerSlides.value.length <= 1) return
  isDragging.value = true
  dragStartX.value = getPointX(event)
  dragOffset.value = 0
  stopAutoSlide()
}

function moveBannerDrag(event: MouseEvent | TouchEvent) {
  if (!isDragging.value) return
  dragOffset.value = getPointX(event) - dragStartX.value
}

function endBannerDrag() {
  if (!isDragging.value) return
  const threshold = 48
  const offset = dragOffset.value
  isDragging.value = false
  dragOffset.value = 0

  if (offset > threshold) {
    goBanner(-1)
  }
  else if (offset < -threshold) {
    goBanner(1)
  }
  startAutoSlide()
}

async function loadData() {
  loading.value = true
  try {
    const response = await getHomeData({ recipeLimit: '8', userLimit: '6' })
    homeData.value = response.data || homeData.value
  }
  catch {
    homeData.value = {
      banners: [],
      categories: [],
      recommendedRecipes: [],
      recommendedUsers: [],
      hotKeywords: [],
    }
  }
  finally {
    loading.value = false
  }
}

function openSearch(keyword = '') {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : ''
  uni.navigateTo({ url: `/pages/search/index${query}` })
}

function openCategory(item?: { code?: string; name?: string; keyword?: string }) {
  if (!item) {
    uni.navigateTo({ url: '/pages/category/index' })
    return
  }

  if (!item.code) {
    openSearch(item.keyword || item.name || '')
    return
  }

  const query = new URLSearchParams({
    categoryCode: item.code,
    ...(item.name ? { keyword: item.name } : {}),
  }).toString()

  uni.navigateTo({ url: `/pages/search/index?${query}` })
}

watch(bannerSlides, (slides) => {
  if (bannerIndex.value >= slides.length) {
    bannerIndex.value = 0
  }
  startAutoSlide()
})

onMounted(() => {
  loadData()
  startAutoSlide()
  if (typeof window !== 'undefined') {
    window.addEventListener('mousemove', moveBannerDrag)
    window.addEventListener('mouseup', endBannerDrag)
  }
})

onBeforeUnmount(() => {
  stopAutoSlide()
  if (typeof window !== 'undefined') {
    window.removeEventListener('mousemove', moveBannerDrag)
    window.removeEventListener('mouseup', endBannerDrag)
  }
})
</script>

<template>
  <view class="page-body home-page">
    <view class="search-bar" @click="openSearch()">
      <view class="search-input">
        <text class="search-icon">⌕</text>
        <text>搜索菜谱、食材或达人...</text>
      </view>
    </view>

    <view
      class="banner"
      @mousedown="startBannerDrag"
      @touchstart="startBannerDrag"
      @touchmove.stop.prevent="moveBannerDrag"
      @touchend="endBannerDrag"
      @touchcancel="endBannerDrag"
    >
      <view class="banner-track" :class="{ dragging: isDragging }" :style="bannerTrackStyle">
        <view
          v-for="item in bannerSlides"
          :key="item.id"
          class="banner-slide"
          :class="item.accentClass"
        >
          <image v-if="getBannerImage(item)" class="banner-image" :src="getBannerImage(item)" mode="aspectFill" />
          <view class="banner-shade" />
          <view class="banner-content">
            <view class="banner-title">{{ item.placeholder ? '✨' : '🔥' }} {{ item.title || '发现今日灵感' }}</view>
            <view class="banner-desc">{{ item.subtitle || '最新活动、菜谱专题和推荐内容会在这里展示。' }}</view>
          </view>
          <view class="banner-mark">Cook</view>
        </view>
      </view>
      <view class="banner-dots">
        <view
          v-for="(_, index) in bannerSlides"
          :key="index"
          class="dot"
          :class="{ active: bannerIndex === index }"
        />
      </view>
    </view>

    <view class="category-nav surface-card">
      <view class="category-grid">
        <view v-for="(item, index) in homeCategories" :key="item.name" class="category-item" @click="openCategory(item)">
          <view class="category-icon" :class="`cat-${index % 5}`">{{ item.icon }}</view>
          <text>{{ item.name }}</text>
        </view>
        <view class="category-item" @click="openCategory(undefined)">
          <view class="category-icon cat-more">📋</view>
          <text>更多</text>
        </view>
      </view>
    </view>

    <view class="content-section">
      <view class="section-head">
        <text class="section-title">为你推荐</text>
        <text class="section-link" @click="loadData()">换一批 ↻</text>
      </view>
      <view v-if="loading" class="loading-text">加载中...</view>
      <view v-else-if="displayRecipes.length" class="recipe-waterfall">
        <RecipeCard v-for="(item, index) in displayRecipes" :key="item.id" :item="item" :index="index" variant="masonry" />
      </view>
      <EmptyState v-else title="暂无推荐菜谱" description="稍后再试，或者先去分类页看看。" />

      <view class="section-head">
        <text class="section-title">推荐达人</text>
        <text class="section-link" @click="openSearch()">去搜索</text>
      </view>
      <scroll-view v-if="homeData.recommendedUsers.length" scroll-x class="expert-scroll">
        <view class="expert-list">
          <view v-for="(item, index) in homeData.recommendedUsers" :key="item.id" class="expert-card surface-card">
            <view class="expert-avatar">{{ ['👩‍🍳', '🧑‍🍳', '🍰'][index % 3] }}</view>
            <view class="expert-name">{{ item.nickname || `用户${item.id}` }}</view>
            <view class="expert-meta">{{ item.recipeCount || 0 }} 菜谱 · {{ item.followerCount || 0 }} 粉丝</view>
            <view class="expert-bio">{{ item.bio || '分享家常菜谱和饮食记录。' }}</view>
          </view>
        </view>
      </scroll-view>
      <EmptyState v-else title="暂无推荐达人" description="稍后再试，或直接去搜索感兴趣的作者。" />

      <view v-if="homeData.hotKeywords.length" class="hot-row">
        <view v-for="item in homeData.hotKeywords.slice(0, 4)" :key="item" class="food-chip" @click="openSearch(item)">
          {{ item }}
        </view>
      </view>
    </view>

    <AppTabBar current="home" />
  </view>
</template>

<style scoped lang="scss">
.home-page {
  padding-top: 1rpx;
}

.search-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  padding: 18rpx 28rpx 16rpx;
  background: rgba(255, 252, 248, 0.9);
  backdrop-filter: blur(18rpx);
}

.search-input {
  min-height: 76rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: var(--app-surface-muted);
  color: var(--app-text-muted);
  font-size: 26rpx;
  box-shadow: inset 0 0 0 1rpx rgba(15, 23, 42, 0.05);
}

.search-icon {
  font-size: 32rpx;
  color: var(--app-primary);
}

.banner {
  position: relative;
  min-height: 320rpx;
  margin: 12rpx 28rpx 24rpx;
  border-radius: 30rpx;
  overflow: hidden;
  box-shadow: var(--app-shadow-lg);
  cursor: grab;
  touch-action: pan-y;
  user-select: none;
}

.banner:active {
  cursor: grabbing;
}

.banner-track {
  min-height: 320rpx;
  display: flex;
  transition: transform 0.36s ease;
  will-change: transform;
}

.banner-track.dragging {
  transition: none;
}

.banner-slide {
  position: relative;
  flex: 0 0 100%;
  min-height: 320rpx;
  overflow: hidden;
  background: linear-gradient(135deg, #e86d2f 0%, #ff9b54 48%, #2d8768 100%);
}

.banner-slide--1,
.banner-slide--green {
  background: linear-gradient(135deg, #2d8768 0%, #69b578 52%, #f3a83c 100%);
}

.banner-slide--2,
.banner-slide--blue {
  background: linear-gradient(135deg, #2f6f9f 0%, #54a3c7 52%, #f08f5f 100%);
}

.banner-slide--orange {
  background: linear-gradient(135deg, #e86d2f 0%, #ff9b54 52%, #2d8768 100%);
}

.banner-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.banner-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.58), rgba(15, 23, 42, 0.16) 58%, rgba(15, 23, 42, 0.04));
}

.banner-content {
  position: relative;
  z-index: 2;
  padding: 46rpx 44rpx;
  color: #fff;
}

.banner-title {
  max-width: 470rpx;
  font-size: 40rpx;
  font-weight: 800;
  line-height: 1.25;
}

.banner-desc {
  max-width: 480rpx;
  margin-top: 16rpx;
  font-size: 25rpx;
  line-height: 1.6;
  opacity: 0.92;
}

.banner-mark {
  position: absolute;
  right: -18rpx;
  bottom: 34rpx;
  color: rgba(255, 255, 255, 0.16);
  font-size: 92rpx;
  font-weight: 900;
  transform: rotate(-9deg);
}

.banner-dots {
  position: absolute;
  z-index: 3;
  left: 44rpx;
  bottom: 28rpx;
  display: flex;
  gap: 10rpx;
}

.dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.5);
}

.dot.active {
  width: 36rpx;
  background: #fff;
}

.category-nav {
  margin: 0 28rpx;
  padding: 26rpx 18rpx;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 22rpx 0;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
  color: var(--app-text);
  font-size: 22rpx;
}

.category-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
}

.cat-0 { background: #fff1f0; }
.cat-1 { background: #fff7e6; }
.cat-2 { background: #f6ffed; }
.cat-3 { background: #e6fffb; }
.cat-4,
.cat-more { background: #f3efff; }

.content-section {
  padding: 0 28rpx 24rpx;
}

.recipe-waterfall {
  column-count: 2;
  column-gap: 18rpx;
}

.loading-text {
  padding: 32rpx;
  color: var(--app-text-muted);
  text-align: center;
}

.expert-scroll {
  width: 100%;
}

.expert-list {
  display: inline-flex;
  gap: 18rpx;
  padding-bottom: 6rpx;
}

.expert-card {
  width: 250rpx;
  padding: 24rpx;
}

.expert-avatar {
  width: 82rpx;
  height: 82rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
  font-size: 38rpx;
}

.expert-name {
  margin-top: 16rpx;
  font-size: 27rpx;
  font-weight: 700;
  color: var(--app-text);
}

.expert-meta,
.expert-bio {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--app-text-muted);
  line-height: 1.6;
}

.hot-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 28rpx;
}
</style>
