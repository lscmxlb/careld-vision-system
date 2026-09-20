/**
 * 排班 / 预约相关API
 */
import { get, post } from '@/utils/request'
import type { Department, PageResult, Reserve, ScheduleSlot } from '@/types'

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

  cancelReserve: (
    id: number,
    payload: { cancelReason?: string; cancelReasonType?: number; refundFlag?: number },
  ): Promise<void> => post<void>(`/schedules/reserves/${id}/cancel`, payload),

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

  /** 按天聚合每日名额（工作台日历：A=已约 B=当天可接受预约总数；无排班日期不返回） */
  getSlotDailySummary: (
    startDate: string,
    endDate: string,
    storeId?: number,
  ): Promise<import('@/types').SlotDailySummary[]> =>
    get('/schedule-rules/slot-daily-summary', { startDate, endDate, storeId }),
}

export const appointmentConfigApi = {
  getConfig: (storeId?: number): Promise<import('@/types').AppointmentConfig> =>
    get('/appointment-config', { storeId }),
}

export const medicalStaffApi = {
  /** 本店启用人员（开始养护执行人下拉） */
  getStaffList: (params: { keyword?: string; staffRole?: number; status?: number; page?: number; size?: number }): Promise<PageResult<import('@/types').MedicalStaff>> =>
    get('/medical-staff', params),
}

export const departmentApi = {
  /** 科室列表（授权自动计费取第一条启用科室的收费标准；接口返回 PageResult，此处解包为数组） */
  getDepartmentList: async (storeId?: number): Promise<Department[]> => {
    const res = await get<PageResult<Department>>('/departments', { storeId, size: 200 })
    return res?.list ?? []
  },
}
