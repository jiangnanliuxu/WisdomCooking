<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { login, sendCode } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const mode = ref<'password' | 'code'>('password')
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  phone: '',
  password: '',
  code: '',
})

/**
 * 手机号基础校验，先在前端挡住明显非法输入，减少无意义接口请求。
 */
function isValidPhone(phone: string) {
  return /^1\d{10}$/.test(phone)
}

const canRequestCode = computed(() => isValidPhone(form.phone) && countdown.value === 0 && !sending.value)
const canSubmit = computed(() => {
  if (!isValidPhone(form.phone) || submitting.value) {
    return false
  }
  return mode.value === 'password' ? form.password.trim().length >= 6 : form.code.trim().length >= 4
})

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

/**
 * 验证码发送后启动 60 秒倒计时，避免重复点击和重复下发。
 */
function startCountdown() {
  countdown.value = 60
  stopCountdown()
  countdownTimer = setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0
      stopCountdown()
      return
    }
    countdown.value -= 1
  }, 1000)
}

async function requestCode() {
  if (!isValidPhone(form.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (countdown.value > 0 || sending.value) {
    return
  }
  sending.value = true
  try {
    await sendCode({ phone: form.phone, scene: 'login' })
    uni.showToast({ title: '验证码已发送', icon: 'none' })
    startCountdown()
  }
  finally {
    sending.value = false
  }
}

async function submit() {
  if (!isValidPhone(form.phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (mode.value === 'password' && form.password.trim().length < 6) {
    uni.showToast({ title: '密码长度至少 6 位', icon: 'none' })
    return
  }
  if (mode.value === 'code' && form.code.trim().length < 4) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }

  submitting.value = true
  try {
    const response = await login({
      phone: form.phone,
      password: mode.value === 'password' ? form.password : undefined,
      code: mode.value === 'code' ? form.code : undefined,
    })
    if (response.data) {
      authStore.setLoginState(response.data.accessToken, response.data.user)
    }
    uni.reLaunch({ url: '/pages/profile/index' })
  }
  finally {
    submitting.value = false
  }
}

function switchMode(nextMode: 'password' | 'code') {
  mode.value = nextMode
  if (nextMode === 'password') {
    form.code = ''
  }
  else {
    form.password = ''
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/auth/register' })
}

function goResetPassword() {
  uni.navigateTo({ url: '/pages/auth/reset-password' })
}

onUnmounted(() => {
  stopCountdown()
})
</script>

<template>
  <view class="auth-page">
    <view class="auth-shell">
      <view class="hero-panel">
        <view class="hero-panel__eyebrow">码上智厨</view>
        <view class="hero-panel__title">登录后继续管理你的三餐与灵感</view>
        <view class="hero-panel__subtitle">从家常菜谱、AI 营养建议到社区互动，常用能力放在一个入口里。</view>
        <view class="hero-panel__chips">
          <view class="hero-chip">
            <text class="hero-chip__value">24h</text>
            <text class="hero-chip__label">饮食记录随手查</text>
          </view>
          <view class="hero-chip">
            <text class="hero-chip__value">AI</text>
            <text class="hero-chip__label">对话与识图联动</text>
          </view>
        </view>
      </view>

      <view class="auth-card">
        <view class="auth-card__header">
          <view>
            <view class="auth-card__title">欢迎回来</view>
            <view class="auth-card__desc">手机号登录，支持密码和验证码两种方式。</view>
          </view>
          <view class="auth-card__badge">账户登录</view>
        </view>

        <view class="mode-switch">
          <view
            class="mode-switch__item"
            :class="{ 'mode-switch__item--active': mode === 'password' }"
            @click="switchMode('password')"
          >
            密码登录
          </view>
          <view
            class="mode-switch__item"
            :class="{ 'mode-switch__item--active': mode === 'code' }"
            @click="switchMode('code')"
          >
            验证码登录
          </view>
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

          <view v-if="mode === 'password'" class="field-card">
            <view class="field-card__label">登录密码</view>
            <input
              v-model="form.password"
              class="field-card__input"
              password
              placeholder="请输入登录密码"
              placeholder-class="field-card__placeholder"
            />
          </view>

          <view v-else class="field-card field-card--inline">
            <view class="field-card__main">
              <view class="field-card__label">短信验证码</view>
              <input
                v-model="form.code"
                class="field-card__input"
                type="number"
                maxlength="6"
                placeholder="请输入验证码"
                placeholder-class="field-card__placeholder"
              />
            </view>
            <button class="code-button" :disabled="!canRequestCode" @click="requestCode">
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </button>
          </view>
        </view>

        <button class="primary-button" :disabled="!canSubmit" @click="submit">
          {{ submitting ? '登录中...' : '登录' }}
        </button>

        <view class="auth-links">
          <text class="auth-links__item" @click="goRegister">创建账号</text>
          <text class="auth-links__item auth-links__item--strong" @click="goResetPassword">忘记密码</text>
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
    linear-gradient(180deg, #fff4ea 0%, #f6f7fb 240rpx, #f3f5f7 100%);
}

.auth-shell {
  display: grid;
  gap: 24rpx;
}

.hero-panel {
  padding: 32rpx 32rpx 12rpx;
}

.hero-panel__eyebrow {
  display: inline-flex;
  align-items: center;
  height: 48rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(249, 115, 22, 0.12);
  color: #c2410c;
  font-size: 22rpx;
  font-weight: 600;
}

.hero-panel__title {
  margin-top: 24rpx;
  font-size: 56rpx;
  line-height: 1.18;
  font-weight: 700;
  color: #111827;
}

.hero-panel__subtitle {
  margin-top: 18rpx;
  font-size: 26rpx;
  line-height: 1.7;
  color: #526071;
}

.hero-panel__chips {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 26rpx;
}

.hero-chip {
  min-height: 144rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.05);
  border: 1rpx solid rgba(255, 255, 255, 0.72);
}

.hero-chip__value {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
  color: #111827;
}

.hero-chip__label {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.5;
  color: #5b6777;
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
  background: #eefdf3;
  color: #15803d;
  font-size: 22rpx;
  text-align: center;
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  padding: 10rpx;
  margin-top: 28rpx;
  border-radius: 24rpx;
  background: #f3f5f7;
}

.mode-switch__item {
  height: 80rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  color: #64748b;
  transition: all 0.2s ease;
}

.mode-switch__item--active {
  background: #ffffff;
  color: #ea580c;
  font-weight: 600;
  box-shadow: 0 12rpx 24rpx rgba(15, 23, 42, 0.06);
}

.field-list {
  display: grid;
  gap: 18rpx;
  margin-top: 24rpx;
}

.field-card {
  padding: 20rpx 22rpx;
  border-radius: 24rpx;
  background: #f7f8fa;
  border: 1rpx solid #edf0f3;
}

.field-card--inline {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.field-card__main {
  flex: 1;
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

.code-button {
  margin: 0;
  padding: 0 22rpx;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 22rpx;
  background: #fff1e8;
  color: #c2410c;
  font-size: 24rpx;
  border: none;
}

.code-button[disabled] {
  background: #e5e7eb;
  color: #94a3b8;
}

.code-button::after,
.primary-button::after {
  border: none;
}

.primary-button {
  margin-top: 28rpx;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 26rpx;
  background: linear-gradient(135deg, #f97316, #fb923c);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 18rpx 32rpx rgba(249, 115, 22, 0.28);
}

.primary-button[disabled] {
  background: #d7dbe1;
  box-shadow: none;
  color: #ffffff;
}

.auth-links {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 28rpx;
  font-size: 24rpx;
}

.auth-links__item {
  color: #64748b;
}

.auth-links__item--strong {
  color: #ea580c;
  font-weight: 600;
}
</style>
