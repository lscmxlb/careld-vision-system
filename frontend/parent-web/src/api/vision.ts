/**
 * 视力检测相关 API
 */
import request from './request'
import type { VisionRecord, VisionRecordDetail, VisionCompare } from '@/types'

export const visionApi = {
  /** 获取视力检测记录列表 */
  getVisionRecords: (childId: number): Promise<VisionRecord[]> => {
    return request.get('/vision/records', { params: { childId } })
  },

  /** 获取视力检测记录详情 */
  getVisionRecordDetail: (id: number): Promise<VisionRecordDetail> => {
    return request.get(`/vision/records/${id}`)
  },

  /** 对比视力（养护前后） */
  compareVision: (childId: number, reserveId: number): Promise<VisionCompare> => {
    return request.get('/vision/compare', { params: { childId, reserveId } })
  }
}
