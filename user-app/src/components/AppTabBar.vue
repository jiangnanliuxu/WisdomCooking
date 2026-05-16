<script setup lang="ts">
const props = defineProps<{
  current: 'home' | 'community' | 'ai' | 'message' | 'profile'
}>()

const tabs = [
  { key: 'home', label: '首页', icon: '⌂', url: '/pages/home/index' },
  { key: 'community', label: '社区', icon: '♧', url: '/pages/community/index' },
  { key: 'ai', label: 'AI', icon: '✦', url: '/pages/ai/index' },
  { key: 'message', label: '消息', icon: '◔', url: '/pages/message/index' },
  { key: 'profile', label: '我的', icon: '○', url: '/pages/profile/index' },
]

function switchPage(url: string) {
  uni.reLaunch({ url })
}
</script>

<template>
  <view class="tab-bar">
    <view v-for="tab in tabs" :key="tab.key" class="tab-item" :class="{ active: props.current === tab.key }" @click="switchPage(tab.url)">
      <text class="tab-icon">{{ tab.icon }}</text>
      <text>{{ tab.label }}</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 80;
  max-width: 860rpx;
  margin: 0 auto;
  display: flex;
  padding: 12rpx 18rpx calc(env(safe-area-inset-bottom) + 14rpx);
  background: rgba(255, 252, 248, 0.92);
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(20rpx);
  box-shadow: 0 -10rpx 30rpx rgba(22, 28, 45, 0.06);
}

.tab-item {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5rpx;
  height: 88rpx;
  border-radius: 24rpx;
  font-size: 21rpx;
  color: var(--app-text-muted);
  transition: color 0.18s ease, background 0.18s ease;
}

.tab-icon {
  width: 42rpx;
  height: 42rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  line-height: 42rpx;
}

.active {
  color: var(--app-primary-strong);
  background: var(--app-primary-soft);
  font-weight: 700;
}

.active .tab-icon {
  color: #fff;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  box-shadow: 0 8rpx 18rpx rgba(232, 109, 47, 0.24);
}
</style>
