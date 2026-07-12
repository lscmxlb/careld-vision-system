/**
 * 排班预约相关API
 */
import request from './request'
import type { Schedule, Reserve, BatchScheduleRequest, PageResult } from '@/types'

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
  // 获取预约列表
  getReserveList: (params: {
    storeId?: number
    status?: number
    date?: string
    page?: number
    size?: number
  }): Promise<PageResult<Reserve>> => {
    return request.get('/schedules/reserves', { params })
  },

  // 创建预约
  createReserve: (data: Partial<Reserve>): Promise<Reserve> => {
    return request.post('/schedules/reserves', data)
  },

  // 取消预约
  cancelReserve: (id: number, cancelReason: string): Promise<void> => {
    return request.post(`/schedules/reserves/${id}/cancel`, { cancelReason })
  }
}
