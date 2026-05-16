<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { uploadImage, uploadVideo } from '@/api/operation'
import { createRecipe, getRecipe, listCategories, submitRecipe, updateRecipe } from '@/api/recipe'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'
import { requireLogin } from '@/utils/format'
import OssVideoUploader from '@/components/OssVideoUploader.vue'
import type { CategoryGroup, MediaAsset } from '@/types/cook'

interface UploadAsset {
  id?: number
  url: string
  originalName?: string
  hlsUrl?: string
  status?: string
}

interface CategoryOption {
  code: string
  name: string
}

const difficultyOptions = ['简单', '中等', '困难']
const stepGradients = [
  'linear-gradient(135deg,#ffecd2,#fcb69f)',
  'linear-gradient(135deg,#a1c4fd,#c2e9fb)',
  'linear-gradient(135deg,#f6d365,#fda085)',
]

const recipeId = ref(0)
const coverAsset = ref<UploadAsset | null>(null)
const videoAsset = ref<UploadAsset | null>(null)
const categoryOptions = ref<CategoryOption[]>([])
const form = reactive({
  title: '',
  categoryCode: '',
  difficulty: difficultyOptions[1],
  cookTime: '',
  serving: '',
  intro: '',
  tipsText: '',
  ingredients: [
    { name: '', amount: '' },
  ],
  steps: [
    { description: '', imageMediaId: 0, imageUrl: '' },
  ],
})

const categoryNames = computed(() => categoryOptions.value.map(item => item.name))
const selectedCategoryIndex = computed(() => {
  const index = categoryOptions.value.findIndex(item => item.code === form.categoryCode)
  return index >= 0 ? index : 0
})
const selectedCategoryName = computed(() => {
  const selected = categoryOptions.value.find(item => item.code === form.categoryCode)
  return selected?.name || ''
})
const videoProcessing = computed(() => ['uploaded', 'transcoding'].includes(videoAsset.value?.status || ''))

function flattenCategories(groups: CategoryGroup[]) {
  return groups.flatMap(group => group.children.map(item => ({
    code: item.code,
    name: item.name,
  })))
}

async function loadCategoryOptions() {
  const response = await listCategories()
  categoryOptions.value = flattenCategories(response.data || [])
  if (!form.categoryCode && categoryOptions.value.length) {
    form.categoryCode = categoryOptions.value[0].code
  }
}

function normalizeIngredients(items: Array<Record<string, unknown>>) {
  const next = items
    .map(item => ({
      name: String(item.name || '').trim(),
      amount: String(item.amount || '').trim(),
    }))
    .filter(item => item.name || item.amount)
  return next.length ? next : [{ name: '', amount: '' }]
}

function normalizeSteps(items: Array<Record<string, unknown>>) {
  const next = items
    .map(item => ({
      description: String(item.content || item.description || item.desc || item.text || '').trim(),
      imageMediaId: Number(item.imageMediaId || item.mediaId || 0),
      imageUrl: resolveAssetUrl(String(item.imageUrl || item.coverUrl || item.mediaUrl || '')),
    }))
    .filter(item => item.description)
  return next.length ? next : [{ description: '', imageMediaId: 0, imageUrl: '' }]
}

async function loadDetail(id: number) {
  const response = await getRecipe(id)
  const data = response.data
  if (!data) return
  coverAsset.value = data.coverMediaId ? { id: data.coverMediaId, url: mediaIdToRawUrl(data.coverMediaId) } : null
  form.title = data.title || ''
  form.categoryCode = data.categoryCode || form.categoryCode
  form.difficulty = data.difficulty || difficultyOptions[1]
  form.cookTime = data.cookTime || ''
  form.serving = data.serving || ''
  form.intro = data.intro || ''
  form.tipsText = (data.tips || []).map(item => String(item.text || item.content || item.desc || '')).filter(Boolean).join('\n')
  form.ingredients = normalizeIngredients(data.ingredients || [])
  form.steps = normalizeSteps(data.steps || [])
  const video = data.video || {}
  const rawVideoUrl = String(video.url || video.hlsUrl || video.playUrl || video.m3u8Url || '')
  videoAsset.value = rawVideoUrl
    ? {
        id: Number(video.mediaId || 0) || undefined,
        url: resolveAssetUrl(rawVideoUrl),
        hlsUrl: resolveAssetUrl(String(video.hlsUrl || '')),
        originalName: String(video.originalName || ''),
        status: String(video.status || ''),
      }
    : null
}

