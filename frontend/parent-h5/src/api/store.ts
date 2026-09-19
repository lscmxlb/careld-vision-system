/**
 * 门店相关API
 */
import { get } from '@/utils/request'
import type { Store } from '@/types'

export const storeApi = {
  /** 全量门店（家长添加儿童时选择医院） */
  getAllStores: (): Promise<Store[]> => get<Store[]>('/stores/all'),
}
