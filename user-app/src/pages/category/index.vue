<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppNavBar from '@/components/AppNavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { listCategories } from '@/api/recipe'
import type { CategoryGroup } from '@/types/cook'

const groups = ref<CategoryGroup[]>([])

async function loadData() {
  try {
    const response = await listCategories()
    groups.value = response.data || []
  }
  catch {
    groups.value = []
  }
}

function goSearch(code: string, name: string) {
  uni.navigateTo({ url: `/pages/search/index?keyword=${encodeURIComponent(name)}&categoryCode=${encodeURIComponent(code)}` })
}

onMounted(loadData)
</script>

<template>
  <view class="page-body category-page">
    <AppNavBar title="菜谱分类" back />
    <view class="category-hero">
      <view class="category-hero__title">按分类找到今天的灵感</view>
      <view class="category-hero__desc">覆盖中华菜系、场景分类和更多分类，后台新增后会同步展示。</view>
    </view>

    <view v-if="groups.length" class="category-sections">
      <view v-for="group in groups" :key="group.code" class="surface-card category-section">
        <view class="section-head compact">
          <view>
            <view class="section-title">{{ group.name }}</view>
            <view class="section-caption">{{ group.children.length }} 个分类</view>
          </view>
        </view>
        <view class="category-grid">
          <view v-for="item in group.children" :key="item.code" class="category-item" @click="goSearch(item.code, item.name)">
            <view class="category-icon" :style="{ background: item.color || '#fff7e6' }">{{ item.icon || '🏷️' }}</view>
            <view class="category-name">{{ item.name }}</view>
          </view>
        </view>
      </view>
    </view>
    <EmptyState v-else title="暂无分类" description="分类数据尚未返回，请稍后重试。" />
  </view>
</template>

<style scoped lang="scss">
.category-hero {
  margin: 24rpx 28rpx 0;
  padding: 30rpx;
  border-radius: 30rpx;
  color: #fff;
  background: linear-gradient(135deg, #e86d2f, #ff9b54 58%, #2d8768);
  box-shadow: var(--app-shadow-lg);
}

.category-hero__title {
  font-size: 36rpx;
  font-weight: 800;
}

.category-hero__desc {
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.6;
  opacity: 0.92;
}

.category-sections {
  display: grid;
  gap: 20rpx;
  padding: 24rpx 28rpx 28rpx;
}

.category-section {
  padding: 26rpx;
}

.compact {
  margin: 0 0 20rpx;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20rpx 12rpx;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10rpx;
  text-align: center;
}

.category-icon {
  width: 78rpx;
  height: 78rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
}

.category-name {
  color: var(--app-text);
  font-size: 23rpx;
}
</style>
