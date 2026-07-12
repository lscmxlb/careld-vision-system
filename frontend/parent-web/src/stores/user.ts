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

  const login = async (phone: string, password: string) => {
    const loginData: LoginRequest = {
      phone,
      password,
      loginType: 2 // 2=手机号密码登录
    }
    const res = await authApi.login(loginData)
    setToken(res.accessToken)
    userInfo.value = res.user
    permissions.value = res.user.permissions || []
    return res
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch {
      // 即使接口失败也清除本地状态
    } finally {
      clearToken()
    }
  }

  const initAuth = async () => {
    if (token.value && !userInfo.value) {
      // 可以在这里添加获取用户信息的逻辑
      // 目前 token 存在即认为已登录
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
    initAuth,
    setToken,
    clearToken
  }
})
