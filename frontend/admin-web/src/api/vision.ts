/**
 * 视力记录相关API
 */
import request from './request'
import type { VisionRecord, VisionRecordQuery, VisionCompare, PageResponse } from '@/types'

export const visionApi = {
  // 获取视力记录列表
  getVisionRecordList: (params: VisionRecordQuery): Promise<PageResponse<VisionRecord>> => {
    return request.get('/vision/records', { params })
  },

  // 获取视力记录详情
  getVisionRecordDetail: (id: number): Promise<VisionRecord> => {
    return request.get(`/vision/records/${id}`)
  },

  // 创建视力记录
  createVisionRecord: (data: Partial<VisionRecord>): Promise<VisionRecord> => {
    return request.post('/vision/records', data)
  },

  // 视力数据对比（检测前后）
  compareVision: (params: { childId: number; reserveId: number }): Promise<VisionCompare> => {
    return request.get('/vision/compare', { params })
  }
}
