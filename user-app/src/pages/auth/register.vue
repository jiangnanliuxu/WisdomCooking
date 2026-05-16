<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { register } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const submitting = ref(false)

const form = reactive({
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

function isValidPhone(phone: string) {
  return /^1\d{10}$/.test(phone)
}

const canSubmit = computed(() => {
  return isValidPhone(form.phone)
    && form.nickname.trim().length >= 2
    && form.password.trim().length >= 6
    && form.password === form.confirmPassword
    && !submitting.value
})

async function submit() {
  if (!isValidPhone(form.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (form.nickname.trim().length < 2) {
    uni.showToast({ title: '昵称至少 2 个字符', icon: 'none' })
    return
  }
  if (form.password.trim().length < 6) {
    uni.showToast({ title: '密码长度至少 6 位', icon: 'none' })
    return
  }
  if (form.password !== form.confirmPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const response = await register(form)
    if (response.data) {
      authStore.setLoginState(response.data.accessToken, response.data.user)
    }
    uni.reLaunch({ url: '/pages/profile/index' })
  }
  finally {
    submitting.value = false
  }
}

function goLogin() {
  uni.navigateBack()
}
</script>

<template>
  <view class="auth-page auth-page--register">
    <view class="auth-shell">
      <view class="hero-panel">
        <view class="hero-panel__eyebrow">新用户注册</view>
        <view class="hero-panel__title">创建你的饮食管理空间</view>
        <view class="hero-panel__subtitle">记录偏好、沉淀菜谱、加入社区讨论，后续所有推荐都会围绕你的习惯展开。</view>
        <view class="hero-grid">
          <view class="hero-grid__card">
            <view class="hero-grid__title">专属偏好</view>
            <view class="hero-grid__desc">从口味到热量目标，资料越完整，推荐越准确。</view>
          </view>
          <view class="hero-grid__card hero-grid__card--accent">
            <view class="hero-grid__title">社区互动</view>
            <view class="hero-grid__desc">发布菜谱、动态和提问，个人内容会自动沉淀到主页。</view>
          </view>
        </view>
      </view>

      <view class="auth-card">
        <view class="auth-card__header">
          <view>
            <view class="auth-card__title">创建账号</view>
            <view class="auth-card__desc">用手机号快速注册，登录后即可完善个人资料。</view>
          </view>
          <view class="auth-card__badge">账号开通</view>
        </view>

        <view class="field-list">
          <view class="field-card">
            <view class="field-card__label">手机号</view>
            <input
              v-model="form.phone"
              class="field-card__input"
              type="number"
              maxlength="11"
              placeholder="请输入 11 位手机号"
              placeholder-class="field-card__placeholder"
            />
          </view>

          <view class="field-card">
            <view class="field-card__label">昵称</view>
            <input
              v-model="form.nickname"
              class="field-card__input"
              maxlength="20"
              placeholder="用于社区展示，例如：清蒸鲈鱼研究员"
              placeholder-class="field-card__placeholder"
            />
          </view>

          <view class="field-card">
            <view class="field-card__label">登录密码</view>
            <input
              v-model="form.password"
              class="field-card__input"
              password
              maxlength="32"
              placeholder="至少 6 位，建议字母和数字组合"
              placeholder-class="field-card__placeholder"
            />
          </view>

          <view class="field-card">
            <view class="field-card__label">确认密码</view>
            <input
              v-model="form.confirmPassword"
              class="field-card__input"
              password
              maxlength="32"
              placeholder="请再次输入密码"
              placeholder-class="field-card__placeholder"
            />
          </view>
        </view>

        <button class="primary-button" :disabled="!canSubmit" @click="submit">
          {{ submitting ? '注册中...' : '完成注册' }}
        </button>

        <view class="auth-footer">
          <text class="auth-footer__tip">注册即默认同意平台基础服务协议与隐私规则。</text>
          <text class="auth-footer__link" @click="goLogin">已有账号，去登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.auth-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + 24rpx) 24rpx 48rpx;
  background:
    linear-gradient(180deg, #eefbf4 0%, #f8f6ff 220rpx, #f3f5f7 100%);
}

.auth-page--register {
  background:
    linear-gradient(180deg, #eefbf4 0%, #fff7ed 220rpx, #f4f6f8 100%);
}

.auth-shell {
  display: grid;
  gap: 24rpx;
}

.hero-panel {
  padding: 32rpx 32rpx 8rpx;
}

.hero-panel__eyebrow {
  display: inline-flex;
  align-items: center;
  height: 48rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(22, 163, 74, 0.12);
  color: #15803d;
  font-size: 22rpx;
  font-weight: 600;
}

.hero-panel__title {
  margin-top: 24rpx;
  font-size: 54rpx;
  line-height: 1.18;
  font-weight: 700;
  color: #111827;
}

.hero-panel__subtitle {
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #536274;
}

.hero-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 28rpx;
}

.hero-grid__card {
  min-height: 168rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.05);
}

.hero-grid__card--accent {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.84), rgba(255, 244, 230, 0.96));
}

.hero-grid__title {
  font-size: 30rpx;
  font-weight: 600;
  color: #111827;
}

.hero-grid__desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #64748b;
}

.auth-card {
  padding: 32rpx 28rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20rpx 60rpx rgba(15, 23, 42, 0.08);
}

.auth-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16rpx;
}

.auth-card__title {
  font-size: 42rpx;
  font-weight: 700;
  color: #111827;
}

.auth-card__desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #64748b;
}

.auth-card__badge {
  flex-shrink: 0;
  min-width: 132rpx;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #ecfdf3;
  color: #15803d;
  font-size: 22rpx;
  text-align: center;
}

.field-list {
  display: grid;
  gap: 18rpx;
  margin-top: 28rpx;
}

.field-card {
  padding: 20rpx 22rpx;
  border-radius: 24rpx;
  background: #f7f8fa;
  border: 1rpx solid #edf0f3;
}

.field-card__label {
  font-size: 22rpx;
  color: #8a94a6;
}

.field-card__input {
  width: 100%;
  height: 64rpx;
  margin-top: 8rpx;
  font-size: 30rpx;
  color: #111827;
}

.field-card__placeholder {
  color: #b2bac6;
}

.primary-button::after {
  border: none;
}

.primary-button {
  margin-top: 28rpx;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #16a34a, #f97316);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 18rpx 32rpx rgba(22, 163, 74, 0.22);
}

.primary-button[disabled] {
  background: #d7dbe1;
  box-shadow: none;
}

.auth-footer {
  display: grid;
  gap: 16rpx;
  margin-top: 28rpx;
}

.auth-footer__tip {
  font-size: 22rpx;
  line-height: 1.6;
  color: #94a3b8;
}

.auth-footer__link {
  font-size: 24rpx;
  color: #ea580c;
  font-weight: 600;
}
</style>
