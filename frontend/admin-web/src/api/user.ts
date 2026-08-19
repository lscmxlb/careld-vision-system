/**
 * 用户相关API
 */
import request from './request'
import type { User, PageResponse } from '@/types'

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

  // 创建用户
  createUser: (data: Partial<User>): Promise<User> => {
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
  }
}
