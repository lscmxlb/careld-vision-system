/**
 * 儿童档案相关API
 */
import request from './request'
import type { Child, ChildQuery, CreateChildRequest, AuditRequest, PageResponse } from '@/types'

export const childApi = {
  // 获取档案列表
  getChildList: (params: ChildQuery): Promise<PageResponse<Child>> => {
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

  // 审核档案
  auditChild: (id: number, data: AuditRequest): Promise<void> => {
    return request.post(`/children/${id}/audit`, data)
  },

  // 删除档案
  deleteChild: (id: number): Promise<void> => {
    return request.delete(`/children/${id}`)
  },

  // 获取待审核档案数量
  getPendingAuditCount: (): Promise<number> => {
    return request.get('/children/pending-count')
  }
}
