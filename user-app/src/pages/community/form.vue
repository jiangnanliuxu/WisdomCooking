<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { uploadImage } from '@/api/operation'
import { createPost, getPost, updatePost } from '@/api/post'
import { listRecipes } from '@/api/recipe'
import type { RecipeListItem } from '@/types/cook'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'
import { requireLogin } from '@/utils/format'
import {
  createAmapWebMap,
  formatPoiLocationText,
  getCurrentCoordinate,
  isAmapConfigured,
  normalizeLocationText,
  requestUserLocationPermission,
  reverseGeocode,
  searchNearbyPois,
  type AmapWebMapController,
  type LocationCoordinate,
  type LocationPoi,
} from '@/utils/amap'

interface MediaPreviewItem {
  id: number
  url: string
}

const postId = ref(0)
const form = reactive({
  content: '',
  visibility: 'public',
  topicCodesText: '',
  location: '',
  relatedRecipeId: '',
  sourceType: 'normal',
})
const mediaItems = ref<MediaPreviewItem[]>([])
const recipePicker = reactive({
  visible: false,
  loading: false,
  keyword: '',
  rows: [] as RecipeListItem[],
  selected: null as RecipeListItem | null,
})

const gradients = [
  'linear-gradient(135deg,#ff9a9e,#fecfef)',
  'linear-gradient(135deg,#ffecd2,#fcb69f)',
  'linear-gradient(135deg,#f6d365,#fda085)',
]
const imageIcons = ['🦐', '🍲', '😋']
const defaultCoordinate: LocationCoordinate = {
  latitude: 31.2304,
  longitude: 121.4737,
}
const locationPicker = reactive({
  visible: false,
  loading: false,
  resolving: false,
  latitude: defaultCoordinate.latitude,
  longitude: defaultCoordinate.longitude,
  selectedName: '',
  selectedAddress: '',
  selectedPoiId: '',
  pois: [] as LocationPoi[],
})
let h5MapController: AmapWebMapController | null = null

const charCount = computed(() => form.content.length)
const topicsPreview = computed(() => {
  const tags = normalizeTopicCodes(form.topicCodesText)
  return tags.length ? tags.map(tag => `#${tag}`).join(' ') : '未选择'
})
const recipeSelected = computed(() => Boolean(form.relatedRecipeId))
const selectedRecipeTitle = computed(() => recipePicker.selected?.title || (form.relatedRecipeId ? `菜谱 #${form.relatedRecipeId}` : ''))
const visibilityLabel = computed(() => (
  form.visibility === 'followers' ? '仅粉丝可见' : form.visibility === 'private' ? '仅自己可见' : '所有人'
))
const selectedLocationLabel = computed(() => (
  locationPicker.selectedName || locationPicker.selectedAddress || '点击地图或选择附近地点'
))
const canConfirmLocation = computed(() => Boolean(normalizeLocationText(selectedLocationLabel.value)))

async function loadDetail(id: number) {
  const response = await getPost(id)
  const data = response.data
  if (!data) return
  form.content = data.content || ''
  form.visibility = data.visibility || 'public'
  form.topicCodesText = (data.topicCodes || []).join(',')
  form.location = data.location || ''
  form.relatedRecipeId = String(data.relatedRecipeId || '')
  form.sourceType = data.sourceType || 'normal'
  recipePicker.selected = data.relatedRecipeId
    ? { id: data.relatedRecipeId, title: data.relatedRecipeTitle || `菜谱 #${data.relatedRecipeId}` } as RecipeListItem
    : null
  mediaItems.value = (data.mediaIds || []).map(id => ({
    id,
    url: mediaIdToRawUrl(id),
  }))
}

async function submit() {
  if (!requireLogin()) return
  const topicCodes = normalizeTopicCodes(form.topicCodesText)
  if (topicCodes.length > 5) {
    uni.showToast({ title: '动态话题最多5个', icon: 'none' })
    return
  }
  if (topicCodes.some(item => item.length > 20)) {
    uni.showToast({ title: '单个话题不能超过20个字', icon: 'none' })
    return
  }
  const payload = {
    content: form.content,
    visibility: form.visibility,
    topicCodes,
    location: form.location,
    relatedRecipeId: form.relatedRecipeId ? Number(form.relatedRecipeId) : undefined,
    sourceType: form.sourceType,
    mediaIds: mediaItems.value.map(item => item.id),
  }
  if (postId.value) {
    await updatePost(postId.value, payload)
  }
  else {
    await createPost(payload)
  }
  uni.showToast({ title: '已提交审核', icon: 'none' })
  uni.navigateBack()
}

