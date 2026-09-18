/**
 * 儿童档案相关API
 */
import request from './request'
import type { Child, ChildQuery, CreateChildRequest, ChildServiceRecord } from '@/types'

export const childApi = {
  // 获取档案列表（后端返回全量数组，前端本地分页展示）
  getChildList: (params: ChildQuery): Promise<Child[]> => {
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

  // 档案审核（通过时需指定主治医生）
  auditChild: (id: number, data: {
    auditStatus: number
    auditRemark?: string
    doctorId?: number
    doctorName?: string
  }): Promise<void> => {
    return request.post(`/children/${id}/audit`, data)
  },

  // 启用/禁用档案（status: 1=启用, 0=禁用）
  setChildStatus: (id: number, data: { status: number }): Promise<void> => {
    return request.put(`/children/${id}/status`, data)
  },

  // 搜索儿童（TV/模糊使用）
  searchChildren: (params: { storeId: number; keyword: string }): Promise<Child[]> => {
    return request.get('/children/search', { params })
  },

  // 按监护人手机号精确核验本店儿童（门店新建预约选人，后端解密比对）
  searchChildrenByPhone: (params: { phone: string; storeId?: number }): Promise<Child[]> => {
    return request.get('/children/by-phone', { params })
  },

  // 预约选人选项（已审核且启用，明文姓名/手机号+剩余次数）
  pickOptions: (params: { storeId?: number; keyword?: string }): Promise<Child[]> => {
    return request.get('/children/pick-options', { params })
  },

  // 剩余可约次数
  getRemainingCount: (id: number): Promise<number> => {
    return request.get(`/children/${id}/remaining-count`)
  },

  // 添加服务记录（授予可约次数）
  grantServiceRecord: (id: number, data: {
    changeCount: number
    paymentAmount: number
    paymentMethod: string
    doctorId: number
    doctorName?: string
    remark?: string
  }): Promise<number> => {
    return request.post(`/children/${id}/service-records`, data)
  },

  // 服务次数变更流水
  getServiceRecords: (id: number): Promise<ChildServiceRecord[]> => {
    return request.get(`/children/${id}/service-records`)
  }
}
