/**
 * 用户状态管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, LoginRequest } from '@/types'
import { authApi } from '@/api'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<User | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const storeId = computed(() => userInfo.value?.storeId)

  // Actions
  const setToken = (newToken: string, newRefreshToken?: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('refreshToken', newRefreshToken)
    }
  }

  const clearToken = () => {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  const login = async (loginData: LoginRequest) => {
    const res = await authApi.login(loginData)
    setToken(res.accessToken, res.refreshToken)
    userInfo.value = res.user
    return res
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch {
      // 即使API调用失败也清除本地状态
    } finally {
      clearToken()
    }
  }

  const handleRefreshToken = async () => {
    if (!refreshToken.value) {
      clearToken()
      throw new Error('无刷新令牌')
    }
    try {
      const res = await authApi.refreshToken(refreshToken.value)
      setToken(res.accessToken)
      return res.accessToken
    } catch {
      clearToken()
      throw new Error('刷新令牌失败')
    }
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLoggedIn,
    storeId,
    login,
    logout,
    handleRefreshToken,
    setToken,
    clearToken
  }
})
