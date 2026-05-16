<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppNavBar from '@/components/AppNavBar.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  createCheckin,
  deleteCheckin,
  generateCheckinPost,
  getCheckinSummary,
  listCheckinsByMonth,
  updateCheckin,
} from '@/api/checkin'
import { uploadImage } from '@/api/operation'
import { listRecipes } from '@/api/recipe'
import type { CheckinItem, CheckinSummary, RecipeListItem } from '@/types/cook'
import { formatTime, requireLogin } from '@/utils/format'
import { mediaIdToRawUrl, mediaIdsToUrls, resolveAssetUrl } from '@/utils/media'

interface MediaPreviewItem {
  id: number
  url: string
}

const summary = ref<CheckinSummary>({})
const rows = ref<CheckinItem[]>([])
const displayMonth = ref(new Date())
const showEditor = ref(false)
const editingId = ref<number>()
const submitting = ref(false)
const uploading = ref(false)
const form = reactive({
  checkinDate: formatDate(new Date()),
  content: '',
  recipeId: '',
})
const mediaItems = ref<MediaPreviewItem[]>([])
const recipePicker = reactive({
  visible: false,
  loading: false,
  keyword: '',
  rows: [] as RecipeListItem[],
  selected: null as RecipeListItem | null,
})

const weekLabels = ['日', '一', '二', '三', '四', '五', '六']
const thumbIcons = ['🍳', '🥗', '🍲', '🦐', '🐟', '🍚']

const monthLabel = computed(() => `${displayMonth.value.getFullYear()}年${displayMonth.value.getMonth() + 1}月`)
const maxCheckinDate = computed(() => formatDate(new Date()))
const selectedRecipeTitle = computed(() => recipePicker.selected?.title || (form.recipeId ? `菜谱 #${form.recipeId}` : ''))
const checkedDates = computed(() => new Set(rows.value.map(item => item.checkinDate).filter(Boolean) as string[]))
const sortedRows = computed(() => [...rows.value].sort((left, right) => {
  const leftTime = `${left.checkinDate || ''} ${left.createdAt || ''}`
  const rightTime = `${right.checkinDate || ''} ${right.createdAt || ''}`
  return rightTime.localeCompare(leftTime)
}))

const calendarDays = computed(() => {
  const year = displayMonth.value.getFullYear()
  const month = displayMonth.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDate = new Date(year, month + 1, 0).getDate()
  const today = formatDate(new Date())
  const days: Array<{ key: string; day?: number; date?: string; checked?: boolean; today?: boolean; empty?: boolean }> = []
  for (let index = 0; index < firstDay.getDay(); index += 1) {
    days.push({ key: `empty-${index}`, empty: true })
  }
  for (let day = 1; day <= lastDate; day += 1) {
    const date = formatDate(new Date(year, month, day))
    days.push({
      key: date,
      day,
      date,
      checked: checkedDates.value.has(date),
      today: date === today,
    })
  }
  return days
})

