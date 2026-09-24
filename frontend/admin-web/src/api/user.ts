/**
 * 用户相关API
 */
import request from './request'
import type { User, PageResponse } from '@/types'

export interface PhoneLookupIdentity {
  source: 'sys_user' | 'medical_staff'
  id: number
  username?: string
  realName?: string
  phone?: string
  userType?: number
  userTypeName?: string
  staffRole?: number
  staffRoleName?: string
  roleNames?: string[]
  centerName?: string
  agentName?: string
  storeName?: string
  status?: number
  createdAt?: string
  lastLoginTime?: string
  childCount?: number
}

export interface PhoneLookupResult {
  phone: string
  found: boolean
  identities: PhoneLookupIdentity[]
}

export const userApi = {
  // 获取当前用户信息
  getCurrentUser: (): Promise<User> => {
    return request.get('/users/me')
  },

  // 获取用户列表
  getUserList: (params: {
    page?: number
    size?: number
    userType?: number
    storeId?: number
    centerId?: number
    agentId?: number
    status?: number
    keyword?: string
  }): Promise<PageResponse<User>> => {
    return request.get('/users', { params })
  },

  // 创建用户（后端返回新用户ID）
  createUser: (data: Partial<User>): Promise<number> => {
    return request.post('/users', data)
  },

  // 更新用户
  updateUser: (id: number, data: Partial<User>): Promise<User> => {
    return request.put(`/users/${id}`, data)
  },

  // 删除用户
  deleteUser: (id: number): Promise<void> => {
    return request.delete(`/users/${id}`)
  },

  // 重置密码
  resetPassword: (id: number, newPassword: string): Promise<void> => {
    return request.post(`/users/${id}/reset-password`, { newPassword })
  },

  // 启用/禁用用户
  updateUserStatus: (id: number, status: number): Promise<void> => {
    return request.patch(`/users/${id}/status`, { status })
  },

  // 修改本人密码
  changeMyPassword: (oldPassword: string, newPassword: string): Promise<void> => {
    return request.post('/users/me/password', { oldPassword, newPassword })
  },

  // 手机号码查询（该号码是否已注册及其身份/角色）
  phoneLookup: (phone: string): Promise<PhoneLookupResult> => {
    return request.get('/users/phone-lookup', { params: { phone } })
  }
}
