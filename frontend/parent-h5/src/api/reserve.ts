/**
 * 排班 / 预约相关API
 */
import { get, post } from '@/utils/request'
import type { PageResult, Reserve, ScheduleSlot } from '@/types'

export const reserveApi = {
  /** 预约列表（statuses 逗号分隔多状态，5=已爽约） */
  getReserveList: (params: {
    storeId?: number
    childId?: number
    status?: number
    statuses?: string
    noShowFlag?: boolean
    date?: string
    startDate?: string
    keyword?: string
    page?: number
    size?: number
  }): Promise<PageResult<Reserve>> => get<PageResult<Reserve>>('/schedules/reserves', params),

  getReserveDetail: (id: number): Promise<Reserve> => get<Reserve>(`/schedules/reserves/${id}`),

  /** 创建预约（新链路：slotId + childId，后端校验审核/次数/满额/每日一约） */
  createReserveV2: (data: { childId: number; slotId: string; remark?: string }): Promise<number> =>
    post<number>('/schedules/reserves/v2', data as unknown as Record<string, unknown>),

  /** 家长端取消恒为家长原因（cancelReasonType=1），便于列表区分医院取消/家长取消 */
  cancelReserve: (id: number, cancelReason: string): Promise<void> =>
    post<void>(`/schedules/reserves/${id}/cancel`, { cancelReason, cancelReasonType: 1 }),

  /** 开始养护（录入养护前视力） */
  startCare: (
    id: number,
    data: {
      executorId?: number
      executorName?: string
      startTime?: string
      visionBeforeLeft?: string
      visionBeforeRight?: string
      visionBeforeBoth?: string
    },
  ): Promise<void> => post<void>(`/schedules/reserves/${id}/start`, data as unknown as Record<string, unknown>),

  /** 完成养护（录入养护后视力） */
  completeCare: (
    id: number,
    data: { visionAfterLeft?: string; visionAfterRight?: string; visionAfterBoth?: string },
  ): Promise<void> => post<void>(`/schedules/reserves/${id}/complete`, data as unknown as Record<string, unknown>),

  /** 预约关联养护记录（完成养护弹窗回填） */
  getCareRecord: (id: number): Promise<import('@/types').CareRecord> =>
    get(`/schedules/reserves/${id}/care-record`),

  /** 标记爽约（不退还次数） */
  markNoShow: (id: number): Promise<void> => post<void>(`/schedules/reserves/${id}/no-show`),

  /** 预约调整到新时段 */
  adjustReserve: (id: number, slotId: string): Promise<void> =>
    post<void>(`/schedules/reserves/${id}/adjust`, { slotId }),
}

export const scheduleRuleApi = {
  /** 某日期可约时段 */
  getSlots: (date: string, storeId?: number): Promise<ScheduleSlot[]> =>
    get<ScheduleSlot[]>('/schedule-rules/slots', { date, storeId }),

  /** 日期范围内有排班的日期（去重） */
  getAvailableDates: (startDate: string, endDate: string, storeId?: number): Promise<string[]> =>
    get<string[]>('/schedule-rules/available-dates', { startDate, endDate, storeId }),
}

export const appointmentConfigApi = {
  getConfig: (storeId?: number): Promise<import('@/types').AppointmentConfig> =>
    get('/appointment-config', { storeId }),
}

export const medicalStaffApi = {
  /** 人员列表（建档按所选医院查医生；开始养护执行人下拉） */
  getStaffList: (params: { storeId?: number; keyword?: string; staffRole?: number; status?: number; page?: number; size?: number }): Promise<PageResult<import('@/types').MedicalStaff>> =>
    get('/medical-staff', params),
}

export const departmentApi = {
  /** 科室列表（授权自动计费取第一条启用科室的收费标准） */
  getDepartmentList: (storeId?: number): Promise<import('@/types').Department[]> =>
    get('/departments', { storeId }),
}
