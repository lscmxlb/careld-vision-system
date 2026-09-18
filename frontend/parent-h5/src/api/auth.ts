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

  /** 修改本人姓名（建档时同步家长姓名） */
  updateMyProfile: (data: { realName: string }): Promise<void> =>
    put<void>('/users/me', data as unknown as Record<string, unknown>),
}