async function loadData() {
  if (!requireLogin()) return
  const [summaryResponse, listResponse] = await Promise.all([
    getCheckinSummary(),
    listCheckinsByMonth(formatDate(displayMonth.value)),
  ])
  summary.value = summaryResponse.data || {}
  rows.value = listResponse.data || []
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function changeMonth(offset: number) {
  const current = displayMonth.value
  displayMonth.value = new Date(current.getFullYear(), current.getMonth() + offset, 1)
  loadData()
}

function openCreate() {
  editingId.value = undefined
  const today = new Date()
  const inDisplayMonth = today.getFullYear() === displayMonth.value.getFullYear()
    && today.getMonth() === displayMonth.value.getMonth()
  form.checkinDate = inDisplayMonth ? formatDate(today) : formatDate(displayMonth.value)
  form.content = ''
  form.recipeId = ''
  recipePicker.selected = null
  mediaItems.value = []
  showEditor.value = true
}

function openEdit(item: CheckinItem) {
  editingId.value = item.id
  form.checkinDate = item.checkinDate || formatDate(new Date())
  form.content = item.content || ''
  form.recipeId = item.recipeId ? String(item.recipeId) : ''
  recipePicker.selected = item.recipeId
    ? { id: item.recipeId, title: item.recipeTitle || recordTitle(item) } as RecipeListItem
    : null
  mediaItems.value = (item.mediaIds || []).map(id => ({ id, url: mediaIdToRawUrl(id) }))
  showEditor.value = true
}

function closeEditor() {
  showEditor.value = false
  submitting.value = false
}

async function submitCheckin() {
  if (!form.content.trim()) {
    uni.showToast({ title: '请填写打卡内容', icon: 'none' })
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const payload = {
      checkinDate: form.checkinDate,
      content: form.content,
      recipeId: form.recipeId ? Number(form.recipeId) : undefined,
      mediaIds: mediaItems.value.map(item => item.id),
      source: recipePicker.selected ? { recipeTitle: recipePicker.selected.title } : {},
    }
    if (editingId.value) {
      await updateCheckin(editingId.value, payload)
    }
    else {
      await createCheckin(payload)
    }
    closeEditor()
    await loadData()
  }
  finally {
    submitting.value = false
  }
}

function removeCheckin(id: number) {
  uni.showModal({
    title: '删除打卡',
    content: '删除后不可恢复，确认删除这条打卡记录吗？',
    success: async ({ confirm }) => {
      if (!confirm) return
      await deleteCheckin(id)
      await loadData()
    },
  })
}

async function postCheckin(item: CheckinItem) {
  if (item.generatedPostId) {
    uni.showToast({ title: '已生成动态', icon: 'none' })
    return
  }
  await generateCheckinPost(item.id)
  uni.showToast({ title: '动态已生成', icon: 'none' })
  await loadData()
}

function recordTitle(item: CheckinItem) {
  const source = item.source || {}
  return String(item.recipeTitle || source.recipeName || source.recipeTitle || source.title || (item.recipeId ? `关联菜谱 #${item.recipeId}` : '饮食打卡'))
}

function recordMeta(item: CheckinItem) {
  const source = item.source || {}
  const author = String(source.authorNickname || source.authorName || '')
  if (author) return `跟着 ${author} 的菜谱制作`
  if (item.recipeTitle) return `关联菜谱：${item.recipeTitle}`
  if (item.recipeId) return `关联菜谱 #${item.recipeId}`
  return '自主记录'
}

function recordImages(item: CheckinItem) {
  return mediaIdsToUrls(item.mediaIds).slice(0, 4)
}

function recordCover(item: CheckinItem) {
  return recordImages(item)[0] || ''
}

function recordExtraImages(item: CheckinItem) {
  return recordImages(item).slice(1, 4)
}

function changeCheckinDate(event: { detail: { value: string } }) {
  form.checkinDate = event.detail.value || form.checkinDate
}

async function chooseImages() {
  if (mediaItems.value.length >= 4 || uploading.value) return
  uni.chooseImage({
    count: 4 - mediaItems.value.length,
    success: async ({ tempFilePaths }) => {
      const files = Array.isArray(tempFilePaths) ? tempFilePaths : tempFilePaths ? [tempFilePaths] : []
      uploading.value = true
      try {
        const uploaded = await Promise.all(files.map(async (filePath: string) => {
          const response = await uploadImage(filePath)
          if (!response.data?.id) return null
          return {
            id: response.data.id,
            url: resolveAssetUrl(response.data.url || mediaIdToRawUrl(response.data.id)),
          }
        }))
        mediaItems.value = [
          ...mediaItems.value,
          ...uploaded.filter((item: MediaPreviewItem | null): item is MediaPreviewItem => Boolean(item)),
        ]
      }
      finally {
        uploading.value = false
      }
    },
  })
}

function removeImage(index: number) {
  mediaItems.value.splice(index, 1)
}

async function openRecipePicker() {
  recipePicker.visible = true
  await loadRecipeOptions()
}

function closeRecipePicker() {
  recipePicker.visible = false
}

function clearRecipeLink() {
  form.recipeId = ''
  recipePicker.selected = null
}

async function loadRecipeOptions() {
  recipePicker.loading = true
  try {
    const response = await listRecipes({
      keyword: recipePicker.keyword.trim(),
      page: '1',
      pageSize: '20',
    })
    recipePicker.rows = response.data?.items || []
  }
  catch {
    recipePicker.rows = []
  }
  finally {
    recipePicker.loading = false
  }
}

function selectRecipe(item: RecipeListItem) {
  form.recipeId = String(item.id)
  recipePicker.selected = item
  closeRecipePicker()
}

onMounted(loadData)
</script>

<template>
  <view class="page-body checkin-page">
    <AppNavBar title="打卡记录" back />

    <view class="stats-bar">
      <view class="stat">
        <view class="num">{{ summary.totalCount || 0 }}</view>
        <view class="label">累计打卡</view>
      </view>
      <view class="stat">
        <view class="num">{{ summary.monthCount || 0 }}</view>
        <view class="label">本月打卡</view>
      </view>
      <view class="stat">
        <view class="num">{{ summary.streakDays || 0 }}</view>
        <view class="label">连续天数</view>
      </view>
    </view>

    <view class="calendar-section">
      <view class="calendar-header">
        <text class="month">{{ monthLabel }}</text>
        <view class="month-nav">
          <text @click="changeMonth(-1)">‹</text>
          <text @click="changeMonth(1)">›</text>
        </view>
      </view>
      <view class="calendar-grid">
        <text v-for="item in weekLabels" :key="item" class="day-label">{{ item }}</text>
        <view
          v-for="item in calendarDays"
          :key="item.key"
          class="day"
          :class="{ empty: item.empty, checked: item.checked, today: item.today }"
        >
          {{ item.day || 0 }}
        </view>
      </view>
    </view>

    <view class="section-row">
      <text class="section-title-small">最近打卡</text>
      <button class="add-btn" @click="openCreate">新增打卡</button>
    </view>

    <view v-if="sortedRows.length" class="record-list">
      <view v-for="(item, index) in sortedRows" :key="item.id" class="record-card">
        <view class="record-top">
          <view class="recipe-thumb">
            <image v-if="recordCover(item)" class="recipe-thumb__image" :src="recordCover(item)" mode="aspectFill" />
            <template v-else>{{ thumbIcons[index % thumbIcons.length] }}</template>
          </view>
          <view class="record-info">
            <view class="recipe-name">{{ recordTitle(item) }}</view>
            <view class="recipe-meta">{{ recordMeta(item) }}</view>
            <view class="record-time">{{ formatTime(item.createdAt || item.checkinDate) }} 打卡</view>
          </view>
        </view>

        <view v-if="recordExtraImages(item).length" class="record-photos">
          <image v-for="url in recordExtraImages(item)" :key="url" :src="url" mode="aspectFill" />
        </view>

        <view v-if="item.content" class="record-note">{{ item.content }}</view>

        <view class="record-actions">
          <text class="action primary" @click="postCheckin(item)">{{ item.generatedPostId ? '已生成动态' : '发布动态' }}</text>
          <text class="action" @click="openEdit(item)">编辑</text>
          <text class="action" @click="removeCheckin(item.id)">删除</text>
        </view>
      </view>
    </view>
    <EmptyState v-else title="还没有打卡记录" description="先记录一顿饭，后续可以直接生成社区动态。" />

    <view v-if="showEditor" class="modal-mask" @click="closeEditor">
      <view class="editor-card" @click.stop>
        <view class="editor-title">{{ editingId ? '编辑打卡' : '新增打卡' }}</view>
        <picker mode="date" :value="form.checkinDate" start="2020-01-01" :end="maxCheckinDate" @change="changeCheckinDate">
          <view class="editor-input editor-input--picker">{{ form.checkinDate || '选择打卡日期' }}</view>
        </picker>
        <view class="editor-link-row">
          <view class="editor-link-main" @click="openRecipePicker">
            <text class="editor-link-icon">📖</text>
            <text class="editor-link-text">{{ form.recipeId ? `关联菜谱：${selectedRecipeTitle}` : '关联菜谱' }}</text>
          </view>
          <text v-if="form.recipeId" class="editor-link-clear" @click="clearRecipeLink">清除</text>
        </view>
        <view class="editor-image-grid">
          <view v-for="(item, index) in mediaItems" :key="item.id" class="editor-image-item">
            <image :src="item.url" mode="aspectFill" />
            <view class="editor-image-remove" @click="removeImage(index)">✕</view>
          </view>
          <view v-if="mediaItems.length < 4" class="editor-image-add" @click="chooseImages">
            <text>{{ uploading ? '上传中' : '📷' }}</text>
            <text>{{ uploading ? '请稍候' : '上传图片' }}</text>
          </view>
        </view>
        <textarea v-model="form.content" class="editor-textarea" placeholder="记录今天吃了什么、感觉如何" />
        <view class="editor-actions">
          <button class="cancel-btn" @click="closeEditor">取消</button>
          <button class="save-btn" :disabled="submitting" @click="submitCheckin">{{ submitting ? '保存中...' : '保存' }}</button>
        </view>
      </view>
    </view>

    <view v-if="recipePicker.visible" class="recipe-picker">
      <view class="recipe-picker__mask" @click="closeRecipePicker" />
      <view class="recipe-picker__panel">
        <view class="recipe-picker__header">
          <view>
            <view class="recipe-picker__title">选择关联菜谱</view>
            <view class="recipe-picker__subtitle">打卡完成后会显示菜谱中文名</view>
          </view>
          <view class="recipe-picker__close" @click="closeRecipePicker">✕</view>
        </view>
        <view class="recipe-picker__search">
          <input v-model="recipePicker.keyword" class="recipe-picker__input" placeholder="搜索菜谱名称" confirm-type="search" @confirm="loadRecipeOptions" />
          <button class="recipe-picker__search-btn" @click="loadRecipeOptions">搜索</button>
        </view>
        <scroll-view scroll-y class="recipe-picker__list">
          <view v-if="recipePicker.loading" class="recipe-picker__empty">加载中...</view>
          <view v-else-if="!recipePicker.rows.length" class="recipe-picker__empty">暂无可选菜谱</view>
          <template v-else>
            <view
              v-for="item in recipePicker.rows"
              :key="item.id"
              class="recipe-picker__item"
              :class="{ active: String(item.id) === form.recipeId }"
              @click="selectRecipe(item)"
            >
              <view class="recipe-picker__cover">📖</view>
              <view class="recipe-picker__body">
                <view class="recipe-picker__name">{{ item.title }}</view>
                <view class="recipe-picker__meta">{{ item.authorNickname || '匿名作者' }} · {{ item.categoryCode || '家常菜' }}</view>
              </view>
              <view class="recipe-picker__check">{{ String(item.id) === form.recipeId ? '✓' : '' }}</view>
            </view>
          </template>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.checkin-page {
  background: #f5f5f5;
}

.stats-bar {
  display: flex;
  justify-content: space-around;
  padding: 38rpx 28rpx;
  color: #fff;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
}

.stat {
  text-align: center;
}

.num {
  font-size: 52rpx;
  font-weight: 800;
  line-height: 1.1;
}

.label {
  margin-top: 8rpx;
  font-size: 23rpx;
  opacity: 0.92;
}

.calendar-section {
  margin: 24rpx 28rpx;
  padding: 28rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: var(--app-shadow-sm);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22rpx;
}

.month {
  font-size: 30rpx;
  font-weight: 700;
  color: var(--app-text);
}

.month-nav {
  display: flex;
  gap: 18rpx;
}

.month-nav text {
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  background: #f5f5f5;
  color: var(--app-text-soft);
  font-size: 34rpx;
  line-height: 50rpx;
  text-align: center;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8rpx;
  text-align: center;
}

.day-label {
  padding: 8rpx 0;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.day {
  width: 54rpx;
  height: 54rpx;
  margin: 0 auto;
  border-radius: 50%;
  color: var(--app-text);
  font-size: 24rpx;
  line-height: 54rpx;
}

.day.checked {
  background: var(--app-primary);
  color: #fff;
  font-weight: 700;
}

.day.today {
  border: 3rpx solid var(--app-primary);
  color: var(--app-primary);
  font-weight: 700;
  line-height: 48rpx;
}

.day.checked.today {
  color: #fff;
  line-height: 48rpx;
}

.day.empty {
  color: transparent;
}

.section-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 8rpx 28rpx 18rpx;
}

.section-title-small {
  color: var(--app-text);
  font-size: 30rpx;
  font-weight: 700;
}

.add-btn {
  height: 58rpx;
  margin: 0;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: var(--app-primary-soft);
  color: var(--app-primary-strong);
  font-size: 24rpx;
  line-height: 58rpx;
}

.record-list {
  display: grid;
  gap: 20rpx;
  padding: 0 28rpx 32rpx;
}

.record-card {
  border-radius: 24rpx;
  overflow: hidden;
  background: #fff;
  box-shadow: var(--app-shadow-sm);
}

.record-top {
  display: flex;
  gap: 22rpx;
  padding: 26rpx;
}

.recipe-thumb {
  flex-shrink: 0;
  width: 150rpx;
  height: 150rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ffecd2, #fcb69f);
  font-size: 56rpx;
  overflow: hidden;
}

.recipe-thumb__image {
  width: 100%;
  height: 100%;
}

.record-info {
  min-width: 0;
  flex: 1;
}

.recipe-name {
  color: var(--app-text);
  font-size: 29rpx;
  font-weight: 700;
}

.recipe-meta {
  margin-top: 8rpx;
  color: var(--app-text-muted);
  font-size: 23rpx;
}

.record-time {
  margin-top: 12rpx;
  color: #b8bec8;
  font-size: 22rpx;
}

.record-photos {
  display: flex;
  gap: 10rpx;
  padding: 0 26rpx 24rpx;
}

.record-photos image {
  width: 112rpx;
  height: 112rpx;
  border-radius: 12rpx;
  background: var(--app-surface-muted);
}

.record-note {
  margin: 0 26rpx 22rpx;
  padding: 16rpx 18rpx;
  border-radius: 14rpx;
  background: #fafafa;
  color: var(--app-text-soft);
  font-size: 25rpx;
  line-height: 1.6;
}

.record-actions {
  display: flex;
  border-top: 1rpx solid #f5f5f5;
}

.action {
  flex: 1;
  padding: 20rpx 0;
  border-right: 1rpx solid #f5f5f5;
  color: var(--app-text-soft);
  font-size: 25rpx;
  text-align: center;
}

.action:last-child {
  border-right: 0;
}

.action.primary {
  color: var(--app-primary);
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(15, 23, 42, 0.38);
}

.editor-card {
  width: 100%;
  max-width: 860rpx;
  padding: 30rpx 28rpx calc(30rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #fff;
}

.editor-title {
  margin-bottom: 22rpx;
  color: var(--app-text);
  font-size: 32rpx;
  font-weight: 800;
}

.editor-input,
.editor-textarea {
  width: 100%;
  border-radius: 18rpx;
  background: #f8fafc;
  font-size: 26rpx;
}

.editor-input {
  height: 80rpx;
  padding: 0 22rpx;
}

.editor-input--picker {
  color: var(--app-text);
  line-height: 80rpx;
}

.editor-link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 18rpx;
  padding: 20rpx 22rpx;
  border-radius: 18rpx;
  background: #fff8f2;
}

.editor-link-main {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12rpx;
  flex: 1;
}

.editor-link-icon {
  flex-shrink: 0;
}

.editor-link-text {
  min-width: 0;
  overflow: hidden;
  color: var(--app-primary-strong);
  font-size: 25rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.editor-link-clear {
  flex-shrink: 0;
  color: var(--app-text-muted);
  font-size: 23rpx;
}

.editor-image-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 18rpx;
}

.editor-image-item,
.editor-image-add {
  position: relative;
  aspect-ratio: 1;
  border-radius: 16rpx;
  overflow: hidden;
  background: #f8fafc;
}

.editor-image-item image {
  width: 100%;
  height: 100%;
}

.editor-image-remove {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.62);
  color: #fff;
  font-size: 20rpx;
  line-height: 34rpx;
  text-align: center;
}

