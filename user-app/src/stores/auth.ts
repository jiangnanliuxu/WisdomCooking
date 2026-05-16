import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser } from '@/api/user'
import type { UserProfile } from '@/types/cook'
import { clearUserToken, getStoredUser, getUserToken, setStoredUser, setUserToken } from '@/utils/auth'

/**
 * 用户端登录态。
 * 负责缓存 token 和用户资料，并为需要登录的页面提供统一判断。
 */
export const useAuthStore = defineStore('user-auth', () => {
  const token = ref(getUserToken())
  const profile = ref<UserProfile | null>(getStoredUser<UserProfile>())

  const isLoggedIn = computed(() => Boolean(token.value))

  function setLoginState(nextToken: string, user: UserProfile) {
    token.value = nextToken
    profile.value = user
    setUserToken(nextToken)
    setStoredUser(user)
  }

  async function refreshProfile() {
    const response = await getCurrentUser()
    profile.value = response.data || null
    if (profile.value) {
      setStoredUser(profile.value)
    }
  }

  function logout() {
    token.value = ''
    profile.value = null
    clearUserToken()
  }

  return {
    token,
    profile,
    isLoggedIn,
    setLoginState,
    refreshProfile,
    logout,
  }
})
