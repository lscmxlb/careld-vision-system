/**
 * 数据统计相关API
 */
import request from './request'
import type { DashboardStats, WeeklyTrend, VisionStatistics } from '@/types'

export const statisticsApi = {
  // 获取看板统计数据
  getDashboardStats: (storeId: number): Promise<DashboardStats> => {
    return request.get('/statistics/dashboard', { params: { storeId } })
  },

  // 获取每周/每月预约趋势
  getWeeklyTrend: (params: {
    storeId: number
    period: 'week' | 'month'
  }): Promise<WeeklyTrend> => {
    return request.get('/statistics/weekly-trend', { params })
  },

  // 获取视力检测统计
  getVisionStatistics: (params: {
    storeId: number
    startDate: string
    endDate: string
  }): Promise<VisionStatistics> => {
    return request.get('/statistics/vision-stats', { params })
  }
}