.editor-image-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  border: 1rpx dashed rgba(232, 109, 47, 0.36);
  color: var(--app-primary);
  font-size: 22rpx;
}

.editor-textarea {
  min-height: 190rpx;
  margin-top: 18rpx;
  padding: 20rpx 22rpx;
  line-height: 1.65;
}

.editor-actions {
  display: flex;
  gap: 18rpx;
  margin-top: 24rpx;
}

.cancel-btn,
.save-btn {
  flex: 1;
  height: 76rpx;
  margin: 0;
  border-radius: 999rpx;
  font-size: 27rpx;
  line-height: 76rpx;
}

.cancel-btn {
  background: #f8fafc;
  color: var(--app-text-soft);
}

.save-btn {
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
}

.recipe-picker {
  position: fixed;
  inset: 0;
  z-index: 160;
}

.recipe-picker__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
}

.recipe-picker__panel {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  max-height: 78vh;
  padding: 28rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
  border-radius: 32rpx 32rpx 0 0;
  background: #fff;
}

.recipe-picker__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.recipe-picker__title {
  color: var(--app-text);
  font-size: 31rpx;
  font-weight: 800;
}

.recipe-picker__subtitle {
  margin-top: 6rpx;
  color: var(--app-text-muted);
  font-size: 23rpx;
}

