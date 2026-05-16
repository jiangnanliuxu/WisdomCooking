<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppNavBar from '@/components/AppNavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import { createFeedback, listMyFeedbacks, uploadImage } from '@/api/operation'
import type { FeedbackItem } from '@/types/cook'
import { formatTime, requireLogin } from '@/utils/format'
import { mediaIdsToUrls, resolveAssetUrl } from '@/utils/media'

interface UploadPreview {
  id: number
  url: string
}

const feedbackTypes = [
  { label: '功能异常', value: 'bug' },
  { label: '体验问题', value: 'experience' },
  { label: '内容举报', value: 'report' },
  { label: '功能建议', value: 'suggestion' },
  { label: '其他', value: 'other' },
]

const statusText: Record<string, string> = {
  processing: '处理中',
  pending: '待处理',
  resolved: '已解决',
  rejected: '已关闭',
  closed: '已关闭',
}

const feedbackTypeText: Record<string, string> = {
  bug: '功能异常',
  experience: '体验问题',
  report: '内容举报',
  suggestion: '功能建议',
  function: '功能建议',
  other: '其他',
}

const rows = ref<FeedbackItem[]>([])
const uploads = ref<UploadPreview[]>([])
const uploading = ref(false)
const submitting = ref(false)
const form = reactive({
  type: 'bug',
  content: '',
  contact: '',
})

const feedbackTypeLabels = feedbackTypes.map(item => item.label)
const currentTypeIndex = computed(() => Math.max(feedbackTypes.findIndex(item => item.value === form.type), 0))

async function loadData() {
  if (!requireLogin()) return
  const response = await listMyFeedbacks({ page: '1', pageSize: '20' })
  rows.value = response.data?.items || []
}

function changeType(event: { detail: { value: number | string } }) {
  const index = Number(event.detail.value)
  form.type = feedbackTypes[index]?.value || feedbackTypes[0].value
}

function typeLabel(type?: string) {
  return feedbackTypeText[type || ''] || type || '功能建议'
}

function statusLabel(status?: string) {
  return statusText[status || ''] || status || '处理中'
}

function historyImages(item: FeedbackItem) {
  return mediaIdsToUrls(item.mediaIds).slice(0, 4)
}

function chooseScreenshots() {
  if (uploads.value.length >= 4 || uploading.value) return
  uni.chooseImage({
    count: 4 - uploads.value.length,
    success: async ({ tempFilePaths }) => {
      const files = Array.isArray(tempFilePaths) ? tempFilePaths : tempFilePaths ? [tempFilePaths] : []
      if (!files.length) return
      uploading.value = true
      try {
        const uploaded = await Promise.all(files.map(async (filePath: string) => {
          const response = await uploadImage(filePath)
          if (!response.data?.id) return null
          return {
            id: response.data.id,
            url: resolveAssetUrl(response.data.url || ''),
          }
        }))
        uploads.value = [
          ...uploads.value,
          ...uploaded.filter((item: UploadPreview | null): item is UploadPreview => Boolean(item)),
        ].slice(0, 4)
      }
      finally {
        uploading.value = false
      }
    },
  })
}

function removeUpload(index: number) {
  uploads.value.splice(index, 1)
}

