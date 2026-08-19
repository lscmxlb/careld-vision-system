/**
 * 医院相关 API
 */
import request from './request'
import type { Store } from '@/types'

export const storeApi = {
  /** 获取医院列表（家长端选择医院用，全量） */
  getStoreList: (): Promise<Store[]> => {
    return request.get('/stores/all')
  }
}