.recipe-picker__close {
  color: var(--app-text-muted);
  font-size: 30rpx;
}

.recipe-picker__search {
  display: flex;
  gap: 12rpx;
  margin-top: 22rpx;
}

.recipe-picker__input {
  flex: 1;
  height: 70rpx;
  padding: 0 20rpx;
  border-radius: 16rpx;
  background: #f8fafc;
  font-size: 25rpx;
}

.recipe-picker__search-btn {
  width: 132rpx;
  height: 70rpx;
  margin: 0;
  border-radius: 16rpx;
  background: var(--app-primary);
  color: #fff;
  font-size: 24rpx;
  line-height: 70rpx;
}

.recipe-picker__list {
  max-height: 52vh;
  margin-top: 18rpx;
}

.recipe-picker__empty {
  padding: 48rpx 0;
  color: var(--app-text-muted);
  font-size: 24rpx;
  text-align: center;
}

.recipe-picker__item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.recipe-picker__item.active .recipe-picker__name {
  color: var(--app-primary);
}

.recipe-picker__cover {
  width: 76rpx;
  height: 76rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary-soft);
}

.recipe-picker__body {
  min-width: 0;
  flex: 1;
}

.recipe-picker__name {
  overflow: hidden;
  color: var(--app-text);
  font-size: 27rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-picker__meta {
  margin-top: 6rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.recipe-picker__check {
  width: 36rpx;
  color: var(--app-primary);
  font-size: 28rpx;
  text-align: center;
}
</style>