function changeCategory(event: { detail: { value: number } }) {
  const selected = categoryOptions.value[event.detail.value]
  if (selected) {
    form.categoryCode = selected.code
  }
}

function changeDifficulty(event: { detail: { value: number } }) {
  form.difficulty = difficultyOptions[event.detail.value] || difficultyOptions[1]
}

function addIngredient() {
  form.ingredients.push({ name: '', amount: '' })
}

function removeIngredient(index: number) {
  if (form.ingredients.length === 1) return
  form.ingredients.splice(index, 1)
}

function addStep() {
  form.steps.push({ description: '', imageMediaId: 0, imageUrl: '' })
}

function removeStep(index: number) {
  if (form.steps.length === 1) return
  form.steps.splice(index, 1)
}

function pickCover() {
  uni.chooseImage({
    count: 1,
    success: async ({ tempFilePaths }) => {
      const [filePath] = tempFilePaths || []
      if (!filePath) return
      const response = await uploadImage(filePath)
      if (!response.data?.id) return
      coverAsset.value = {
        id: response.data.id,
        url: resolveAssetUrl(response.data.url || ''),
      }
    },
  })
}

function pickVideo() {
  uni.chooseVideo({
    compressed: true,
    success: async ({ tempFilePath, name }) => {
      if (!tempFilePath) return
      const response = await uploadVideo(tempFilePath)
      if (response.data) {
        applyVideoAsset(response.data, name)
      }
    },
  })
}

function applyVideoAsset(asset: MediaAsset, fallbackName = '') {
  videoAsset.value = {
    id: asset.id,
    url: resolveAssetUrl(asset.url || ''),
    hlsUrl: resolveAssetUrl(asset.hlsUrl || ''),
    originalName: asset.originalName || fallbackName,
    status: asset.status || '',
  }
}

function pickStepImage(index: number) {
  uni.chooseImage({
    count: 1,
    success: async ({ tempFilePaths }) => {
      const [filePath] = tempFilePaths || []
      if (!filePath) return
      const response = await uploadImage(filePath)
      if (!response.data?.id) return
      form.steps[index].imageMediaId = response.data.id
      form.steps[index].imageUrl = resolveAssetUrl(response.data.url || '')
    },
  })
}

function removeStepImage(index: number) {
  form.steps[index].imageMediaId = 0
  form.steps[index].imageUrl = ''
}

function validateForm() {
  const validIngredients = form.ingredients.filter(item => item.name.trim())
  const validSteps = form.steps.filter(item => item.description.trim())
  if (!coverAsset.value?.id || !form.title.trim() || !form.categoryCode || !form.difficulty || !form.cookTime.trim() || !form.intro.trim() || !validIngredients.length || !validSteps.length) {
    uni.showToast({ title: '请先补全必填项', icon: 'none' })
    return false
  }
  return true
}

