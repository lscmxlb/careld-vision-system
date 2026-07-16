/**
 * 用户相关API
 */
import request from './request'
import type { User } from '@/types'

export const userApi = {
  // 获取当前登录用户信息
  getCurrentUser: (): Promise<User> => {
    return request.get('/users/me')
  }
}
