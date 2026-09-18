/**
 * 登录态 Store
 */
import { defineStore } from 'pinia'
import { authApi, userApi } from '@/api'
import { clearAuth, getToken, getUserInfo, setRefreshToken, setToken, setUserInfo } from '@/utils/auth'
import type { User } from '@/types'

interface UserState {
  token: string
  userInfo: User | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: getToken(),
    userInfo: getUserInfo(),
  }),

  getters: {
    isLogin: (state) => !!state.token,
    storeId: (state): number | undefined => state.userInfo?.storeId ?? undefined,
    storeName: (state): string => state.userInfo?.storeName || '',
    /** 医务人员姓名优先，其次登录名 */
    displayName: (state): string => state.userInfo?.realName || state.userInfo?.username || '未登录',
    userType: (state): number | undefined => state.userInfo?.userType,
  },

  actions: {
    async loginByPassword(phone: string, password: string) {
      const res = await authApi.login({ phone, password, loginType: 2 })
      this.applyLogin(res)
      return res
    },

    async loginBySms(phone: string, code: string) {
      const res = await authApi.smsLogin(phone, code)
      this.applyLogin(res)
      return res
    },

    applyLogin(res: { accessToken: string; refreshToken?: string; user: User }) {
      this.token = res.accessToken
      setToken(res.accessToken)
      if (res.refreshToken) setRefreshToken(res.refreshToken)
      this.userInfo = res.user
      setUserInfo(res.user)
    },

    /** 刷新用户信息（我的页面进入时调用） */
    async fetchProfile() {
      const user = await userApi.getCurrentUser()
      this.userInfo = user
      setUserInfo(user)
      return user
    },

    async logout() {
      try {
        await authApi.logout()
      } catch {
        // 登出失败不阻塞本地清理
      }
      this.reset()
    },

    reset() {
      this.token = ''
      this.userInfo = null
      clearAuth()
    },
  },
})
