export function formatTime(value?: string) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

export function requireLogin() {
  const token = uni.getStorageSync('cook_user_token')
  if (!token) {
    uni.navigateTo({ url: '/pages/auth/login' })
    return false
  }
  return true
}
