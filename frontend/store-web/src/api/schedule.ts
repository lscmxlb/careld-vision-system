/**
 * 排班预约相关API
 */
import request from './request'
import type { Schedule, Reserve, BatchScheduleRequest, PageResult, ReserveDailyStatistics, CareRecord } from '@/types'

export const scheduleApi = {
  // 获取排班日历
  getScheduleCalendar: (params: {
    storeId: number
    startDate: string
    endDate: string
  }): Promise<Record<string, Schedule[]>> => {
    return request.get('/schedules/calendar', { params })
  },

  // 创建排班
  createSchedule: (data: Partial<Schedule>): Promise<Schedule> => {
    return request.post('/schedules', data)
  },

  // 批量创建排班
  batchCreateSchedule: (data: BatchScheduleRequest): Promise<void> => {
    return request.post('/schedules/batch', data)
  }
}

export const reserveApi = {
  // 获取预约列表（noShowFlag=true 仅爽约；false 排除爽约）
  getReserveList: (params: {
    storeId?: number
    status?: number
    /** 多状态筛选，逗号分隔（1-5，5=已爽约），优先于 status/noShowFlag */
    statuses?: string
    noShowFlag?: boolean
    date?: string
    startDate?: string
    keyword?: string
    page?: number
    size?: number
    /** true 按预约日期倒序（取最近记录），默认升序 */
    orderDesc?: boolean
  }): Promise<PageResult<Reserve>> => {
    return request.get('/schedules/reserves', { params })
  },

  // 创建预约
  createReserve: (data: Partial<Reserve>): Promise<Reserve> => {
    return request.post('/schedules/reserves', data)
  },

  // 取消预约（备注选填；cancelReasonType 1=家长原因 2=医院原因；refundFlag 1=返还次数 0=不返还）
  cancelReserve: (
    id: number,
    payload: { cancelReason?: string; cancelReasonType?: number; refundFlag?: number }
  ): Promise<void> => {
    return request.post(`/schedules/reserves/${id}/cancel`, payload)
  },

  // 创建预约（新链路：slotId + childId，校验审核/次数/满额/每日一约）
  createReserveV2: (data: { childId: number; slotId: string; remark?: string }): Promise<number> => {
    return request.post('/schedules/reserves/v2', data)
  },

  // 开始养护（录入养护前视力）
  startCare: (
    id: number,
    data: { executorId?: number; executorName?: string; startTime?: string; visionBeforeLeft?: string; visionBeforeRight?: string; visionBeforeBoth?: string }
  ): Promise<void> => {
    return request.post(`/schedules/reserves/${id}/start`, data)
  },

  // 查询预约的养护记录（养护记录登记弹窗回填）
  getCareRecord: (id: number): Promise<CareRecord> => {
    return request.get(`/schedules/reserves/${id}/care-record`)
  },

  // 完成养护（录入养护后视力）
  completeCare: (
    id: number,
    data: { visionAfterLeft?: string; visionAfterRight?: string; visionAfterBoth?: string }
  ): Promise<void> => {
    return request.post(`/schedules/reserves/${id}/complete`, data)
  },

  // 标记爽约（不退还预约次数）
  markNoShow: (id: number): Promise<void> => {
    return request.post(`/schedules/reserves/${id}/no-show`)
  },

  // 预约调整（已预约记录更换到新时段）
  adjustReserve: (id: number, slotId: string): Promise<void> => {
    return request.post(`/schedules/reserves/${id}/adjust`, { slotId })
  },

  // 预约统计（每日预约数/完成数/取消数）
  getStatistics: (startDate: string, endDate: string, storeId?: number): Promise<ReserveDailyStatistics[]> => {
    return request.get('/schedules/reserves/statistics', { params: { startDate, endDate, storeId } })
  }
}
