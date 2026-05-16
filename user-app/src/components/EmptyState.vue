<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <view class="surface-card empty-box">
    <view class="empty-icon">∅</view>
    <view class="empty-title">{{ title }}</view>
    <view class="empty-desc">{{ description }}</view>
  </view>
</template>

<style scoped lang="scss">
.empty-box {
  padding: 56rpx 32rpx;
  text-align: center;
}

.empty-icon {
  width: 96rpx;
  height: 96rpx;
  margin: 0 auto;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 62rpx;
  font-weight: 300;
}

.empty-title {
  margin-top: 18rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--app-text);
}

.empty-desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: var(--app-text-muted);
}
</style>
