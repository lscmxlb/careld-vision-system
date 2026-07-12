/**
 * 门店相关API
 */
import request from './request'
import type { Store } from '@/types'

export const storeApi = {
  // 获取门店列表
  getStoreList: (params?: {
    page?: number
    size?: number
    keyword?: string
  }): Promise<{ list: Store[]; total: number }> => {
    return request.get('/stores', { params })
  },

  // 获取门店详情
  getStoreDetail: (id: number): Promise<Store> => {
    return request.get(`/stores/${id}`)
  },

  // 获取当前用户所属门店
  getCurrentStore: (): Promise<Store> => {
    return request.get('/stores/current')
  }
}
