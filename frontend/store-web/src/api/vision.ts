/**
 * 视力检测相关API
 */
import request from './request'
import type { VisionRecord, VisionCompare, PageResult } from '@/types'

export const visionApi = {
  // 获取视力记录列表（同一次检测的左右眼已合并为一行）
  getVisionRecords: (params: {
    childId?: number
    childName?: string
    storeId?: number
    startDate?: string
    endDate?: string
    page?: number
    size?: number
  }): Promise<PageResult<VisionRecord>> => {
    return request.get('/vision/records/grouped', { params })
  },

  // 获取视力记录详情
  getVisionRecordDetail: (id: number): Promise<VisionRecord> => {
    return request.get(`/vision/records/${id}`)
  },

  // 创建视力记录
  createVisionRecord: (data: Partial<VisionRecord>): Promise<VisionRecord> => {
    return request.post('/vision/records', data)
  },

  // 视力对比
  compareVision: (params: { childId: number; reserveId?: number }): Promise<VisionCompare> => {
    return request.get('/vision/compare', { params })
  }
}