async function saveDraft(submitAfter = false) {
  if (!requireLogin()) return
  if (submitAfter && !validateForm()) return

  const payload = {
    title: form.title,
    categoryCode: form.categoryCode,
    coverMediaId: coverAsset.value?.id,
    intro: form.intro,
    difficulty: form.difficulty,
    cookTime: form.cookTime,
    serving: form.serving,
    ingredients: form.ingredients
      .map(item => ({ name: item.name.trim(), amount: item.amount.trim() }))
      .filter(item => item.name),
    steps: form.steps
      .map((item, index) => ({
        stepNumber: index + 1,
        description: item.description.trim(),
        content: item.description.trim(),
        ...(item.imageMediaId ? { imageMediaId: item.imageMediaId, imageUrl: item.imageUrl, mediaUrl: item.imageUrl } : {}),
      }))
      .filter(item => item.description),
    tips: form.tipsText
      .split('\n')
      .map(item => item.trim())
      .filter(Boolean)
      .map(text => ({ text })),
    video: videoAsset.value
      ? {
          mediaId: videoAsset.value.id,
          url: videoAsset.value.url,
          hlsUrl: videoAsset.value.hlsUrl || videoAsset.value.url,
          originalName: videoAsset.value.originalName,
          status: videoAsset.value.status || 'uploaded',
        }
      : {},
  }

  const response = recipeId.value
    ? await updateRecipe(recipeId.value, payload)
    : await createRecipe(payload)

  const nextId = response.data?.id || recipeId.value
  recipeId.value = nextId
  if (submitAfter && nextId) {
    await submitRecipe(nextId)
  }
  uni.showToast({
    title: submitAfter && videoProcessing.value ? '视频仍在处理，完成后会自动用于播放' : submitAfter ? '已提交审核' : '草稿已保存',
    icon: 'none',
  })
  if (submitAfter) {
    setTimeout(returnAfterPublish, 600)
  }
}

function goBack() {
  uni.navigateBack()
}

function returnAfterPublish() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
    return
  }
  uni.redirectTo({ url: '/pages/recipe/mine' })
}

onLoad(async (options) => {
  recipeId.value = Number(options?.id || 0)
  await loadCategoryOptions()
  if (recipeId.value) {
    await loadDetail(recipeId.value)
  }
})
</script>

