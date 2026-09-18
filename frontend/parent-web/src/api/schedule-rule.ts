/**
 * 排班规则（可约日期/时段）相关 API
 */
import request from './request'
import type { ScheduleSlot } from '@/types'

export const scheduleRuleApi = {
  /** 查询日期范围内的可约日期（去重） */
  getAvailableDates: (storeId: number, startDate: string, endDate: string): Promise<string[]> => {
    return request.get('/schedule-rules/available-dates', { params: { storeId, startDate, endDate } })
  },

  /** 查询某日期的可约时段 */
  getSlots: (date: string, storeId?: number): Promise<ScheduleSlot[]> => {
    return request.get('/schedule-rules/slots', { params: { storeId, date } })
  }
}
