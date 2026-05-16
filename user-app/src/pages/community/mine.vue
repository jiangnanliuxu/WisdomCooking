<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/EmptyState.vue'
import { deletePost, listMyPosts, submitPost, withdrawPost } from '@/api/post'
import type { PostItem } from '@/types/cook'
import { formatTime, requireLogin } from '@/utils/format'
import { mediaIdsToUrls } from '@/utils/media'

const status = ref('')
const rows = ref<PostItem[]>([])

const statusOptions = [
  { label: '全部', value: '' },
  { label: '审核中', value: 'pending_review' },
  { label: '已通过', value: 'published' },
  { label: '已驳回', value: 'rejected' },
]

const displayRows = computed(() => rows.value)

async function loadData() {
  if (!requireLogin()) return
  try {
    const response = await listMyPosts({ status: status.value, page: '1', pageSize: '20' })
    rows.value = response.data?.items || []
  }
  catch {
    rows.value = []
  }
}

function openForm() {
  uni.navigateTo({ url: '/pages/community/form' })
}

function goBack() {
  uni.navigateBack()
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/community/detail?id=${id}` })
}

function editPost(id: number) {
  uni.navigateTo({ url: `/pages/community/form?id=${id}` })
}

async function doSubmit(id: number) {
  await submitPost(id)
  await loadData()
}

async function doWithdraw(id: number) {
  await withdrawPost(id)
  await loadData()
}

async function removePost(id: number) {
  await deletePost(id)
  await loadData()
}

function statusLabel(item: PostItem) {
  return item.status === 'published' ? '已通过' : item.status === 'pending_review' ? '审核中' : item.status === 'rejected' ? '已驳回' : '草稿'
}

function statusClass(item: PostItem) {
  return item.status === 'published' ? 'approved' : item.status === 'pending_review' ? 'pending' : item.status === 'rejected' ? 'rejected' : 'draft'
}

function displayTime(item: PostItem) {
  const text = formatTime(item.publishedAt || item.createdAt)
  return text === '-' ? '刚刚' : text
}

function imageUrls(item: PostItem) {
  return mediaIdsToUrls(item.mediaIds).slice(0, 3)
}

function recipeText(item: PostItem) {
  if (item.relatedRecipeTitle) return `关联菜谱：${item.relatedRecipeTitle}`
  return item.relatedRecipeId ? `关联菜谱 #${item.relatedRecipeId}` : '无关联菜谱'
}

onMounted(loadData)
</script>

<template>
  <view class="page-body my-posts-page">
    <view class="page-nav">
      <view class="page-nav__left">
        <view class="page-nav__back" @click="goBack">‹</view>
        <view class="page-nav__title">我的动态</view>
        <view class="page-nav__count">共{{ displayRows.length }}条</view>
      </view>
      <view class="page-nav__add" @click="openForm">+ 发布</view>
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
      <view v-for="item in displayRows" :key="item.id" class="surface-card post-card">
        <view class="post-card__status">
          <view class="status-pill" :class="statusClass(item)">{{ statusLabel(item) }}</view>
          <view class="post-card__time">{{ displayTime(item) }}</view>
        </view>

        <view class="post-card__body">
          <view class="post-card__text">{{ item.content }}</view>
          <view v-if="imageUrls(item).length" class="post-card__images" :class="`post-card__images--${imageUrls(item).length}`">
            <image
              v-for="url in imageUrls(item)"
              :key="url"
              class="post-card__image"
              :src="url"
              mode="aspectFill"
            />
          </view>
          <view class="post-card__meta">
            <text>{{ recipeText(item) }}</text>
            <text>❤️ {{ item.likeCount || 0 }} · 💬 {{ item.commentCount || 0 }}</text>
          </view>
        </view>

        <view v-if="item.status === 'rejected' && item.blockReason" class="reject-box">
          <view class="reject-box__title">⚠️ 驳回原因</view>
          <view class="reject-box__text">{{ item.blockReason }}</view>
        </view>

        <view class="post-card__actions">
          <template v-if="item.status === 'published'">
            <view class="action primary" @click="openDetail(item.id)">查看</view>
            <view class="action" @click="editPost(item.id)">编辑</view>
            <view class="action danger" @click="removePost(item.id)">删除</view>
          </template>
          <template v-else-if="item.status === 'pending_review'">
            <view class="action" @click="openDetail(item.id)">预览</view>
            <view class="action" @click="editPost(item.id)">编辑</view>
            <view class="action danger" @click="doWithdraw(item.id)">撤回</view>
          </template>
          <template v-else>
            <view class="action" @click="openDetail(item.id)">预览</view>
            <view class="action primary" @click="editPost(item.id)">修改重发</view>
            <view class="action danger" @click="removePost(item.id)">删除</view>
          </template>
        </view>
      </view>
    </view>

    <EmptyState v-else title="还没有动态" description="发布第一条做饭记录，或者先去社区看看别人的分享。" />
  </view>
</template>

<style scoped lang="scss">
.my-posts-page {
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

.post-card__status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 22rpx 0;
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

.post-card__time {
  font-size: 20rpx;
  color: var(--app-text-muted);
}

.post-card__body {
  padding: 18rpx 22rpx 22rpx;
}

.post-card__text {
  color: var(--app-text);
  font-size: 24rpx;
  line-height: 1.7;
}

.post-card__images {
  display: grid;
  gap: 8rpx;
  margin-top: 14rpx;
  overflow: hidden;
  border-radius: 16rpx;
}

.post-card__images--1 {
  grid-template-columns: 1fr;
}

.post-card__images--2 {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.post-card__images--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.post-card__image {
  width: 100%;
  height: 100%;
  aspect-ratio: 1;
  background: var(--app-surface-muted);
}

.post-card__images--1 .post-card__image {
  aspect-ratio: 16 / 9;
}

.post-card__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-top: 14rpx;
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.reject-box {
  margin: 0 22rpx 12rpx;
  padding: 14rpx 16rpx;
  border-radius: 16rpx;
  background: #fff2f0;
}

.reject-box__title {
  color: #ff4d4f;
  font-size: 22rpx;
  font-weight: 700;
}

.reject-box__text {
  margin-top: 6rpx;
  color: var(--app-text-soft);
  font-size: 21rpx;
  line-height: 1.65;
}

.post-card__actions {
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