<template>
  <view class="page-body recipe-form-page">
    <view class="form-nav">
      <view class="form-nav__back" @click="goBack">✕</view>
      <view class="form-nav__title">{{ recipeId ? '编辑菜谱' : '发布菜谱' }}</view>
      <button class="form-nav__submit" @click="saveDraft(true)">发布</button>
    </view>

    <view class="notice-bar">💡 发布的菜谱需经过审核后才会公开展示，请确保内容原创且无违规信息</view>

    <view class="surface-card section-card">
      <view class="section-title">📸 菜谱封面 <text class="required">*必填</text></view>
      <view class="cover-upload" :class="{ active: !!coverAsset }" @click="pickCover">
        <view v-if="coverAsset" class="cover-upload__preview">
          <image class="cover-upload__image" :src="coverAsset.url" mode="aspectFill" />
          <view class="cover-upload__change">更换封面</view>
        </view>
        <template v-else>
          <text class="icon">📷</text>
          <text>点击上传菜谱封面</text>
          <text class="hint">建议 16:9，图片不超过 20MB</text>
        </template>
      </view>
    </view>

    <view class="surface-card section-card">
      <view class="section-title">📝 基本信息 <text class="required">*必填</text></view>
      <view class="form-group">
        <view class="form-label">菜谱名称 <text class="req">*</text></view>
        <input v-model="form.title" class="form-input" placeholder="给你的菜谱起个名字" />
      </view>
      <view class="form-row">
        <view class="form-group">
          <view class="form-label">菜谱分类 <text class="req">*</text></view>
          <picker :range="categoryNames" :value="selectedCategoryIndex" @change="changeCategory">
            <view class="picker-field">{{ selectedCategoryName || '请选择分类' }}</view>
          </picker>
        </view>
        <view class="form-group">
          <view class="form-label">难度等级 <text class="req">*</text></view>
          <picker :range="difficultyOptions" @change="changeDifficulty">
            <view class="picker-field">{{ form.difficulty }}</view>
          </picker>
        </view>
      </view>
      <view class="form-row">
        <view class="form-group">
          <view class="form-label">所需时间 <text class="req">*</text></view>
          <input v-model="form.cookTime" class="form-input" placeholder="如：45分钟" />
        </view>
        <view class="form-group">
          <view class="form-label">份量</view>
          <input v-model="form.serving" class="form-input" placeholder="如：3人份" />
        </view>
      </view>
      <view class="form-group">
        <view class="form-label">菜品简介 <text class="req">*</text></view>
        <textarea v-model="form.intro" class="form-textarea" placeholder="简单描述一下这道菜的特点、口味..." />
      </view>
    </view>

    <view class="surface-card section-card">
      <view class="section-title">🎬 教学视频</view>
      <OssVideoUploader :asset="videoAsset" @uploaded="applyVideoAsset" @fallback="pickVideo" />
    </view>

    <view class="surface-card section-card">
      <view class="section-title">🥬 所需食材 <text class="required">*必填</text></view>
      <view class="ingredient-list">
        <view v-for="(item, index) in form.ingredients" :key="index" class="ingredient-row">
          <input v-model="item.name" class="form-input compact" placeholder="食材名称" />
          <input v-model="item.amount" class="form-input compact amount" placeholder="用量" />
          <button class="remove-btn" @click="removeIngredient(index)">✕</button>
        </view>
        <button class="add-btn" @click="addIngredient">＋ 添加食材</button>
      </view>
    </view>

    <view class="surface-card section-card">
      <view class="section-title">📖 图文步骤 <text class="required">*必填</text></view>
      <view class="step-list">
        <view v-for="(item, index) in form.steps" :key="index" class="step-card">
          <view class="step-header">
            <view class="step-num">{{ index + 1 }}</view>
            <button class="step-delete" @click="removeStep(index)">删除步骤</button>
          </view>
          <view
            class="step-image"
            :class="{ active: !!item.imageUrl }"
            :style="item.imageUrl ? undefined : { background: stepGradients[index % stepGradients.length] }"
            @click="pickStepImage(index)"
          >
            <image v-if="item.imageUrl" class="step-image__media" :src="item.imageUrl" mode="aspectFill" />
            <template v-else><text class="icon">📷</text> 添加步骤图片</template>
            <view v-if="item.imageUrl" class="step-image__change">更换步骤图</view>
          </view>
          <view v-if="item.imageUrl" class="step-image__remove" @click.stop="removeStepImage(index)">移除图片</view>
          <textarea v-model="item.description" class="step-textarea" placeholder="描述这一步的操作..." />
        </view>
        <button class="add-btn" @click="addStep">＋ 添加步骤</button>
      </view>
    </view>

    <view class="surface-card section-card">
      <view class="section-title">💡 烹饪小贴士</view>
      <textarea v-model="form.tipsText" class="form-textarea tips" placeholder="分享一些烹饪技巧、注意事项..." />
    </view>

    <view class="submit-bar">
      <button class="draft-btn" @click="saveDraft(false)">存草稿</button>
      <button class="submit-btn" @click="saveDraft(true)">📤 提交审核</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.recipe-form-page {
  padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
}

.form-nav {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 18rpx 28rpx;
  background: rgba(255, 252, 248, 0.92);
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18rpx);
}

.form-nav__back {
  font-size: 38rpx;
  color: var(--app-text-soft);
}

.form-nav__title {
  flex: 1;
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  color: var(--app-text);
}

.form-nav__submit {
  height: 60rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 24rpx;
  line-height: 60rpx;
}

.notice-bar {
  margin-top: 2rpx;
  padding: 18rpx 28rpx;
  background: #fff7e6;
  color: #fa8c16;
  font-size: 22rpx;
}

.section-card {
  margin: 18rpx 28rpx 0;
  padding: 24rpx;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 30rpx;
}

.required,
.req {
  color: var(--app-danger);
  font-size: 20rpx;
}

.cover-upload,
.video-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 18rpx;
  border: 2rpx dashed rgba(15, 23, 42, 0.12);
  border-radius: 22rpx;
  color: var(--app-text-muted);
}

.cover-upload {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
}

