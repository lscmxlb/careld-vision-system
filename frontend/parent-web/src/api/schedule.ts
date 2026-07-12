/**
 * 排班相关 API
 */
import request from './request'
import type { Schedule } from '@/types'

export const scheduleApi = {
  /** 获取可用排班日历 — 后端返回扁平列表，此处按日期分组后返回 */
  getAvailableSchedules: async (
    storeId: number,
    startDate: string,
    endDate: string
  ): Promise<Record<string, Schedule[]>> => {
    const list: Schedule[] = await request.get('/schedules/calendar', {
      params: { storeId, startDate, endDate }
    })
    // 将扁平列表按 scheduleDate 分组，并补充 availableCount
    const grouped: Record<string, Schedule[]> = {}
    if (Array.isArray(list)) {
      for (const item of list) {
        const date = item.scheduleDate
        if (!date) continue
        const availableCount = (item.maxCapacity ?? 0) - (item.reservedCount ?? 0)
        const enriched = { ...item, availableCount }
        if (!grouped[date]) {
          grouped[date] = []
        }
        grouped[date].push(enriched)
      }
    }
    return grouped
  }
}