async function submitFeedback() {
  if (!form.type) {
    uni.showToast({ title: '请选择反馈类型', icon: 'none' })
    return
  }
  if (!form.content.trim()) {
    uni.showToast({ title: '请填写问题描述', icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await createFeedback({
      type: form.type,
      content: form.content,
      contact: form.contact,
      mediaIds: uploads.value.map(item => item.id),
    })
    form.content = ''
    form.contact = ''
    uploads.value = []
    uni.showToast({ title: '反馈已提交', icon: 'none' })
    await loadData()
  }
  finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <view class="page-body feedback-page">
    <AppNavBar title="问题反馈" back />

    <view class="form-section surface-card">
      <view class="form-group">
        <view class="form-label">反馈类型 <text class="req">*</text></view>
        <picker :range="feedbackTypeLabels" :value="currentTypeIndex" @change="changeType">
          <view class="form-select">
            <text>{{ typeLabel(form.type) }}</text>
            <text class="select-arrow">▾</text>
          </view>
        </picker>
      </view>

      <view class="form-group">
        <view class="form-label">问题描述 <text class="req">*</text></view>
        <textarea
          v-model="form.content"
          class="form-textarea"
          placeholder="请详细描述您遇到的问题或建议，以便我们更好地为您解决"
          maxlength="1000"
        />
      </view>

      <view class="form-group">
        <view class="form-label">上传截图</view>
        <view class="upload-area" @click="chooseScreenshots">
          <view class="upload-icon">📷</view>
          <view class="upload-text">{{ uploading ? '上传中...' : '点击上传截图' }}</view>
          <view class="upload-hint">最多上传4张，支持JPG/PNG</view>
        </view>
        <view v-if="uploads.length" class="uploaded-images">
          <view v-for="(item, index) in uploads" :key="item.id" class="img-item">
            <image :src="item.url" mode="aspectFill" />
            <view class="del" @click.stop="removeUpload(index)">×</view>
          </view>
        </view>
      </view>

      <view class="form-group">
        <view class="form-label">联系方式</view>
        <input v-model="form.contact" class="form-input" placeholder="手机号/邮箱（选填，方便我们联系您）" />
      </view>
    </view>

    <button class="submit-btn" :disabled="submitting || uploading" @click="submitFeedback">
      {{ submitting ? '提交中...' : '提交反馈' }}
    </button>

    <view class="history-section">
      <view class="history-title">反馈记录</view>
      <view v-if="rows.length" class="history-list">
        <view v-for="item in rows" :key="item.id" class="history-card">
          <view class="history-card__top">
            <text class="type">{{ typeLabel(item.type) }}</text>
            <text class="status-tag" :class="item.status || 'processing'">{{ statusLabel(item.status) }}</text>
          </view>
          <view class="content">{{ item.content || '未填写内容' }}</view>
          <view v-if="historyImages(item).length" class="history-images">
            <image v-for="url in historyImages(item)" :key="url" :src="url" mode="aspectFill" />
          </view>
          <view class="time">{{ formatTime(item.createdAt) }}</view>
          <view v-if="item.replyContent" class="reply">
            <text class="label">回复：</text>{{ item.replyContent }}
          </view>
        </view>
      </view>
      <EmptyState v-else title="还没有反馈记录" description="提交后，处理进度和回复会显示在这里。" />
    </view>
  </view>
</template>

<style scoped lang="scss">
.feedback-page {
  background: #f5f5f5;
}

.form-section {
  margin: 24rpx 28rpx 0;
  padding: 28rpx;
  border-radius: 24rpx;
  box-shadow: var(--app-shadow-sm);
}

.form-group {
  margin-bottom: 28rpx;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-bottom: 14rpx;
  font-size: 26rpx;
  font-weight: 700;
  color: var(--app-text);
}

.req {
  color: var(--app-danger);
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  border: 1rpx solid #e8e8e8;
  border-radius: 16rpx;
  background: #fff;
  font-size: 26rpx;
}

.form-input,
.form-select {
  min-height: 82rpx;
  padding: 0 22rpx;
}

.form-select {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--app-text);
}

.select-arrow {
  color: var(--app-text-muted);
}

.form-textarea {
  min-height: 240rpx;
  padding: 20rpx 22rpx;
  line-height: 1.65;
}

.upload-area {
  border: 2rpx dashed #e0e0e0;
  border-radius: 18rpx;
  padding: 38rpx 20rpx;
  text-align: center;
  background: #fff;
  color: var(--app-text-muted);
}

.upload-icon {
  font-size: 54rpx;
  line-height: 1;
}

.upload-text {
  margin-top: 12rpx;
  font-size: 25rpx;
  color: var(--app-text-soft);
}

.upload-hint {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #b8bec8;
}

.uploaded-images,
.history-images {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
}

.uploaded-images {
  margin-top: 16rpx;
}

.img-item {
  position: relative;
  width: 140rpx;
  height: 140rpx;
}

.img-item image,
.history-images image {
  width: 100%;
  height: 100%;
  border-radius: 14rpx;
  background: var(--app-surface-muted);
}

.del {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background: var(--app-danger);
  color: #fff;
  font-size: 22rpx;
  line-height: 34rpx;
  text-align: center;
}

.submit-btn {
  height: 88rpx;
  margin: 32rpx 28rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  line-height: 88rpx;
  box-shadow: 0 14rpx 28rpx rgba(232, 109, 47, 0.22);
}

.submit-btn[disabled] {
  opacity: 0.65;
}

.history-section {
  padding: 0 28rpx 32rpx;
}

.history-title {
  margin-bottom: 20rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--app-text);
}

.history-list {
  display: grid;
  gap: 18rpx;
}

.history-card {
  padding: 24rpx;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: var(--app-shadow-sm);
}

.history-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 14rpx;
}

.type {
  font-size: 26rpx;
  font-weight: 700;
  color: var(--app-text);
}

.status-tag {
  flex-shrink: 0;
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  background: var(--app-warning-soft);
  color: var(--app-warning);
}

.status-tag.resolved {
  background: var(--app-success-soft);
  color: var(--app-success);
}

.status-tag.rejected {
  background: var(--app-danger-soft);
  color: var(--app-danger);
}

.status-tag.closed {
  background: var(--app-danger-soft);
  color: var(--app-danger);
}

.content {
  color: var(--app-text-soft);
  font-size: 25rpx;
  line-height: 1.7;
}

.history-images {
  margin-top: 14rpx;
}

.history-images image {
  width: 112rpx;
  height: 112rpx;
}

.time {
  margin-top: 12rpx;
  font-size: 22rpx;
  color: #b8bec8;
}

.reply {
  margin-top: 14rpx;
  padding: 16rpx 18rpx;
  border-radius: 14rpx;
  background: #f8f8f8;
  color: var(--app-text-soft);
  font-size: 24rpx;
  line-height: 1.6;
}

.reply .label {
  color: var(--app-primary);
  font-weight: 700;
}
</style>
