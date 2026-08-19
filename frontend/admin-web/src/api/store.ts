/**
 * 医院相关API
 */
import request from './request'
import type { Store, StoreQuery, PageResponse } from '@/types'

export const storeApi = {
  // 获取医院列表
  getStoreList: (params: StoreQuery): Promise<PageResponse<Store>> => {
    return request.get('/stores', { params })
  },

  // 获取医院详情
  getStoreDetail: (id: number): Promise<Store> => {
    return request.get(`/stores/${id}`)
  },

  // 创建医院
  createStore: (data: Partial<Store>): Promise<Store> => {
    return request.post('/stores', data)
  },

  // 更新医院
  updateStore: (id: number, data: Partial<Store>): Promise<Store> => {
    return request.put(`/stores/${id}`, data)
  },

  // 更新医院状态
  updateStoreStatus: (id: number, status: number): Promise<void> => {
    return request.patch(`/stores/${id}/status`, { status })
  },

  // 删除医院
  deleteStore: (id: number): Promise<void> => {
    return request.delete(`/stores/${id}`)
  },

  // 获取所有医院（下拉选择）
  getAllStores: (): Promise<Store[]> => {
    return request.get('/stores/all')
  }
}
