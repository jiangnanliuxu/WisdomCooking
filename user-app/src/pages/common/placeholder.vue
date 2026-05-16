<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import AppNavBar from '@/components/AppNavBar.vue'
import EmptyState from '@/components/EmptyState.vue'

const title = ref('功能占位')
const desc = ref('该页面后端接口尚未开放，当前只提供稳定占位态。')

onLoad((options) => {
  title.value = String(options?.title || title.value)
  desc.value = String(options?.desc || desc.value)
})
</script>

<template>
  <view class="page-body">
    <AppNavBar :title="title" back />
    <view style="padding: 24rpx;">
      <EmptyState :title="title" :description="desc" />
    </view>
  </view>
</template>
