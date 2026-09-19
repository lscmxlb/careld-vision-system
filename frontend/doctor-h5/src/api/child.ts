/**
 * 儿童档案相关API
 */
import { del, get, post, put } from '@/utils/request'
import type { Child, ChildQuery, CreateChildRequest, ChildServiceRecord } from '@/types'

function toQuery(params: ChildQuery): Record<string, unknown> {
  return { ...params } as Record<string, unknown>
}

export const childApi = {
  /** 档案列表（后端返回全量数组，前端本地分页） */
  getChildList: (params: ChildQuery): Promise<Child[]> =>
    get<Child[]>('/children', toQuery(params)),

  getChildDetail: (id: number): Promise<Child> => get<Child>(`/children/${id}`),

  createChild: (data: CreateChildRequest): Promise<number> =>
    post<number>('/children', data as unknown as Record<string, unknown>),

  updateChild: (id: number, data: Partial<CreateChildRequest>): Promise<void> =>
    put<void>(`/children/${id}`, data as unknown as Record<string, unknown>),

  /** 审核（通过时需指定主治医生） */
  auditChild: (
    id: number,
    data: { auditStatus: number; auditRemark?: string; doctorId?: number; doctorName?: string },
  ): Promise<void> => post<void>(`/children/${id}/audit`, data as unknown as Record<string, unknown>),

  /** 启用/禁用档案 */
  setChildStatus: (id: number, status: number): Promise<void> =>
    put<void>(`/children/${id}/status`, { status }),

  /** 预约选人选项（已审核且启用，明文姓名/手机号+剩余次数） */
  pickOptions: (params: { storeId?: number; keyword?: string }): Promise<Child[]> =>
    get<Child[]>('/children/pick-options', params),

  /** 按监护人手机号精确核验本店儿童 */
  searchChildrenByPhone: (params: { phone: string; storeId?: number }): Promise<Child[]> =>
    get<Child[]>('/children/by-phone', params),

  getRemainingCount: (id: number): Promise<number> => get<number>(`/children/${id}/remaining-count`),

  /** 授权可约次数（服务记录） */
  grantServiceRecord: (
    id: number,
    data: {
      changeCount: number
      paymentAmount?: number
      paymentMethod?: string
      doctorId?: number
      doctorName?: string
      remark?: string
    },
  ): Promise<number> => post<number>(`/children/${id}/service-records`, data as unknown as Record<string, unknown>),

  getServiceRecords: (id: number): Promise<ChildServiceRecord[]> =>
    get<ChildServiceRecord[]>(`/children/${id}/service-records`),

  deleteChild: (id: number): Promise<void> => del<void>(`/children/${id}`),

  /** 恢复家长删除（隐藏）的档案，恢复后家长端重新可见 */
  restoreChild: (id: number): Promise<void> => put<void>(`/children/${id}/restore`, {}),
}
