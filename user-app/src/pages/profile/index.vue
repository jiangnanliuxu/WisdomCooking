<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import AppTabBar from '@/components/AppTabBar.vue'
import { logout } from '@/api/auth'
import { getUserPublicProfile } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { resolveAssetUrl } from '@/utils/media'
import type { UserPublicProfile } from '@/types/cook'

const authStore = useAuthStore()
const publicProfile = ref<UserPublicProfile | null>(null)

const profileName = computed(() => authStore.profile?.nickname || (authStore.isLoggedIn ? '美食爱好者' : '未登录用户'))
const profileBio = computed(() => authStore.profile?.bio || (authStore.isLoggedIn ? '还没有填写个性签名。' : '登录后可同步你的资料、收藏和互动记录。'))
const profileAvatar = computed(() => resolveAssetUrl(authStore.profile?.avatarUrl || ''))

const stats = computed(() => {
  if (publicProfile.value) {
    return {
      works: (publicProfile.value.recipeCount || 0) + (publicProfile.value.postCount || 0),
      fans: publicProfile.value.followerCount || 0,
      following: publicProfile.value.followingCount || 0,
      likes: parseCachedStats().likes,
    }
  }
  return parseCachedStats()
})

function parseCachedStats() {
  try {
    const parsed = authStore.profile?.statsJson ? JSON.parse(authStore.profile.statsJson) : {}
    const defaults = { works: 0, fans: 0, following: 0, likes: 0 }
    return {
      works: parsed.recipeCount ?? parsed.works ?? defaults.works,
      fans: parsed.followerCount ?? parsed.fans ?? defaults.fans,
      following: parsed.followingCount ?? parsed.following ?? defaults.following,
      likes: parsed.likeCount ?? parsed.likes ?? defaults.likes,
    }
  }
  catch {
    return { works: 0, fans: 0, following: 0, likes: 0 }
  }
}

const menus = [
  { icon: '📝', label: '发布菜谱', url: '/pages/recipe/form' },
  { icon: '📸', label: '发布动态', url: '/pages/community/form' },
  { icon: '📋', label: '我的动态', url: '/pages/community/mine' },
  { icon: '📖', label: '我的菜谱', url: '/pages/recipe/mine' },
  { icon: '⭐', label: '我的收藏', url: '/pages/profile/favorites' },
  { icon: '♥', label: '我的点赞', url: '/pages/profile/likes' },
  { icon: '🏆', label: '打卡记录', url: '/pages/profile/checkins' },
]

const supportMenus = [
  { icon: '⚙️', label: '编辑资料', url: '/pages/profile/edit' },
  { icon: '📢', label: '问题反馈', url: '/pages/profile/feedback' },
]

async function loadData() {
  if (authStore.isLoggedIn) {
    try {
      await authStore.refreshProfile()
      if (authStore.profile?.id) {
        const response = await getUserPublicProfile(authStore.profile.id)
        publicProfile.value = response.data || null
      }
    }
    catch {
      // Keep cached or preview profile visible when the API is unavailable.
    }
  }
}

function go(url: string) {
  uni.navigateTo({ url })
}

function openStat(type: 'works' | 'fans' | 'following') {
  if (!authStore.isLoggedIn) {
    go('/pages/auth/login')
    return
  }
  if (type === 'works') {
    go('/pages/profile/works')
    return
  }
  go(`/pages/profile/social?tab=${type === 'fans' ? 'followers' : 'following'}`)
}

async function handleLogout() {
  if (authStore.isLoggedIn) {
    try {
      await logout()
    }
    finally {
      authStore.logout()
    }
  }
  else {
    go('/pages/auth/login')
  }
}

onShow(loadData)
</script>

