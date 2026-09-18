/**
 * 养护记录相关API
 */
import { get } from '@/utils/request'
import type { CareRecord, PageResult } from '@/types'

export const careRecordApi = {
  /** 分页列表（姓名/家长/手机号筛选） */
  getRecordPage: (params: {
    storeId?: number
    childId?: number
    childName?: string
    parentName?: string
    phone?: string
    minCareCount?: number
    page?: number
    size?: number
  }): Promise<PageResult<CareRecord>> => get<PageResult<CareRecord>>('/care-records/page', params),

  /** 某儿童的养护记录（时间倒序，详情页复用） */
  getRecordsByChild: (childId: number, storeId?: number): Promise<CareRecord[]> =>
    get<CareRecord[]>('/care-records', { childId, storeId }),

  getRecordDetail: (id: number): Promise<CareRecord> => get<CareRecord>(`/care-records/${id}`),
}
