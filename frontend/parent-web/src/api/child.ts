/**
 * 儿童档案相关 API
 */
import request from './request'
import type { Child, CreateChildRequest } from '@/types'

export const childApi = {
  /** 获取当前家长的孩子列表 */
  getMyChildren: (parentUserId?: number): Promise<Child[]> => {
    const params = parentUserId ? { parentUserId } : {}
    return request.get('/children', { params })
  },

  /** 获取档案详情 */
  getChildDetail: (id: number): Promise<Child> => {
    return request.get(`/children/${id}`)
  },

  /** 添加孩子 */
  addChild: (data: CreateChildRequest): Promise<Child> => {
    return request.post('/children', data)
  },

  /** 更新孩子信息 */
  updateChild: (id: number, data: Partial<CreateChildRequest>): Promise<Child> => {
    return request.put(`/children/${id}`, data)
  }
}
