/**
 * 认证相关 API
 */
import request from './request'
import type { LoginRequest, LoginResponse } from '@/types'

export const authApi = {
  /** 登录 */
  login: (data: LoginRequest): Promise<LoginResponse> => {
    return request.post('/auth/login', data)
  },

  /** 刷新 Token */
  refreshToken: (refreshToken: string): Promise<{ accessToken: string; expiresIn: number }> => {
    return request.post('/auth/refresh', { refreshToken })
  },

  /** 登出 */
  logout: (): Promise<void> => {
    return request.post('/auth/logout')
  },

  /** 发送短信验证码 */
  sendSmsCode: (phone: string): Promise<void> => {
    return request.post('/auth/sms/send', { phone })
  },

  /** 短信验证码登录（未注册自动创建家长账号） */
  smsLogin: (phone: string, code: string): Promise<LoginResponse> => {
    return request.post('/auth/sms/login', { phone, code })
  }
}
