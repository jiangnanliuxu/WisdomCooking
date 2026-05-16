<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppTabBar from '@/components/AppTabBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import PostCard from '@/components/PostCard.vue'
import { favoritePost, likePost, listPosts, unfavoritePost, unlikePost } from '@/api/post'
import { followUser, unfollowUser } from '@/api/user'
import type { PostItem } from '@/types/cook'
import { CHINESE_CUISINES } from '@/constants/cuisine'
import { requireLogin } from '@/utils/format'

const activeFilter = ref('')
const rows = ref<PostItem[]>([])
const loading = ref(false)
const cuisineTabs = [
  { code: '', label: '推荐' },
  { code: 'following', label: '关注' },
  ...CHINESE_CUISINES,
  { code: 'other', label: '其他' },
]

async function loadData() {
  if (activeFilter.value === 'following') {
    loading.value = false
    rows.value = []
    return
  }
  loading.value = true
  try {
    const params: Record<string, string> = { page: '1', pageSize: '20' }
    if (activeFilter.value) {
      params.recipeCategoryCode = activeFilter.value
    }
    const postRes = await listPosts(params)
    rows.value = postRes.data?.items || []
  }
  catch {
    rows.value = []
  }
  finally {
    loading.value = false
  }
}

async function doLike(id: number) {
  if (!requireLogin()) return
  const current = rows.value.find(item => item.id === id)
  if (current?.liked) {
    await unlikePost(id)
  }
  else {
    await likePost(id)
  }
  await loadData()
}

async function doFavorite(id: number) {
  if (!requireLogin()) return
  const current = rows.value.find(item => item.id === id)
  if (current?.favorited) {
    await unfavoritePost(id)
  }
  else {
    await favoritePost(id)
  }
  await loadData()
}

async function doFollow(item: PostItem) {
  if (!requireLogin()) return
  if (!item.userId) return
  if (item.authorFollowed) {
    await unfollowUser(item.userId)
  }
  else {
    await followUser(item.userId)
  }
  await loadData()
}

function switchFilter(code: string) {
  activeFilter.value = code
  loadData()
}

function openForm() {
  if (!requireLogin()) return
  uni.navigateTo({ url: '/pages/community/form' })
}

onMounted(loadData)
</script>

<template>
  <view class="page-body community-page">
    <view class="app-page-head">
      <view class="app-page-head__row">
        <view>
          <view class="section-title">社区广场</view>
          <view class="section-caption">发现大家的下厨记录和饮食打卡</view>
        </view>
        <view class="icon-action" @click="openForm">＋</view>
      </view>
    </view>

    <view class="topic-tabs">
      <view
        v-for="tab in cuisineTabs"
        :key="tab.code || 'recommend'"
        class="topic-tab"
        :class="{ active: activeFilter === tab.code, 'muted-chip': tab.code === 'following' }"
        @click="switchFilter(tab.code)"
      >
        {{ tab.label }}
      </view>
    </view>

    <view class="feed-list">
      <view v-if="loading" class="loading-text">加载中...</view>
      <template v-else>
        <PostCard
          v-for="(item, index) in rows"
          :key="item.id"
          :item="item"
          :index="index"
          @like="doLike"
          @favorite="doFavorite"
          @follow="doFollow"
        />
      </template>
      <EmptyState
        v-if="!loading && !rows.length"
        title="暂无动态"
        :description="activeFilter === 'following' ? '关注流后端暂未提供，先看看推荐或菜系筛选。' : '发布第一条做饭记录，或者切换到其他菜系看看。'"
      />
    </view>

    <AppTabBar current="community" />
  </view>
</template>

<style scoped lang="scss">
.community-page {
  padding-top: 1rpx;
}

.topic-tabs {
  position: sticky;
  top: 96rpx;
  z-index: 20;
  background: rgba(255, 252, 248, 0.9);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.feed-list {
  display: grid;
  gap: 18rpx;
  padding: 22rpx 28rpx 28rpx;
}

.loading-text {
  padding: 32rpx;
  color: var(--app-text-muted);
  text-align: center;
}
</style>
