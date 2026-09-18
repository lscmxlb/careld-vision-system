/**
 * 养护记录相关API
 */
import request from './request'
import type { CareRecord, PageResult } from '@/types'

export const careRecordApi = {
  // 养护记录列表（按儿童查询，时间倒序）
  getRecordsByChild: (childId: number, storeId?: number): Promise<CareRecord[]> => {
    return request.get('/care-records', { params: { childId, storeId } })
  },

  // 养护记录分页（按医院查询，支持儿童姓名/家长姓名/手机号/养护次数筛选）
  getRecordPage: (params: {
    storeId?: number
    childId?: number
    childName?: string
    parentName?: string
    phone?: string
    /** 筛选累计已完成养护次数大于该值的儿童 */
    minCareCount?: number
    page?: number
    size?: number
  }): Promise<PageResult<CareRecord>> => {
    return request.get('/care-records/page', { params })
  },

  // 养护记录详情
  getRecordDetail: (id: number): Promise<CareRecord> => {
    return request.get(`/care-records/${id}`)
  }
}
