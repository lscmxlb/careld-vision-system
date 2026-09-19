/**
 * 数据统计相关API
 */
import request from './request'
import type { StoreTraffic, VisionImprovement, NationalSummary, StoreOverview } from '@/types'

export const statisticsApi = {
  // 医院客流统计
  getStoreTraffic: (params: {
    storeId: number
    startDate: string
    endDate: string
    groupBy: 'day' | 'week' | 'month'
  }): Promise<StoreTraffic> => {
    return request.get('/statistics/store-traffic', { params })
  },

  // 视力改善统计
  getVisionImprovement: (params: {
    storeId: number
    startDate: string
    endDate: string
  }): Promise<VisionImprovement> => {
    return request.get('/statistics/vision-improvement', { params })
  },

  // 全国医院数据汇总
  getNationalSummary: (params: {
    startDate: string
    endDate: string
  }): Promise<NationalSummary> => {
    return request.get('/statistics/national-summary', { params })
  },

  // 单医院数据统计（基本信息 + 动态统计）
  getStoreOverview: (storeId: number): Promise<StoreOverview> => {
    return request.get('/statistics/store-overview', { params: { storeId } })
  },

  // 数据导出
  exportData: (data: {
    exportType: string
    storeId?: number
    startDate: string
    endDate: string
    format: 'excel' | 'csv'
  }): Promise<{ downloadUrl: string }> => {
    return request.post('/statistics/export', data)
  }
}
