/**
 * 门店相关API
 */
import { get } from '@/utils/request'
import type { Store } from '@/types'

export const storeApi = {
  /** 全量门店（家长添加孩子时选择所属医院） */
  getAllStores: (): Promise<Store[]> => get<Store[]>('/stores/all'),
}