.cover-upload.active,
.video-upload.active {
  border-style: solid;
}

.cover-upload .icon,
.video-upload .icon {
  font-size: 54rpx;
}

.cover-upload .hint,
.video-upload .hint {
  font-size: 20rpx;
}

.cover-upload__preview {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.cover-upload__image {
  width: 100%;
  height: 100%;
}

.cover-upload__change {
  position: absolute;
  right: 16rpx;
  bottom: 16rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.36);
  color: #fff;
  font-size: 20rpx;
}

.video-upload {
  min-height: 180rpx;
}

.form-group {
  margin-top: 18rpx;
}

.form-row {
  display: flex;
  gap: 12rpx;
}

.form-row .form-group {
  flex: 1;
}

.form-label {
  margin-bottom: 8rpx;
  color: var(--app-text-soft);
  font-size: 22rpx;
}

.form-input,
.form-textarea,
.picker-field,
.step-textarea {
  width: 100%;
  border-radius: 18rpx;
  background: var(--app-surface-muted);
  color: var(--app-text);
  font-size: 24rpx;
}

.form-input,
.picker-field {
  height: 76rpx;
  padding: 0 20rpx;
  line-height: 76rpx;
}

.form-textarea,
.step-textarea {
  min-height: 150rpx;
  padding: 18rpx 20rpx;
  line-height: 1.7;
}

.compact {
  height: 68rpx;
  line-height: 68rpx;
}

.ingredient-list,
.step-list {
  display: grid;
  gap: 16rpx;
  margin-top: 18rpx;
}

.ingredient-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.amount {
  max-width: 200rpx;
}

.remove-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #fff1f0;
  color: var(--app-danger);
  font-size: 24rpx;
  line-height: 56rpx;
}

.add-btn {
  width: 100%;
  height: 72rpx;
  border-radius: 18rpx;
  border: 1rpx dashed rgba(232, 109, 47, 0.3);
  background: transparent;
  color: var(--app-primary);
  font-size: 24rpx;
  line-height: 72rpx;
}

.step-card {
  padding: 20rpx;
  border: 1rpx solid rgba(15, 23, 42, 0.06);
  border-radius: 22rpx;
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14rpx;
}

.step-num {
  width: 46rpx;
  height: 46rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
  line-height: 46rpx;
  text-align: center;
}

.step-delete {
  color: var(--app-danger);
  font-size: 20rpx;
  background: transparent;
}

.step-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
  height: 180rpx;
  margin-bottom: 14rpx;
  border-radius: 18rpx;
  border: 1rpx dashed rgba(15, 23, 42, 0.12);
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.step-image.active {
  border-style: solid;
  color: rgba(255, 255, 255, 0.92);
  overflow: hidden;
}

.step-image .icon {
  font-size: 36rpx;
}

.step-image__media {
  width: 100%;
  height: 100%;
}

.step-image__change {
  position: absolute;
  right: 16rpx;
  bottom: 14rpx;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.36);
  color: #fff;
  font-size: 20rpx;
}

.step-image__remove {
  margin-bottom: 12rpx;
  color: var(--app-danger);
  font-size: 22rpx;
  text-align: right;
}

.tips {
  min-height: 130rpx;
}

.submit-bar {
  position: fixed;
  right: 50%;
  bottom: 0;
  display: flex;
  align-items: center;
  gap: 14rpx;
  width: calc(100% - 56rpx);
  max-width: 804rpx;
  padding: 14rpx 18rpx calc(14rpx + env(safe-area-inset-bottom));
  transform: translateX(50%);
  background: rgba(255, 255, 255, 0.96);
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.draft-btn,
.submit-btn {
  height: 76rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  line-height: 76rpx;
}

.draft-btn {
  width: 200rpx;
  background: #fff;
  color: var(--app-text-soft);
  border: 1rpx solid rgba(15, 23, 42, 0.12);
}

.submit-btn {
  flex: 1;
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
}
</style>
