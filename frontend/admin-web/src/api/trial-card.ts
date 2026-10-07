/**
 * 项目体验卡API（后台医院管理发行与查询）
 */
import request from './request'
import type { PageResponse } from '@/types'

export interface TrialCard {
  id: number
  /** 编号（11位：2位年份+4位区号+5位顺序号） */
  cardNo: string
  /** 验证码（8位数字，末2位为校验位） */
  verifyCode: string
  /** 发行标题（本批体验卡的关键信息） */
  title?: string
  centerId: number
  agentId?: number
  storeId?: number
  areaCode: string
  seqNo: number
  /** 0=未兑换 2=已绑定（已兑换未核销） 1=已使用 3=已禁用 */
  status: number
  /** 绑定时间（家长兑换时写入） */
  boundAt?: string
  usedAt?: string
  usedStoreId?: number
  usedChildId?: number
  usedParentUserId?: number
  remark?: string
  centerName?: string
  agentName?: string
  storeName?: string
  usedStoreName?: string
  usedStoreType?: number
  /** 发行人姓名（后台列表回填） */
  creatorName?: string
  /** 使用的儿童姓名（明文，仅后台下发） */
  childName?: string
  parentName?: string
  parentPhone?: string
  createdAt?: string
}

export interface TrialCardQuery {
  status?: number
  centerId?: number
  agentId?: number
  storeId?: number
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

/** 统计口径：同列表筛选（中心/代理商/医院/编号/日期），不含状态筛选 */
export type TrialCardStatsQuery = Omit<TrialCardQuery, 'status' | 'page' | 'size'>

export interface TrialCardStats {
  /** 已发行总数 */
  total: number
  /** 剩余可用（未兑换） */
  unused: number
  /** 已绑定 */
  bound: number
  /** 已使用 */
  used: number
  /** 已禁用 */
  disabled: number
}

export interface TrialCardIssueRequest {
  centerId: number
  agentId?: number
  storeId?: number
  /** 城市电话区号（3 位区号前补 0，如 755 → 0755） */
  areaCode: string
  /** 发行标题（本批体验卡的关键信息，必填） */
  title: string
  /** 发行数量（1~10000，同批连续编号） */
  count: number
  remark?: string
}

export interface TrialCardStatusResult {
  id: number
  cardNo: string
  /** 操作后的状态：禁用=3，启用=0 */
  status: number
}

export interface TrialCardDisableRangeRequest {
  /** 起始编号（11 位数字） */
  startCardNo: string
  /** 结束编号（11 位数字，含） */
  endCardNo: string
}

export interface TrialCardDisableRangeResult {
  startCardNo: string
  endCardNo: string
  /** 区间内有效卡总数 */
  total: number
  /** 实际禁用张数（仅未兑换） */
  disabled: number
  /** 跳过张数（已绑定/已使用/已禁用） */
  skipped: number
}

export const trialCardApi = {
  getList: (params: TrialCardQuery): Promise<PageResponse<TrialCard>> => {
    return request.get('/trial-cards', { params })
  },

  /** 状态张数统计（同列表筛选口径，不含状态筛选） */
  getStats: (params: TrialCardStatsQuery): Promise<TrialCardStats> => {
    return request.get('/trial-cards/stats', { params })
  },

  issue: (data: TrialCardIssueRequest): Promise<TrialCard[]> => {
    return request.post('/trial-cards', data)
  },

  /** 禁用体验卡（仅未兑换，0→3） */
  disable: (id: number): Promise<TrialCardStatusResult> => {
    return request.put(`/trial-cards/${id}/disable`)
  },

  /** 启用体验卡（仅已禁用，3→0） */
  enable: (id: number): Promise<TrialCardStatusResult> => {
    return request.put(`/trial-cards/${id}/enable`)
  },

  /** 按编号区间批量禁用体验卡（仅未兑换，返回 total/disabled/skipped） */
  disableRange: (data: TrialCardDisableRangeRequest): Promise<TrialCardDisableRangeResult> => {
    return request.post('/trial-cards/disable-range', data)
  },
}
