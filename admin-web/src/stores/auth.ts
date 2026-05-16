import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getAdminInfo, login, logout } from '@/api/auth'
import { getAdminToken, removeAdminToken, setAdminExpires, setAdminToken } from '@/utils/auth'
import type { AdminUserInfo } from '@/types/common'

/**
 * 管理端认证状态。
 * 复用若依后台 token 体系，并缓存管理员基础信息和权限。
 */
export const useAuthStore = defineStore('admin-auth', () => {
  const token = ref(getAdminToken())
  const profile = ref<AdminUserInfo['user'] | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const initialized = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))

  async function loginByPassword(payload: { username: string; password: string; code?: string; uuid?: string }) {
    const response = await login(payload)
    const data = response.data
    if (!data) {
      throw new Error('登录响应为空')
    }
    token.value = data.access_token
    setAdminToken(data.access_token)
    setAdminExpires(data.expires_in)
    await fetchProfile()
  }

  async function fetchProfile() {
    const response = await getAdminInfo()
    profile.value = response.user
    roles.value = response.roles || []
    permissions.value = response.permissions || []
    initialized.value = true
  }

  async function logoutCurrent() {
    try {
      await logout()
    }
    finally {
      clear()
    }
  }

  function clear() {
    token.value = ''
    profile.value = null
    roles.value = []
    permissions.value = []
    initialized.value = false
    removeAdminToken()
  }

  return {
    token,
    profile,
    roles,
    permissions,
    initialized,
    isLoggedIn,
    loginByPassword,
    fetchProfile,
    logoutCurrent,
    clear,
  }
})
