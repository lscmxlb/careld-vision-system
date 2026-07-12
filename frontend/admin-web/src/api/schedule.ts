/**
 * 排班预约相关API
 */
import request from './request'
import type { Schedule, Reserve, ReserveQuery, BatchScheduleRequest, PageResponse } from '@/types'

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
  },

  // 更新排班
  updateSchedule: (id: number, data: Partial<Schedule>): Promise<Schedule> => {
    return request.put(`/schedules/${id}`, data)
  },

  // 删除排班
  deleteSchedule: (id: number): Promise<void> => {
    return request.delete(`/schedules/${id}`)
  }
}

export const reserveApi = {
  // 获取预约列表
  getReserveList: (params: ReserveQuery): Promise<PageResponse<Reserve>> => {
    return request.get('/schedules/reserves', { params })
  },

  // 获取预约详情
  getReserveDetail: (id: number): Promise<Reserve> => {
    return request.get(`/schedules/reserves/${id}`)
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
