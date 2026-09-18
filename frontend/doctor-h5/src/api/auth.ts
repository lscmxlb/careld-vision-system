/**
 * 认证相关API
 */
import { get, post, put } from '@/utils/request'
import type { LoginRequest, LoginResponse } from '@/types'

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> =>
    post<LoginResponse>('/auth/login', data as unknown as Record<string, unknown>, { noAuth: true }),

  logout: (): Promise<void> => post<void>('/auth/logout', undefined, { silent: true }),

  refreshToken: (refreshToken: string): Promise<{ accessToken: string; expiresIn: number }> =>
    post<{ accessToken: string; expiresIn: number }>('/auth/refresh', { refreshToken }, { noAuth: true, silent: true }),

  sendSms: (phone: string): Promise<void> =>
    post<void>('/auth/sms/send', { phone }, { noAuth: true }),

  smsLogin: (phone: string, code: string): Promise<LoginResponse> =>
    post<LoginResponse>('/auth/sms/login', { phone, code }, { noAuth: true }),
}

export const userApi = {
  getCurrentUser: (): Promise<import('@/types').User> => get('/users/me'),

  /** 修改本人密码（医务人员需验证旧密码） */
  changePassword: (data: { oldPassword: string; newPassword: string }): Promise<void> =>
    post<void>('/users/me/password', data),

  /** 修改本人手机号（即登录账号，成功后需重新登录） */
  changePhone: (data: { phone: string }): Promise<void> => put<void>('/users/me/phone', data),
}
