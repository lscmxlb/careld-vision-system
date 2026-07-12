/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest } from '@/types'
import { authApi, userApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)
  const permissions = ref<string[]>([])

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const hasPermission = computed(() => (perm: string) => {
    return permissions.value.includes(perm) || permissions.value.includes('*')
  })

  // Actions
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const clearToken = () => {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
  }

  const login = async (loginData: LoginRequest) => {
    const res = await authApi.login(loginData)
    setToken(res.accessToken)
    userInfo.value = res.user
    permissions.value = res.user.permissions
    return res
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } finally {
      clearToken()
    }
  }

  const fetchUserInfo = async () => {
    const res = await userApi.getCurrentUser()
    userInfo.value = res
    permissions.value = res.permissions
    return res
  }

  const initAuth = async () => {
    if (token.value && !userInfo.value) {
      try {
        await fetchUserInfo()
      } catch {
        clearToken()
      }
    }
  }

  return {
    token,
    userInfo,
    permissions,
    isLoggedIn,
    hasPermission,
    login,
    logout,
    fetchUserInfo,
    initAuth,
    setToken,
    clearToken
  }
})
