/**
 * 认证相关API
 */
import request from './request'
import type { LoginRequest, LoginResponse } from '@/types'

export const authApi = {
  // 登录
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return request.post('/auth/login', data)
  },

  // 登出
  logout: (): Promise<void> => {
    return request.post('/auth/logout')
  },

  // 刷新Token
  refreshToken: (refreshToken: string): Promise<{ accessToken: string; expiresIn: number }> => {
    return request.post('/auth/refresh', { refreshToken })
  },

  // 获取验证码
  getCaptcha: (): Promise<{ captchaKey: string; captchaImage: string }> => {
    return request.get('/auth/captcha')
  }
}
