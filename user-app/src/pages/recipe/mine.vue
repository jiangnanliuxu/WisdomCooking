<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { deleteRecipe, listMyRecipes, submitRecipe, withdrawRecipe } from '@/api/recipe'
import type { RecipeListItem } from '@/types/cook'
import { requireLogin } from '@/utils/format'

const status = ref('')
const rows = ref<RecipeListItem[]>([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '审核中', value: 'pending_review' },
  { label: '已通过', value: 'published' },
  { label: '已驳回', value: 'rejected' },
]

const gradients = [
  'linear-gradient(135deg,#ff9a9e,#fecfef)',
  'linear-gradient(135deg,#84fab0,#8fd3f4)',
  'linear-gradient(135deg,#fbc2eb,#a6c1ee)',
  'linear-gradient(135deg,#f6d365,#fda085)',
]
const icons = ['🐟', '🥗', '🌶️', '🍰']

const displayRows = computed(() => rows.value)

async function loadData() {
  if (!requireLogin()) return
  try {
    const response = await listMyRecipes({ status: status.value, page: '1', pageSize: '20' })
    rows.value = response.data?.items || []
  }
  catch {
    rows.value = []
  }
}

function openForm() {
  uni.navigateTo({ url: '/pages/recipe/form' })
}

function goBack() {
  uni.navigateBack()
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/recipe/detail?id=${id}` })
}

function editRecipe(id: number) {
  uni.navigateTo({ url: `/pages/recipe/form?id=${id}` })
}

async function handleSubmit(id: number) {
  await submitRecipe(id)
  await loadData()
}

async function handleWithdraw(id: number) {
  await withdrawRecipe(id)
  await loadData()
}

async function handleDelete(id: number) {
  await deleteRecipe(id)
  await loadData()
}

function recipeStatus(item: RecipeListItem) {
  return item.publishStatus || item.reviewStatus || 'draft'
}

function statusLabel(item: RecipeListItem) {
  const value = recipeStatus(item)
  return value === 'published' ? '已通过' : value === 'pending_review' ? '审核中' : value === 'rejected' ? '已驳回' : '草稿'
}

function statusClass(item: RecipeListItem) {
  const value = recipeStatus(item)
  return value === 'published' ? 'approved' : value === 'pending_review' ? 'pending' : value === 'rejected' ? 'rejected' : 'draft'
}

onMounted(loadData)
</script>

<template>
  <view class="page-body my-recipe-page">
    <view class="page-nav">
      <view class="page-nav__left">
        <view class="page-nav__back" @click="goBack">‹</view>
        <view class="page-nav__title">我的菜谱</view>
        <view class="page-nav__count">共{{ displayRows.length }}个</view>
      </view>
      <view class="page-nav__add" @click="openForm">+ 发布菜谱</view>
    </view>

    <view class="tab-row">
      <view
        v-for="item in statusOptions"
        :key="item.value"
        class="tab-row__item"
        :class="{ active: status === item.value }"
        @click="status = item.value; loadData()"
      >
        {{ item.label }}
      </view>
    </view>

    <view v-if="displayRows.length" class="card-list">
      <view v-for="(item, index) in displayRows" :key="item.id" class="surface-card recipe-card">
        <view class="recipe-card__top">
          <view class="recipe-card__thumb" :style="{ background: gradients[index % gradients.length] }">{{ icons[index % icons.length] }}</view>
          <view class="recipe-card__info">
            <view>
              <view class="recipe-card__name">{{ item.title }}</view>
              <view class="recipe-card__meta">
                <text>{{ item.categoryCode || '家常菜' }}</text>
                <text>{{ item.difficulty || '中等' }}</text>
                <text>{{ item.cookTime || '30分钟' }}</text>
              </view>
            </view>
            <view class="recipe-card__bottom">
              <view class="recipe-card__stats">❤️ {{ item.likeCount || 0 }} · ⭐ {{ item.favoriteCount || 0 }}</view>
              <view class="status-pill" :class="statusClass(item)">{{ statusLabel(item) }}</view>
            </view>
          </view>
        </view>

        <view v-if="recipeStatus(item) === 'rejected' && item.rejectReason" class="reject-box">{{ item.rejectReason }}</view>

        <view class="recipe-card__actions">
          <template v-if="recipeStatus(item) === 'published'">
            <view class="action primary" @click="openDetail(item.id)">查看</view>
            <view class="action" @click="editRecipe(item.id)">编辑</view>
            <view class="action danger" @click="handleDelete(item.id)">删除</view>
          </template>
          <template v-else-if="recipeStatus(item) === 'pending_review'">
            <view class="action" @click="openDetail(item.id)">预览</view>
            <view class="action" @click="editRecipe(item.id)">编辑</view>
            <view class="action danger" @click="handleWithdraw(item.id)">撤回</view>
          </template>
          <template v-else>
            <view class="action" @click="openDetail(item.id)">预览</view>
            <view class="action primary" @click="editRecipe(item.id)">修改重提</view>
            <view class="action danger" @click="handleDelete(item.id)">删除</view>
          </template>
        </view>
      </view>
    </view>

    <EmptyState v-else title="暂无菜谱" description="先去发布你的第一道菜谱。" />
  </view>
</template>

<style scoped lang="scss">
.my-recipe-page {
  padding-bottom: 28rpx;
}

.page-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 28rpx;
  background: rgba(255, 252, 248, 0.92);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.page-nav__left {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.page-nav__back {
  font-size: 42rpx;
  color: var(--app-text-soft);
}

.page-nav__title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--app-text);
}

.page-nav__count {
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.page-nav__add {
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 22rpx;
}

.tab-row {
  display: flex;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.tab-row__item {
  flex: 1;
  position: relative;
  padding: 22rpx 0;
  text-align: center;
  color: var(--app-text-muted);
  font-size: 24rpx;
}

.tab-row__item.active {
  color: var(--app-primary);
  font-weight: 700;
}

.tab-row__item.active::after {
  content: '';
  position: absolute;
  right: 24%;
  bottom: 0;
  left: 24%;
  height: 6rpx;
  border-radius: 999rpx;
  background: var(--app-primary);
}

.card-list {
  display: grid;
  gap: 18rpx;
  padding: 18rpx 28rpx 0;
}

.recipe-card__top {
  display: flex;
}

.recipe-card__thumb {
  width: 196rpx;
  min-height: 196rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.92);
  font-size: 64rpx;
}

.recipe-card__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 22rpx;
}

.recipe-card__name {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--app-text);
  line-height: 1.5;
}

.recipe-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-top: 10rpx;
}

.recipe-card__meta text {
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  background: var(--app-surface-muted);
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.recipe-card__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.recipe-card__stats {
  color: var(--app-text-muted);
  font-size: 21rpx;
}

.status-pill {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 600;
}

.status-pill.approved {
  background: #f6ffed;
  color: #52c41a;
}

.status-pill.pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-pill.rejected {
  background: #fff2f0;
  color: #ff4d4f;
}

.status-pill.draft {
  background: var(--app-surface-muted);
  color: var(--app-text-soft);
}

.reject-box {
  margin: 0 22rpx 14rpx;
  padding: 12rpx 14rpx;
  border-radius: 14rpx;
  background: #fff2f0;
  color: var(--app-danger);
  font-size: 21rpx;
  line-height: 1.65;
}

.recipe-card__actions {
  display: flex;
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.action {
  flex: 1;
  padding: 18rpx 0;
  text-align: center;
  color: var(--app-text-soft);
  font-size: 24rpx;
  border-right: 1rpx solid rgba(15, 23, 42, 0.05);
}

.action:last-child {
  border-right: 0;
}

.action.primary {
  color: var(--app-primary);
}

.action.danger {
  color: var(--app-danger);
}
</style>
