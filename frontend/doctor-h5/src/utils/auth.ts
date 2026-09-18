/**
 * 本地登录态存储（token / refreshToken / 用户信息）
 * 独立成模块，避免 request 与 store 循环依赖
 */
import type { User } from '@/types'

const TOKEN_KEY = 'careld_doctor_token'
const REFRESH_TOKEN_KEY = 'careld_doctor_refresh_token'
const USER_KEY = 'careld_doctor_user'

export function getToken(): string {
  return (uni.getStorageSync(TOKEN_KEY) as string) || ''
}

export function setToken(token: string) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function getRefreshToken(): string {
  return (uni.getStorageSync(REFRESH_TOKEN_KEY) as string) || ''
}

export function setRefreshToken(token: string) {
  uni.setStorageSync(REFRESH_TOKEN_KEY, token)
}

export function getUserInfo(): User | null {
  const raw = uni.getStorageSync(USER_KEY)
  if (!raw) return null
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw) as User
    } catch {
      return null
    }
  }
  return raw as User
}

export function setUserInfo(user: User) {
  uni.setStorageSync(USER_KEY, JSON.stringify(user))
}

export function clearAuth() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(REFRESH_TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}
