/**
 * 数据统计相关API
 */
import { get } from '@/utils/request'
import type { WorkbenchStats } from '@/types'

export const statisticsApi = {
  /** 工作台统计：儿童档案数量 / 当前已预约数量 / 已完成养护次数（后端按登录门店过滤） */
  getWorkbenchStats: (storeId?: number): Promise<WorkbenchStats> =>
    get<WorkbenchStats>('/statistics/workbench', { storeId }),
}
