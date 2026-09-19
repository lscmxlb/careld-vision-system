/**
 * 登录态 Store（家长端：纯短信验证码登录）
 */
import { defineStore } from 'pinia'
import { authApi, childApi, userApi } from '@/api'
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
    userId: (state): number | undefined => state.userInfo?.id,
    /** 仅返回合法 11 位手机号，避免旧缓存中缺 phone 时把 username 当作手机号 */
    phone: (state): string => {
      const phone = state.userInfo?.phone || ''
      return /^\d{11}$/.test(phone) ? phone : ''
    },
    /** 手机号脱敏展示 */
    phoneMask: (state): string => {
      const phone = state.userInfo?.phone || ''
      return /^\d{11}$/.test(phone) ? `${phone.slice(0, 3)}****${phone.slice(7)}` : '—'
    },
    displayName: (state): string => state.userInfo?.realName || state.userInfo?.phone || '家长',
  },

  actions: {
    sendSms(phone: string) {
      return authApi.sendSms(phone)
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

    /** 刷新用户信息（个人中心进入时调用） */
    async fetchProfile() {
      const user = await userApi.getCurrentUser()
      this.userInfo = user
      setUserInfo(user)
      return user
    },

    /**
     * 登录后按手机号认领未绑定的儿童档案（幂等）；
     * 账号姓名为空或默认「家长+尾4位」时，用档案家长姓名回填账号姓名
     */
    async claimMyChildren() {
      if (!this.token) return
      try {
        const res = await childApi.claimByPhone()
        const parentName = res?.parentName
        const realName = this.userInfo?.realName || ''
        if (parentName && (!realName || /^家长\d{4}$/.test(realName))) {
          await userApi.updateMyProfile({ realName: parentName })
          await this.fetchProfile()
        }
      } catch {
        // 认领失败不阻塞登录与页面加载
      }
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
