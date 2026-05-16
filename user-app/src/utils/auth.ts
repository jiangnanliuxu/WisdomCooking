const TOKEN_KEY = 'cook_user_token'
const USER_KEY = 'cook_user_profile'

export function getUserToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setUserToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function clearUserToken() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}

export function setStoredUser(user: unknown) {
  uni.setStorageSync(USER_KEY, user)
}

export function getStoredUser<T>() {
  return (uni.getStorageSync(USER_KEY) || null) as T | null
}
