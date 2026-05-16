<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { logout } from '@/api/auth'
import { uploadImage } from '@/api/operation'
import { getCurrentUser, updateCurrentUser, updateInterests } from '@/api/user'
import { CHINESE_CUISINES, CHINESE_CUISINE_LABELS } from '@/constants/cuisine'
import { useAuthStore } from '@/stores/auth'
import { mediaIdToRawUrl, resolveAssetUrl } from '@/utils/media'
import {
  getCurrentCoordinate,
  isAmapConfigured,
  normalizeLocationText,
  requestUserLocationPermission,
  reverseGeocodeAdministrativeRegion,
} from '@/utils/amap'

const tagOptions = CHINESE_CUISINES
const genderOptions = ['男', '女', '其他']
const authStore = useAuthStore()
const minBirthday = '1900-01-01'
const maxBirthday = formatDate(new Date())

const form = reactive({
  nickname: '',
  avatarUrl: '',
  gender: '女',
  birthday: '',
  region: '',
  bio: '',
  phone: '',
  interests: [] as string[],
})

const genderLabel = computed(() => form.gender || '未设置')
const birthdayLabel = computed(() => form.birthday || '未设置')
const regionLabel = computed(() => form.region || '未设置')
const avatarSrc = computed(() => resolveAssetUrl(form.avatarUrl))
const selectedGenderIndex = computed(() => {
  const index = genderOptions.indexOf(form.gender)
  return index >= 0 ? index : 0
})

async function loadData() {
  const response = await getCurrentUser()
  const data = response.data
  if (!data) return
  form.nickname = data.nickname || ''
  form.avatarUrl = data.avatarUrl || ''
  form.gender = normalizeGender(data.gender)
  form.birthday = data.birthday || ''
  form.region = data.region || ''
  form.bio = data.bio || ''
  form.phone = data.phone || ''
  form.interests = normalizeInterestTags(data.interestTags || []).slice(0, 5)
}

function toggleTag(tag: string) {
  if (form.interests.includes(tag)) {
    form.interests = form.interests.filter(item => item !== tag)
    return
  }
  if (form.interests.length >= 5) {
    uni.showToast({ title: '最多选择5个标签', icon: 'none' })
    return
  }
  form.interests = [...form.interests, tag]
}

async function submit() {
  await updateCurrentUser(buildProfilePayload())
  await updateInterests({ interestTags: form.interests })
  await authStore.refreshProfile()
  uni.showToast({ title: '资料已保存', icon: 'none' })
  uni.navigateBack()
}

function buildProfilePayload() {
  return {
    nickname: form.nickname,
    avatarUrl: form.avatarUrl,
    gender: form.gender,
    birthday: form.birthday,
    region: form.region,
    bio: form.bio,
  }
}

function goBack() {
  uni.navigateBack()
}

function pickAvatar() {
  uni.chooseImage({
    count: 1,
    success: async ({ tempFilePaths }) => {
      const [filePath] = tempFilePaths || []
      if (!filePath) return
      const response = await uploadImage(filePath)
      const avatarUrl = response.data?.url || mediaIdToRawUrl(response.data?.id)
      if (!avatarUrl) {
        uni.showToast({ title: '头像上传失败', icon: 'none' })
        return
      }
      form.avatarUrl = avatarUrl
      await updateCurrentUser(buildProfilePayload())
      await authStore.refreshProfile()
      uni.showToast({ title: '头像已更新', icon: 'none' })
    },
  })
}

function changeGender(event: { detail: { value: number } }) {
  form.gender = genderOptions[event.detail.value] || genderOptions[0]
}

