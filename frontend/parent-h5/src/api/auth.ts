/**
 * 认证相关API
 */
import { get, post, put } from '@/utils/request'
import type { LoginRequest, LoginResponse } from '@/types'

export interface ParentRegisterPayload {
  phone: string
  code: string
  realName: string
  password: string
}

export interface ParentResetPasswordPayload {
  phone: string
  code: string
  newPassword: string
}

export interface ParentChangePhonePayload {
  phone: string
  code: string
}

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> =>
    post<LoginResponse>('/auth/login', data as unknown as Record<string, unknown>, { noAuth: true }),

  logout: (): Promise<void> => post<void>('/auth/logout', undefined, { silent: true }),

  refreshToken: (refreshToken: string): Promise<{ accessToken: string; expiresIn: number }> =>
    post<{ accessToken: string; expiresIn: number }>('/auth/refresh', { refreshToken }, { noAuth: true, silent: true }),

  sendSms: (phone: string): Promise<void> =>
    post<void>('/auth/sms/send', { phone }, { noAuth: true }),

  /** 家长端手机号+密码登录 */
  parentLogin: (phone: string, password: string): Promise<LoginResponse> =>
    post<LoginResponse>('/auth/parent/login', { phone, password }, { noAuth: true }),

  /** 家长端注册（注册成功即签发登录态） */
  parentRegister: (data: ParentRegisterPayload): Promise<LoginResponse> =>
    post<LoginResponse>('/auth/parent/register', data as unknown as Record<string, unknown>, { noAuth: true }),

  /** 忘记密码：手机号+短信验证码重新设置登录密码 */
  parentResetPassword: (data: ParentResetPasswordPayload): Promise<void> =>
    post<void>('/auth/parent/reset-password', data as unknown as Record<string, unknown>, { noAuth: true }),

  /** 修改手机号：新手机号+短信验证码（旧手机号不验证） */
  parentChangePhone: (data: ParentChangePhonePayload): Promise<void> =>
    post<void>('/auth/parent/change-phone', data as unknown as Record<string, unknown>),
}

export const userApi = {
  getCurrentUser: (): Promise<import('@/types').User> => get('/users/me'),

  /** 修改本人姓名（建档时同步家长姓名） */
  updateMyProfile: (data: { realName: string }): Promise<void> =>
    put<void>('/users/me', data as unknown as Record<string, unknown>),

  /** 修改本人登录密码（需原密码） */
  changeMyPassword: (data: { oldPassword: string; newPassword: string }): Promise<void> =>
    post<void>('/users/me/password', data as unknown as Record<string, unknown>),
}
