<script setup lang="ts">
import { computed, onUnmounted, reactive, ref } from 'vue'
import { resetPassword, sendCode } from '@/api/auth'

const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  phone: '',
  code: '',
  password: '',
  confirmPassword: '',
})

function isValidPhone(phone: string) {
  return /^1\d{10}$/.test(phone)
}

const canRequestCode = computed(() => isValidPhone(form.phone) && countdown.value === 0 && !sending.value)
const canSubmit = computed(() => {
  return isValidPhone(form.phone)
    && form.code.trim().length >= 4
    && form.password.trim().length >= 6
    && form.password === form.confirmPassword
    && !submitting.value
})

function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

/**
 * 重置密码场景同样受验证码频控限制，这里复用统一倒计时逻辑。
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
    await sendCode({ phone: form.phone, scene: 'reset_password' })
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
  if (form.code.trim().length < 4) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
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
    await resetPassword(form)
    uni.showToast({ title: '密码已重置', icon: 'none' })
    uni.navigateBack()
  }
  finally {
    submitting.value = false
  }
}

function goLogin() {
  uni.navigateBack()
}

onUnmounted(() => {
  stopCountdown()
})
</script>

<template>
  <view class="auth-page">
    <view class="auth-shell">
      <view class="hero-panel">
        <view class="hero-panel__eyebrow">账户恢复</view>
        <view class="hero-panel__title">通过短信验证码重置登录密码</view>
        <view class="hero-panel__subtitle">重置后新密码会立即生效，后续请使用新密码或验证码登录。</view>
        <view class="hero-note">
          <view class="hero-note__title">安全提醒</view>
          <view class="hero-note__desc">请在常用设备上操作，并避免把验证码和密码透露给他人。</view>
        </view>
      </view>

      <view class="auth-card">
        <view class="auth-card__header">
          <view>
            <view class="auth-card__title">重置密码</view>
            <view class="auth-card__desc">验证手机号后即可设置新的登录密码。</view>
          </view>
          <view class="auth-card__badge">安全验证</view>
        </view>

        <view class="field-list">
          <view class="field-card">
            <view class="field-card__label">手机号</view>
            <input
              v-model="form.phone"
              class="field-card__input"
              type="number"
              maxlength="11"
              placeholder="请输入注册手机号"
              placeholder-class="field-card__placeholder"
            />
          </view>

          <view class="field-card field-card--inline">
            <view class="field-card__main">
              <view class="field-card__label">验证码</view>
              <input
                v-model="form.code"
                class="field-card__input"
                type="number"
                maxlength="6"
                placeholder="请输入短信验证码"
                placeholder-class="field-card__placeholder"
              />
            </view>
            <button class="code-button" :disabled="!canRequestCode" @click="requestCode">
              {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
            </button>
          </view>

          <view class="field-card">
            <view class="field-card__label">新密码</view>
            <input
              v-model="form.password"
              class="field-card__input"
              password
              maxlength="32"
              placeholder="至少 6 位"
              placeholder-class="field-card__placeholder"
            />
          </view>

          <view class="field-card">
            <view class="field-card__label">确认新密码</view>
            <input
              v-model="form.confirmPassword"
              class="field-card__input"
              password
              maxlength="32"
              placeholder="请再次输入新密码"
              placeholder-class="field-card__placeholder"
            />
          </view>
        </view>

        <button class="primary-button" :disabled="!canSubmit" @click="submit">
          {{ submitting ? '提交中...' : '确认重置' }}
        </button>

        <view class="auth-footer">
          <text class="auth-footer__tip">如仍无法登录，请联系管理员核验账号状态。</text>
          <text class="auth-footer__link" @click="goLogin">返回登录</text>
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
    linear-gradient(180deg, #eef7ff 0%, #fff7ed 220rpx, #f4f6f8 100%);
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
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
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

.hero-note {
  margin-top: 28rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.05);
}

.hero-note__title {
  font-size: 28rpx;
  font-weight: 600;
  color: #111827;
}

.hero-note__desc {
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
  background: #eff6ff;
  color: #1d4ed8;
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
  background: #eff6ff;
  color: #1d4ed8;
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
  background: linear-gradient(135deg, #2563eb, #f97316);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  box-shadow: 0 18rpx 32rpx rgba(37, 99, 235, 0.22);
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
