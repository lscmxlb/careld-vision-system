/**
 * 门店相关 API
 */
import request from './request'
import type { Store } from '@/types'

export const storeApi = {
  /** 获取门店列表（家长端选择门店用） */
  getStoreList: (): Promise<Store[]> => {
    return request.get('/stores')
  }
}
