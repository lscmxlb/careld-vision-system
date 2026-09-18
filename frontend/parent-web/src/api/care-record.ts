/**
 * 养护记录相关 API（家长端瀑布流展示）
 */
import request from './request'
import type { CareRecord } from '@/types'

export const careRecordApi = {
  /** 按儿童查询养护记录（时间倒序） */
  getRecordsByChild: (childId: number): Promise<CareRecord[]> => {
    return request.get('/care-records', { params: { childId } })
  },

  /** 养护记录详情 */
  getRecordDetail: (id: number): Promise<CareRecord> => {
    return request.get(`/care-records/${id}`)
  }
}
