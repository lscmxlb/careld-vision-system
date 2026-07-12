/**
 * 儿童档案相关API
 */
import request from './request'
import type { Child, ChildQuery, CreateChildRequest, PageResult } from '@/types'

export const childApi = {
  // 获取档案列表
  getChildList: (params: ChildQuery): Promise<PageResult<Child>> => {
    return request.get('/children', { params })
  },

  // 获取档案详情
  getChildDetail: (id: number): Promise<Child> => {
    return request.get(`/children/${id}`)
  },

  // 创建档案
  createChild: (data: CreateChildRequest): Promise<Child> => {
    return request.post('/children', data)
  },

  // 更新档案
  updateChild: (id: number, data: Partial<CreateChildRequest>): Promise<Child> => {
    return request.put(`/children/${id}`, data)
  },

  // 搜索儿童
  searchChildren: (params: { storeId: number; keyword: string }): Promise<Child[]> => {
    return request.get('/children/search', { params })
  }
}
