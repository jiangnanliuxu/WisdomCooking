<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppNavBar from '@/components/AppNavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listMyPosts } from '@/api/post'
import { listMyRecipes } from '@/api/recipe'
import type { PostItem, RecipeListItem } from '@/types/cook'
import { mediaIdToRawUrl, mediaIdsToUrls } from '@/utils/media'
import { formatTime } from '@/utils/format'

const active = ref<'recipe' | 'post'>('recipe')
const loading = ref(false)
const recipes = ref<RecipeListItem[]>([])
const posts = ref<PostItem[]>([])

const rowsEmpty = computed(() => active.value === 'recipe' ? !recipes.value.length : !posts.value.length)

async function loadData() {
  loading.value = true
  try {
    if (active.value === 'recipe') {
      const response = await listMyRecipes({ page: '1', pageSize: '20' })
      recipes.value = response.data?.items || []
      return
    }
    const response = await listMyPosts({ page: '1', pageSize: '20' })
    posts.value = response.data?.items || []
  }
  catch {
    if (active.value === 'recipe') {
      recipes.value = []
    }
    else {
      posts.value = []
    }
  }
  finally {
    loading.value = false
  }
}

function switchTab(tab: 'recipe' | 'post') {
  active.value = tab
  loadData()
}

function openRecipe(id: number) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}

function openPost(id: number) {
  uni.navigateTo({ url: `/pages/community/detail?id=${id}` })
}

function recipeCover(item: RecipeListItem) {
  return mediaIdToRawUrl(item.coverMediaId)
}

function postCover(item: PostItem) {
  return mediaIdsToUrls(item.mediaIds)[0] || ''
}

function statusLabel(item: RecipeListItem | PostItem) {
  const recipe = item as RecipeListItem
  const post = item as PostItem
  const status = recipe.reviewStatus || recipe.publishStatus || post.status
  const map: Record<string, string> = {
    draft: '草稿',
    pending_review: '审核中',
    published: '已发布',
    rejected: '已驳回',
    withdrawn: '已撤回',
  }
  return map[String(status || '')] || '未发布'
}

onMounted(loadData)
</script>

<template>
  <view class="page-body works-page">
    <AppNavBar title="我的作品" back />

    <view class="tab-row">
      <view class="tab-row__item" :class="{ active: active === 'recipe' }" @click="switchTab('recipe')">菜谱</view>
      <view class="tab-row__item" :class="{ active: active === 'post' }" @click="switchTab('post')">动态</view>
    </view>

    <view v-if="loading" class="loading-text">加载中...</view>

    <view v-else-if="active === 'recipe'" class="work-list">
      <view v-for="item in recipes" :key="item.id" class="surface-card work-card" @click="openRecipe(item.id)">
        <view class="work-card__cover">
          <image v-if="recipeCover(item)" class="work-card__image" :src="recipeCover(item)" mode="aspectFill" />
          <text v-else>🍳</text>
        </view>
        <view class="work-card__body">
          <view class="work-card__title">{{ item.title || '未命名菜谱' }}</view>
          <view class="work-card__desc">{{ item.intro || '暂未填写简介' }}</view>
          <view class="work-card__meta">
            <text>{{ statusLabel(item) }}</text>
            <text>♥ {{ item.likeCount || 0 }}</text>
            <text>☆ {{ item.favoriteCount || 0 }}</text>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="work-list">
      <view v-for="item in posts" :key="item.id" class="surface-card work-card" @click="openPost(item.id)">
        <view class="work-card__cover">
          <image v-if="postCover(item)" class="work-card__image" :src="postCover(item)" mode="aspectFill" />
          <text v-else>📸</text>
        </view>
        <view class="work-card__body">
          <view class="work-card__title">{{ item.content || '未填写动态内容' }}</view>
          <view class="work-card__desc">{{ formatTime(item.publishedAt || item.createdAt) }}</view>
          <view class="work-card__meta">
            <text>{{ statusLabel(item) }}</text>
            <text>♥ {{ item.likeCount || 0 }}</text>
            <text>💬 {{ item.commentCount || 0 }}</text>
          </view>
        </view>
      </view>
    </view>

    <EmptyState
      v-if="!loading && rowsEmpty"
      :title="active === 'recipe' ? '暂无菜谱作品' : '暂无动态作品'"
      :description="active === 'recipe' ? '发布菜谱后会展示在这里。' : '发布动态后会展示在这里。'"
    />
  </view>
</template>

<style scoped lang="scss">
.works-page {
  padding-bottom: 36rpx;
}

.tab-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin: 22rpx 28rpx 0;
  padding: 8rpx;
  border-radius: 18rpx;
  background: var(--app-surface-muted);
}

.tab-row__item {
  height: 64rpx;
  border-radius: 14rpx;
  color: var(--app-text-muted);
  font-size: 25rpx;
  line-height: 64rpx;
  text-align: center;
}

.tab-row__item.active {
  background: #fff;
  color: var(--app-primary);
  font-weight: 700;
  box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.08);
}

.work-list {
  display: grid;
  gap: 18rpx;
  padding: 22rpx 28rpx;
}

.work-card {
  display: flex;
  overflow: hidden;
}

.work-card__cover {
  width: 178rpx;
  min-height: 178rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 54rpx;
  flex-shrink: 0;
}

.work-card__image {
  width: 100%;
  height: 100%;
}

.work-card__body {
  flex: 1;
  min-width: 0;
  padding: 22rpx;
}

.work-card__title {
  color: var(--app-text);
  font-size: 28rpx;
  font-weight: 700;
  line-height: 1.45;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.work-card__desc {
  margin-top: 10rpx;
  color: var(--app-text-muted);
  font-size: 23rpx;
  line-height: 1.55;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.work-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 14rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.loading-text {
  padding: 42rpx;
  color: var(--app-text-muted);
  text-align: center;
}
</style>