<template>
  <view class="page-body profile-page">
    <view class="profile-header">
      <view class="settings" @click="go('/pages/profile/edit')">⚙️</view>
      <view class="profile-info">
        <view class="profile-avatar">
          <image v-if="profileAvatar" class="profile-avatar__image" :src="profileAvatar" mode="aspectFill" />
          <template v-else>😊</template>
        </view>
        <view class="profile-detail">
          <view class="name">{{ profileName }}</view>
          <view class="bio">{{ profileBio }}</view>
        </view>
      </view>
      <view class="profile-stats">
        <view class="stat-item" @click="openStat('works')">
          <view class="num">{{ stats.works }}</view>
          <view class="label">作品</view>
        </view>
        <view class="stat-item" @click="openStat('fans')">
          <view class="num">{{ stats.fans }}</view>
          <view class="label">粉丝</view>
        </view>
        <view class="stat-item" @click="openStat('following')">
          <view class="num">{{ stats.following }}</view>
          <view class="label">关注</view>
        </view>
        <view class="stat-item">
          <view class="num">{{ stats.likes }}</view>
          <view class="label">获赞</view>
        </view>
      </view>
    </view>

    <view class="profile-actions">
      <button class="primary-button" @click="go('/pages/recipe/form')">发布菜谱</button>
      <button class="ghost-button" @click="go('/pages/auth/login')">{{ authStore.isLoggedIn ? '切换账号' : '登录 / 注册' }}</button>
    </view>

    <view class="menu-section surface-card">
      <view v-for="item in menus" :key="item.url" class="menu-item" @click="go(item.url)">
        <text class="icon">{{ item.icon }}</text>
        <text class="label">{{ item.label }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="menu-section surface-card">
      <view v-for="item in supportMenus" :key="item.url" class="menu-item" @click="go(item.url)">
        <text class="icon">{{ item.icon }}</text>
        <text class="label">{{ item.label }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item danger" @click="handleLogout">
        <text class="icon">↪</text>
        <text class="label">{{ authStore.isLoggedIn ? '退出登录' : '去登录' }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <AppTabBar current="profile" />
  </view>
</template>

<style scoped lang="scss">
.profile-page {
  padding-top: 1rpx;
}

.profile-header {
  position: relative;
  padding: 96rpx 36rpx 36rpx;
  color: #fff;
  background: linear-gradient(135deg, #e86d2f, #ff9b54 56%, #2d8768);
  overflow: hidden;
}

.profile-header::after {
  content: '';
  position: absolute;
  right: -90rpx;
  top: -110rpx;
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  border: 48rpx solid rgba(255, 255, 255, 0.12);
}

.settings {
  position: absolute;
  right: 32rpx;
  top: 52rpx;
  z-index: 2;
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.18);
  font-size: 28rpx;
}

.profile-info {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.profile-avatar {
  width: 132rpx;
  height: 132rpx;
  border-radius: 50%;
  border: 6rpx solid rgba(255, 255, 255, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.28);
  font-size: 58rpx;
}

.profile-avatar__image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.profile-detail {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 38rpx;
  font-weight: 800;
  letter-spacing: 0;
}

.bio {
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.55;
  opacity: 0.92;
}

.profile-stats {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18rpx;
  margin-top: 32rpx;
}

.stat-item {
  text-align: center;
  border-radius: 16rpx;
  transition: background 0.16s ease, transform 0.16s ease;
}

.stat-item:active {
  background: rgba(255, 255, 255, 0.14);
  transform: scale(0.98);
}

.num {
  font-size: 32rpx;
  font-weight: 900;
}

.label {
  margin-top: 4rpx;
  font-size: 21rpx;
  opacity: 0.82;
}

.profile-actions {
  display: flex;
  gap: 16rpx;
  padding: 24rpx 28rpx 4rpx;
}

.profile-actions button {
  flex: 1;
}

.menu-section {
  margin: 20rpx 28rpx 0;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: 94rpx;
  padding: 0 24rpx;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.menu-item:last-child {
  border-bottom: 0;
}

.menu-item .icon {
  width: 44rpx;
  color: var(--app-primary);
  font-size: 30rpx;
  text-align: center;
}

.menu-item .label {
  flex: 1;
  margin: 0;
  color: var(--app-text);
  font-size: 27rpx;
  opacity: 1;
}

.arrow {
  color: var(--app-text-muted);
  font-size: 34rpx;
}

.danger .label,
.danger .icon {
  color: var(--app-danger);
}
</style>
