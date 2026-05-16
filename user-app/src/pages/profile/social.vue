<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { followUser, listMyFollowers, listMyFollowing, unfollowUser } from '@/api/user'
import type { UserPublicProfile } from '@/types/cook'
import { resolveAssetUrl } from '@/utils/media'

const active = ref<'followers' | 'following'>('followers')
const loading = ref(false)
const rows = ref<UserPublicProfile[]>([])

const pageTitle = computed(() => active.value === 'followers' ? '我的粉丝' : '我的关注')
const emptyTitle = computed(() => active.value === 'followers' ? '暂无粉丝' : '暂无关注')
const emptyDesc = computed(() => active.value === 'followers' ? '有人关注你后会展示在这里。' : '关注感兴趣的作者后会展示在这里。')

async function loadData() {
  loading.value = true
  try {
    const response = active.value === 'followers'
      ? await listMyFollowers({ page: '1', pageSize: '50' })
      : await listMyFollowing({ page: '1', pageSize: '50' })
    rows.value = response.data?.items || []
  }
  catch {
    rows.value = []
  }
  finally {
    loading.value = false
  }
}

function switchTab(tab: 'followers' | 'following') {
  active.value = tab
  loadData()
}

async function toggleFollow(item: UserPublicProfile) {
  if (item.followed) {
    await unfollowUser(item.id)
  }
  else {
    await followUser(item.id)
  }
  await loadData()
}

onLoad((options) => {
  active.value = options?.tab === 'following' ? 'following' : 'followers'
})

onMounted(loadData)
</script>

<template>
  <view class="page-body social-page">
    <AppNavBar :title="pageTitle" back />

    <view class="tab-row">
      <view class="tab-row__item" :class="{ active: active === 'followers' }" @click="switchTab('followers')">粉丝</view>
      <view class="tab-row__item" :class="{ active: active === 'following' }" @click="switchTab('following')">关注</view>
    </view>

    <view v-if="loading" class="loading-text">加载中...</view>

    <view v-else-if="rows.length" class="user-list">
      <view v-for="item in rows" :key="item.id" class="surface-card user-card">
        <view class="user-card__avatar">
          <image v-if="resolveAssetUrl(item.avatarUrl || '')" class="user-card__avatar-image" :src="resolveAssetUrl(item.avatarUrl || '')" mode="aspectFill" />
          <text v-else>{{ item.nickname?.slice(0, 1) || '厨' }}</text>
        </view>
        <view class="user-card__body">
          <view class="user-card__name">{{ item.nickname || `用户${item.id}` }}</view>
          <view class="user-card__meta">{{ item.recipeCount || 0 }} 菜谱 · {{ item.postCount || 0 }} 动态 · {{ item.followerCount || 0 }} 粉丝</view>
          <view class="user-card__bio">{{ item.bio || '这个人还没有填写简介。' }}</view>
        </view>
        <button class="follow-btn" :class="{ active: item.followed }" @click="toggleFollow(item)">
          {{ item.followed ? '已关注' : '关注' }}
        </button>
      </view>
    </view>

    <EmptyState v-else :title="emptyTitle" :description="emptyDesc" />
  </view>
</template>

<style scoped lang="scss">
.social-page {
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

.user-list {
  display: grid;
  gap: 18rpx;
  padding: 22rpx 28rpx;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 24rpx;
}

.user-card__avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 34rpx;
  font-weight: 800;
  flex-shrink: 0;
  overflow: hidden;
}

.user-card__avatar-image {
  width: 100%;
  height: 100%;
}

.user-card__body {
  flex: 1;
  min-width: 0;
}

.user-card__name {
  color: var(--app-text);
  font-size: 28rpx;
  font-weight: 700;
}

.user-card__meta,
.user-card__bio {
  margin-top: 8rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
  line-height: 1.55;
}

.user-card__bio {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 1;
}

.follow-btn {
  width: 112rpx;
  height: 56rpx;
  padding: 0;
  border-radius: 999rpx;
  background: var(--app-primary);
  color: #fff;
  font-size: 22rpx;
  line-height: 56rpx;
  flex-shrink: 0;
}

.follow-btn::after {
  border: 0;
}

.follow-btn.active {
  background: var(--app-surface-muted);
  color: var(--app-text-muted);
}

.loading-text {
  padding: 42rpx;
  color: var(--app-text-muted);
  text-align: center;
}
</style>