function normalizeTopicCodes(value: string) {
  return Array.from(new Set(
    value
      .split(/[,\uFF0C\s#]+/)
      .map(item => item.trim())
      .filter(Boolean)
  ))
}

function goBack() {
  uni.navigateBack()
}

function addPreviewImage() {
  if (mediaItems.value.length >= 9) return
  uni.chooseImage({
    count: 9 - mediaItems.value.length,
    success: async ({ tempFilePaths }) => {
      const files = Array.isArray(tempFilePaths) ? tempFilePaths : tempFilePaths ? [tempFilePaths] : []
      const uploaded = await Promise.all(files.map(async (filePath: string) => {
        const response = await uploadImage(filePath)
        if (!response.data?.id) return null
        return {
          id: response.data.id,
          url: resolveAssetUrl(response.data.url || ''),
        }
      }))
      mediaItems.value = [
        ...mediaItems.value,
        ...uploaded.filter((item: MediaPreviewItem | null): item is MediaPreviewItem => Boolean(item)),
      ]
    },
  })
}

function removePreviewImage(index: number) {
  mediaItems.value.splice(index, 1)
}

function clearRecipeLink() {
  form.relatedRecipeId = ''
  recipePicker.selected = null
}

function clearLocation() {
  form.location = ''
}

async function openRecipePicker() {
  recipePicker.visible = true
  await loadRecipeOptions()
}

function closeRecipePicker() {
  recipePicker.visible = false
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
  form.relatedRecipeId = String(item.id)
  recipePicker.selected = item
  closeRecipePicker()
}

async function openLocationPicker() {
  if (!isLocationPickerPlatformSupported()) {
    uni.showToast({ title: '当前平台暂不支持选择位置', icon: 'none' })
    return
  }
  if (!isAmapConfigured()) {
    uni.showToast({ title: '地图服务未配置', icon: 'none' })
    return
  }

  locationPicker.visible = true
  locationPicker.loading = true
  locationPicker.selectedName = form.location
  locationPicker.selectedAddress = ''
  locationPicker.selectedPoiId = ''

  try {
    await requestUserLocationPermission()
    const coordinate = await getCurrentCoordinate()
    await selectLocationCoordinate(coordinate)
    await initH5LocationMap()
  }
  catch (error) {
    locationPicker.visible = false
    destroyH5LocationMap()
    handleLocationPickerError(error)
  }
  finally {
    locationPicker.loading = false
  }
}

function closeLocationPicker() {
  locationPicker.visible = false
  destroyH5LocationMap()
}

async function relocateCurrentPosition() {
  locationPicker.loading = true
  try {
    const coordinate = await getCurrentCoordinate()
    await selectLocationCoordinate(coordinate)
  }
  catch (error) {
    handleLocationPickerError(error)
  }
  finally {
    locationPicker.loading = false
  }
}

async function handleLocationMapTap(event: { detail?: Partial<LocationCoordinate> }) {
  const latitude = Number(event.detail?.latitude)
  const longitude = Number(event.detail?.longitude)
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
    uni.showToast({ title: '未获取到地图坐标', icon: 'none' })
    return
  }
  await selectLocationCoordinate({ latitude, longitude })
}

function selectLocationPoi(poi: LocationPoi) {
  locationPicker.latitude = poi.latitude
  locationPicker.longitude = poi.longitude
  locationPicker.selectedName = formatPoiLocationText(poi)
  locationPicker.selectedAddress = poi.address
  locationPicker.selectedPoiId = poi.id
  syncH5LocationMap()
}

function confirmLocationPicker() {
  const location = normalizeLocationText(selectedLocationLabel.value)
  if (!location) {
    uni.showToast({ title: '请选择位置', icon: 'none' })
    return
  }
  form.location = location
  closeLocationPicker()
}

async function selectLocationCoordinate(coordinate: LocationCoordinate) {
  locationPicker.latitude = coordinate.latitude
  locationPicker.longitude = coordinate.longitude
  locationPicker.resolving = true
  locationPicker.selectedPoiId = ''
  try {
    const result = await reverseGeocode(coordinate)
    locationPicker.latitude = result.latitude
    locationPicker.longitude = result.longitude
    locationPicker.selectedName = result.name
    locationPicker.selectedAddress = result.address
  }
  catch {
    locationPicker.selectedName = '已选择位置'
    locationPicker.selectedAddress = ''
  }
  finally {
    locationPicker.resolving = false
  }
  await refreshNearbyPois({
    latitude: locationPicker.latitude,
    longitude: locationPicker.longitude,
  })
  syncH5LocationMap()
}

async function refreshNearbyPois(coordinate: LocationCoordinate) {
  try {
    locationPicker.pois = (await searchNearbyPois(coordinate)).slice(0, 12)
  }
  catch {
    locationPicker.pois = []
  }
}

function handleLocationPickerError(error: unknown) {
  const message = error instanceof Error ? error.message : ''
  if (message === 'LOCATION_PERMISSION_DENIED') {
    // #ifdef MP-WEIXIN
    uni.showModal({
      title: '需要定位权限',
      content: '请在设置中开启定位权限后再选择位置。',
      confirmText: '去设置',
      success: (result) => {
        if (result.confirm) {
          uni.openSetting({})
        }
      },
    })
    return
    // #endif
    // #ifdef H5
    uni.showToast({ title: '请允许浏览器定位权限', icon: 'none' })
    return
    // #endif
  }
  const title = message === 'LOCATION_FAILED' || message === 'LOCATION_TIMEOUT'
    ? '定位失败'
    : message === 'AMAP_WEB_LOADER_FAILED'
      ? '地图加载失败'
      : '位置选择失败'
  uni.showToast({ title, icon: 'none' })
}

function isLocationPickerPlatformSupported() {
  let supported = false
  // #ifdef MP-WEIXIN
  supported = true
  // #endif
  // #ifdef H5
  supported = true
  // #endif
  return supported
}

async function initH5LocationMap() {
  // #ifdef H5
  await nextTick()
  destroyH5LocationMap()
  h5MapController = await createAmapWebMap(
    'postLocationAmap',
    getSelectedCoordinate(),
    coordinate => selectLocationCoordinate(coordinate)
  )
  syncH5LocationMap()
  // #endif
}

function syncH5LocationMap() {
  h5MapController?.setCenter(getSelectedCoordinate())
}

function destroyH5LocationMap() {
  h5MapController?.destroy()
  h5MapController = null
}

function getSelectedCoordinate(): LocationCoordinate {
  return {
    latitude: locationPicker.latitude,
    longitude: locationPicker.longitude,
  }
}

function cycleVisibility() {
  form.visibility = form.visibility === 'public'
    ? 'followers'
    : form.visibility === 'followers'
      ? 'private'
      : 'public'
}

onLoad((options) => {
  postId.value = Number(options?.id || 0)
  form.relatedRecipeId = String(options?.relatedRecipeId || '')
  form.sourceType = String(options?.sourceType || 'normal')
  if (postId.value) {
    loadDetail(postId.value)
  }
})

onUnload(() => {
  destroyH5LocationMap()
})
</script>

<template>
  <view class="page-body post-form-page">
    <view class="form-nav">
      <view class="form-nav__back" @click="goBack">✕</view>
      <view class="form-nav__title">{{ postId ? '编辑动态' : '发布动态' }}</view>
      <button class="form-nav__submit" @click="submit">发布</button>
    </view>

    <view class="surface-card editor-card">
      <textarea
        v-model="form.content"
        class="editor-card__textarea"
        maxlength="500"
        placeholder="分享你的美食故事、烹饪心得..."
      />
      <view class="editor-card__count">{{ charCount }}/500</view>

      <view class="image-grid">
        <view
          v-for="(item, index) in mediaItems"
          :key="item.id"
          class="image-grid__item"
          :style="item.url ? undefined : { background: gradients[index % gradients.length] }"
        >
          <image v-if="item.url" class="image-grid__image" :src="item.url" mode="aspectFill" />
          <template v-else>{{ imageIcons[index % imageIcons.length] }}</template>
          <view class="image-grid__remove" @click.stop="removePreviewImage(index)">✕</view>
        </view>
        <view v-if="mediaItems.length < 9" class="image-grid__add" @click="addPreviewImage">
          <text class="icon">📷</text>
          <text>添加图片</text>
        </view>
      </view>

      <view v-if="recipeSelected" class="recipe-link-card">
        <text class="icon">📖</text>
        <text class="name">关联菜谱：{{ selectedRecipeTitle }}</text>
        <text class="remove" @click="clearRecipeLink">✕</text>
      </view>
    </view>

    <view class="surface-card option-card">
      <view class="option-row" @click="openRecipePicker">
        <text class="icon">📖</text>
        <text class="label">关联菜谱</text>
        <text class="value">{{ recipeSelected ? selectedRecipeTitle : '未选择' }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="option-row">
        <text class="icon">🏷️</text>
        <text class="label">自定义话题</text>
        <input v-model="form.topicCodesText" class="option-input" placeholder="最多5个，用逗号分隔" />
        <text class="option-preview">{{ topicsPreview }}</text>
      </view>
      <view class="option-row">
        <text class="icon">📍</text>
        <text class="label">添加位置</text>
        <view class="location-display" @click="openLocationPicker">
          <text :class="['location-display__text', { muted: !form.location }]">
            {{ form.location || '未选择位置' }}
          </text>
        </view>
        <button class="location-button" @click.stop="openLocationPicker">选择位置</button>
        <text v-if="form.location" class="location-clear" @click.stop="clearLocation">清除</text>
      </view>
      <view class="option-row" @click="cycleVisibility">
        <text class="icon">👁️</text>
        <text class="label">谁可以看</text>
        <text class="value">{{ visibilityLabel }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view v-if="recipePicker.visible" class="recipe-picker">
      <view class="recipe-picker__mask" @click="closeRecipePicker" />
      <view class="recipe-picker__panel">
        <view class="recipe-picker__header">
          <view>
            <view class="recipe-picker__title">选择关联菜谱</view>
            <view class="recipe-picker__subtitle">从站内已发布菜谱中选择</view>
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
              :class="{ active: String(item.id) === form.relatedRecipeId }"
              @click="selectRecipe(item)"
            >
              <view class="recipe-picker__cover">📖</view>
              <view class="recipe-picker__body">
                <view class="recipe-picker__name">{{ item.title || `菜谱 #${item.id}` }}</view>
                <view class="recipe-picker__meta">{{ item.authorNickname || '未知作者' }} · {{ item.cookTime || '未填写时间' }}</view>
              </view>
            </view>
          </template>
        </scroll-view>
      </view>
    </view>

    <view v-if="locationPicker.visible" class="location-picker">
      <view class="location-picker__mask" @click="closeLocationPicker" />
      <view class="location-picker__panel">
        <view class="location-picker__header">
          <view>
            <view class="location-picker__title">选择位置</view>
            <view class="location-picker__subtitle">点击地图或选择附近地点</view>
          </view>
          <view class="location-picker__close" @click="closeLocationPicker">✕</view>
        </view>

        <view class="location-map-shell">
          <!-- #ifdef MP-WEIXIN -->
          <map
            id="postLocationMap"
            class="location-map"
            :latitude="locationPicker.latitude"
            :longitude="locationPicker.longitude"
            :scale="16"
            :show-location="true"
            @tap="handleLocationMapTap"
          />
          <cover-view class="location-map-pin" />
          <cover-view v-if="locationPicker.loading || locationPicker.resolving" class="location-map-loading">
            {{ locationPicker.loading ? '定位中...' : '解析位置中...' }}
          </cover-view>
          <cover-view class="location-map-locate" @click.stop="relocateCurrentPosition">定位</cover-view>
          <!-- #endif -->
          <!-- #ifdef H5 -->
          <view id="postLocationAmap" class="location-map location-map--h5"></view>
          <view class="location-map-pin" />
          <view v-if="locationPicker.loading || locationPicker.resolving" class="location-map-loading">
            {{ locationPicker.loading ? '定位中...' : '解析位置中...' }}
          </view>
          <view class="location-map-locate" @click.stop="relocateCurrentPosition">定位</view>
          <!-- #endif -->
        </view>

        <view class="selected-location-card">
          <text class="selected-location-card__label">当前选择</text>
          <text class="selected-location-card__name">{{ selectedLocationLabel }}</text>
          <text v-if="locationPicker.selectedAddress" class="selected-location-card__address">
            {{ locationPicker.selectedAddress }}
          </text>
        </view>

        <scroll-view scroll-y class="poi-list">
          <view v-if="!locationPicker.pois.length" class="poi-empty">
            {{ locationPicker.loading ? '正在加载附近地点' : '暂无附近地点，可直接确认地图选点' }}
          </view>
          <view
            v-for="poi in locationPicker.pois"
            :key="poi.id"
            :class="['poi-item', { active: locationPicker.selectedPoiId === poi.id }]"
            @click="selectLocationPoi(poi)"
          >
            <view class="poi-item__main">
              <text class="poi-item__name">{{ poi.name }}</text>
              <text class="poi-item__address">{{ poi.address || '暂无详细地址' }}</text>
            </view>
            <text v-if="poi.distance !== undefined" class="poi-item__distance">{{ poi.distance }}m</text>
          </view>
        </scroll-view>

        <view class="location-picker__footer">
          <button class="location-picker__cancel" @click="closeLocationPicker">取消</button>
          <button
            class="location-picker__confirm"
            :disabled="!canConfirmLocation"
            @click="confirmLocationPicker"
          >
            确认位置
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.post-form-page {
  padding-bottom: 32rpx;
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

.editor-card,
.option-card {
  margin: 18rpx 28rpx 0;
  padding: 24rpx;
}

.editor-card__textarea {
  width: 100%;
  min-height: 220rpx;
  font-size: 28rpx;
  line-height: 1.7;
}

.editor-card__count {
  margin-top: 10rpx;
  text-align: right;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10rpx;
  margin-top: 18rpx;
}

.image-grid__item,
.image-grid__add {
  aspect-ratio: 1;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.image-grid__item {
  color: rgba(255, 255, 255, 0.92);
  font-size: 56rpx;
  overflow: hidden;
}

.image-grid__image {
  width: 100%;
  height: 100%;
}

.image-grid__remove {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.42);
  color: #fff;
  font-size: 18rpx;
  line-height: 34rpx;
  text-align: center;
}

.image-grid__add {
  flex-direction: column;
  gap: 6rpx;
  border: 2rpx dashed rgba(15, 23, 42, 0.12);
  color: var(--app-text-muted);
  font-size: 20rpx;
}

.image-grid__add .icon {
  font-size: 42rpx;
}

.recipe-link-card {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-top: 18rpx;
  padding: 16rpx 18rpx;
  border-radius: 18rpx;
  background: var(--app-warning-soft);
  color: var(--app-warning);
}

.recipe-link-card .name {
  flex: 1;
  font-size: 24rpx;
  font-weight: 600;
}

.recipe-link-card .remove {
  font-size: 24rpx;
  color: var(--app-text-muted);
}

.option-card {
  padding-top: 0;
  padding-bottom: 0;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.05);
}

.option-row:last-child {
  border-bottom: 0;
}

.option-row .icon {
  font-size: 32rpx;
}

.option-row .label {
  min-width: 120rpx;
  font-size: 26rpx;
  color: var(--app-text);
}

.option-row .value,
.option-preview {
  margin-left: auto;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.option-input {
  flex: 1;
  min-width: 0;
  height: 56rpx;
  font-size: 22rpx;
  text-align: right;
}

.location-display {
  flex: 1;
  min-width: 0;
  text-align: right;
}

.location-display__text {
  display: block;
  overflow: hidden;
  color: var(--app-text);
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-display__text.muted {
  color: var(--app-text-muted);
}

.location-button,
.recipe-picker__search-btn,
.location-picker__cancel,
.location-picker__confirm {
  margin: 0;
  border-radius: 999rpx;
  font-size: 22rpx;
}

.location-button::after,
.recipe-picker__search-btn::after,
.location-picker__cancel::after,
.location-picker__confirm::after {
  border: 0;
}

.location-button {
  height: 52rpx;
  padding: 0 18rpx;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  line-height: 52rpx;
}

.location-clear {
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.arrow {
  color: var(--app-text-muted);
  font-size: 24rpx;
}

.recipe-picker,
.location-picker {
  position: fixed;
  inset: 0;
  z-index: 100;
}

.recipe-picker__mask,
.location-picker__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.34);
}

.recipe-picker__panel,
.location-picker__panel {
  position: absolute;
  right: 24rpx;
  bottom: 28rpx;
  left: 24rpx;
  overflow: hidden;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 24rpx 70rpx rgba(15, 23, 42, 0.18);
}

.recipe-picker__header,
.location-picker__header,
.location-picker__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 22rpx 24rpx;
}

.recipe-picker__title,
.location-picker__title {
  color: var(--app-text);
  font-size: 30rpx;
  font-weight: 700;
}

.recipe-picker__subtitle,
.location-picker__subtitle {
  margin-top: 4rpx;
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.recipe-picker__close,
.location-picker__close {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.06);
  color: var(--app-text-muted);
  font-size: 26rpx;
  line-height: 56rpx;
  text-align: center;
}

.recipe-picker__search {
  display: flex;
  gap: 12rpx;
  padding: 0 24rpx 18rpx;
}

.recipe-picker__input {
  flex: 1;
  min-width: 0;
  height: 64rpx;
  padding: 0 18rpx;
  border-radius: 16rpx;
  background: var(--app-surface-muted);
  color: var(--app-text);
  font-size: 24rpx;
}

.recipe-picker__search-btn {
  width: 112rpx;
  height: 64rpx;
  padding: 0;
  background: var(--app-primary);
  color: #fff;
  font-size: 23rpx;
  line-height: 64rpx;
}

.recipe-picker__list {
  height: 520rpx;
  padding: 0 24rpx 24rpx;
  box-sizing: border-box;
}

.recipe-picker__empty {
  padding: 80rpx 0;
  color: var(--app-text-muted);
  font-size: 24rpx;
  text-align: center;
}

.recipe-picker__item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.recipe-picker__item.active .recipe-picker__name {
  color: var(--app-primary);
}

.recipe-picker__cover {
  width: 72rpx;
  height: 72rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-warning-soft);
  color: var(--app-warning);
  font-size: 30rpx;
  flex-shrink: 0;
}

.recipe-picker__body {
  flex: 1;
  min-width: 0;
}

.recipe-picker__name,
.recipe-picker__meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-picker__name {
  color: var(--app-text);
  font-size: 25rpx;
  font-weight: 700;
}

.recipe-picker__meta {
  margin-top: 6rpx;
  color: var(--app-text-muted);
  font-size: 21rpx;
}

.location-map-shell {
  position: relative;
  height: 420rpx;
  margin: 0 24rpx;
  overflow: hidden;
  border-radius: 20rpx;
  background: #eef2f7;
}

.location-map {
  width: 100%;
  height: 420rpx;
}

.location-map--h5 {
  display: block;
}

.location-map-pin {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 2;
  width: 30rpx;
  height: 30rpx;
  margin-top: -30rpx;
  margin-left: -15rpx;
  border: 6rpx solid #fff;
  border-radius: 50%;
  background: var(--app-primary);
  box-shadow: 0 8rpx 18rpx rgba(15, 23, 42, 0.24);
}

.location-map-loading {
  position: absolute;
  top: 18rpx;
  left: 18rpx;
  z-index: 3;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.68);
  color: #fff;
  font-size: 20rpx;
}

.location-map-locate {
  position: absolute;
  right: 18rpx;
  bottom: 18rpx;
  z-index: 3;
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #fff;
  color: var(--app-primary);
  font-size: 22rpx;
  box-shadow: 0 8rpx 18rpx rgba(15, 23, 42, 0.16);
}

.selected-location-card {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  margin: 18rpx 24rpx 0;
  padding: 18rpx;
  border-radius: 18rpx;
  background: rgba(255, 107, 53, 0.08);
}

.selected-location-card__label {
  color: var(--app-primary);
  font-size: 20rpx;
  font-weight: 700;
}

.selected-location-card__name {
  color: var(--app-text);
  font-size: 26rpx;
  font-weight: 700;
}

.selected-location-card__address {
  color: var(--app-text-muted);
  font-size: 22rpx;
  line-height: 1.4;
}

.poi-list {
  height: 300rpx;
  margin-top: 12rpx;
  padding: 0 24rpx;
  box-sizing: border-box;
}

.poi-empty {
  padding: 56rpx 0;
  color: var(--app-text-muted);
  font-size: 24rpx;
  text-align: center;
}

.poi-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.poi-item.active .poi-item__name {
  color: var(--app-primary);
}

.poi-item__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.poi-item__name,
.poi-item__address {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.poi-item__name {
  color: var(--app-text);
  font-size: 25rpx;
  font-weight: 700;
}

.poi-item__address,
.poi-item__distance {
  color: var(--app-text-muted);
  font-size: 21rpx;
}

.location-picker__footer {
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.location-picker__cancel,
.location-picker__confirm {
  flex: 1;
  height: 66rpx;
  line-height: 66rpx;
}

.location-picker__cancel {
  background: rgba(15, 23, 42, 0.06);
  color: var(--app-text);
}

.location-picker__confirm {
  background: linear-gradient(135deg, var(--app-primary), #ff9b54);
  color: #fff;
}

.location-picker__confirm[disabled] {
  opacity: 0.5;
}
</style>
