/**
 * 预约相关 API
 */
import request from './request'
import type { Reserve, CreateReserveRequest } from '@/types'

export const reserveApi = {
  /** 获取我的预约列表 */
  getMyReservations: async (storeId?: number): Promise<Reserve[]> => {
    const params = storeId ? { storeId } : {}
    const res = await request.get('/schedules/reserves', { params })
    return res?.list ?? []
  },

  /** 创建预约 */
  createReservation: (data: CreateReserveRequest): Promise<Reserve> => {
    return request.post('/schedules/reserves', data)
  },

  /** 取消预约 */
  cancelReservation: (id: number, reason: string): Promise<void> => {
    return request.post(`/schedules/reserves/${id}/cancel`, { cancelReason: reason })
  }
}