function changeBirthday(event: { detail: { value: string } }) {
  form.birthday = event.detail.value || ''
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function pickRegion() {
  if (!isAmapConfigured()) {
    uni.showToast({ title: '地图服务未配置', icon: 'none' })
    return
  }
  uni.showLoading({ title: '定位中...' })
  try {
    await requestUserLocationPermission()
    const coordinate = await getCurrentCoordinate()
    const region = await reverseGeocodeAdministrativeRegion(coordinate)
    const text = normalizeLocationText(region.text)
    if (!text) {
      uni.showToast({ title: '未获取到地区', icon: 'none' })
      return
    }
    form.region = text
  }
  catch (error) {
    const message = error instanceof Error ? error.message : ''
    const title = message === 'LOCATION_PERMISSION_DENIED'
      ? '请允许定位权限'
      : message === 'AMAP_WEB_LOADER_FAILED'
        ? '地图加载失败'
        : '地区定位失败'
    uni.showToast({ title, icon: 'none' })
  }
  finally {
    uni.hideLoading()
  }
}

function normalizeGender(value?: string) {
  return genderOptions.includes(value || '') ? String(value) : '其他'
}

function normalizeInterestTags(tags: string[]) {
  const labels = new Map<string, string>(tagOptions.map(item => [item.label, item.label]))
  const allowedLabels = new Set<string>(CHINESE_CUISINE_LABELS)
  tagOptions.forEach((item) => {
    labels.set(`${item.icon} ${item.label}`, item.label)
  })
  return Array.from(new Set(tags.map(item => labels.get(item.trim()) || '').filter(item => allowedLabels.has(item))))
}

async function handleLogout() {
  try {
    await logout()
  }
  finally {
    authStore.logout()
    uni.navigateBack()
  }
}

onMounted(loadData)
</script>

<template>
  <view class="page-body profile-edit-page">
    <view class="page-nav">
      <view class="page-nav__left">
        <view class="page-nav__back" @click="goBack">‹</view>
        <view class="page-nav__title">编辑资料</view>
      </view>
      <view class="page-nav__save" @click="submit">保存</view>
    </view>

    <view class="surface-card avatar-card">
      <view class="avatar-wrap" @click="pickAvatar">
        <view class="avatar-main">
          <image v-if="avatarSrc" class="avatar-main__image" :src="avatarSrc" mode="aspectFill" />
          <template v-else>{{ form.nickname?.slice(0, 1) || '😊' }}</template>
        </view>
        <view class="avatar-edit">📷</view>
      </view>
      <view class="avatar-tip">点击更换头像</view>
    </view>

    <view class="surface-card form-card">
      <view class="form-row">
        <text class="label">昵称</text>
        <input v-model="form.nickname" class="value-input" placeholder="请输入昵称" />
        <text class="arrow">›</text>
      </view>
      <view class="form-row multi">
        <text class="label">个性签名</text>
        <textarea v-model="form.bio" class="value-textarea" placeholder="写点什么介绍自己吧" />
        <text class="arrow">›</text>
      </view>
      <view class="form-row">
        <text class="label">性别</text>
        <picker class="value-picker" :range="genderOptions" :value="selectedGenderIndex" @change="changeGender">
          <view class="value-picker__text">{{ genderLabel }}</view>
        </picker>
        <text class="arrow">›</text>
      </view>
      <view class="form-row">
        <text class="label">生日</text>
        <picker
          class="value-picker"
          mode="date"
          :value="form.birthday || maxBirthday"
          :start="minBirthday"
          :end="maxBirthday"
          @change="changeBirthday"
        >
          <view class="value-picker__text">{{ birthdayLabel }}</view>
        </picker>
        <text class="arrow">›</text>
      </view>
      <view class="form-row" @click="pickRegion">
        <text class="label">地区</text>
        <text class="value-text">{{ regionLabel }}</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="surface-card tag-card">
      <view class="tag-card__title">兴趣标签（最多5个）</view>
      <view class="tag-list">
        <view
          v-for="tag in tagOptions"
          :key="tag.label"
          class="tag-item"
          :class="{ selected: form.interests.includes(tag.label) }"
          @click="toggleTag(tag.label)"
        >
          {{ tag.icon }} {{ tag.label }}
        </view>
      </view>
    </view>

    <view class="surface-card form-card compact">
      <view class="form-row">
        <text class="label">手机号</text>
        <text class="value-text">{{ form.phone || '未绑定' }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="form-row">
        <text class="label">微信</text>
        <text class="value-text muted">未绑定</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="surface-card danger-card">
      <view class="danger-action" @click="handleLogout">退出登录</view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.profile-edit-page {
  padding-bottom: 36rpx;
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

.page-nav__save {
  color: var(--app-primary);
  font-size: 26rpx;
  font-weight: 700;
}

.avatar-card,
.form-card,
.tag-card,
.danger-card {
  margin: 18rpx 28rpx 0;
}

.avatar-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 28rpx 24rpx;
}

.avatar-wrap {
  position: relative;
}

.avatar-main {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(232, 109, 47, 0.16);
  border: 6rpx solid var(--app-primary);
  color: var(--app-primary);
  font-size: 60rpx;
  font-weight: 700;
}

.avatar-main__image {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar-edit {
  position: absolute;
  right: -4rpx;
  bottom: -4rpx;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--app-primary);
  border: 4rpx solid #fff;
  color: #fff;
  font-size: 22rpx;
}

.avatar-tip {
  color: var(--app-text-muted);
  font-size: 22rpx;
}

.form-card {
  padding: 0 24rpx;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  min-height: 92rpx;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.05);
}

.form-row:last-child {
  border-bottom: 0;
}

.form-row.multi {
  align-items: flex-start;
  padding: 22rpx 0;
  min-height: 0;
}

.label {
  width: 110rpx;
  flex-shrink: 0;
  color: var(--app-text);
  font-size: 24rpx;
}

.value-input,
.value-textarea,
.value-picker {
  flex: 1;
  min-width: 0;
  color: var(--app-text);
  font-size: 24rpx;
  text-align: right;
}

.value-picker__text {
  color: var(--app-text-soft);
  font-size: 24rpx;
  text-align: right;
}

.value-textarea {
  height: 100rpx;
  line-height: 1.6;
}

.align-right {
  text-align: right;
}

.value-text {
  flex: 1;
  text-align: right;
  color: var(--app-text-soft);
  font-size: 24rpx;
}

.value-text.muted {
  color: #c6cbd4;
}

.arrow {
  color: var(--app-text-muted);
  font-size: 24rpx;
}

.tag-card {
  padding: 24rpx;
}

.tag-card__title {
  color: var(--app-text);
  font-size: 26rpx;
  font-weight: 700;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.tag-item {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  border: 1rpx solid rgba(15, 23, 42, 0.12);
  color: var(--app-text-soft);
  font-size: 22rpx;
}

.tag-item.selected {
  background: var(--app-warning-soft);
  border-color: rgba(232, 109, 47, 0.24);
  color: var(--app-primary);
}

.form-card.compact {
  padding-top: 0;
  padding-bottom: 0;
}

.danger-card {
  padding: 0 24rpx;
}

.danger-action {
  padding: 28rpx 0;
  text-align: center;
  color: var(--app-danger);
  font-size: 26rpx;
}
</style>
