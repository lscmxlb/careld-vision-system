/**
 * 预约相关 API
 */
import request from './request'
import type { Reserve, CreateReserveRequest } from '@/types'

export const reserveApi = {
  /** 获取我的预约列表 */
  getMyReservations: (storeId?: number): Promise<Reserve[]> => {
    const params = storeId ? { storeId } : {}
    return request.get('/schedules/reserves', { params })
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
