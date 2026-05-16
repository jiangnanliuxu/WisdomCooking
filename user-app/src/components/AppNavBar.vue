<script setup lang="ts">
defineProps<{
  title: string
  back?: boolean
}>()

function goBack() {
  uni.navigateBack()
}
</script>

<template>
  <view class="nav-bar">
    <view v-if="back" class="nav-back" @click="goBack">‹</view>
    <view v-else class="nav-back nav-back--placeholder" />
    <view class="nav-title">{{ title }}</view>
    <view class="nav-back nav-back--placeholder" />
  </view>
</template>

<style scoped lang="scss">
.nav-bar {
  position: sticky;
  top: 0;
  z-index: 40;
  height: 96rpx;
  padding: 0 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 252, 248, 0.9);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.nav-back {
  width: 60rpx;
  height: 60rpx;
  border-radius: 999rpx;
  background: #fff;
  box-shadow: var(--app-shadow-sm);
  font-size: 46rpx;
  line-height: 54rpx;
  color: var(--app-text-soft);
  text-align: center;
}

.nav-back--placeholder {
  opacity: 0;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: 0;
}
</style>
