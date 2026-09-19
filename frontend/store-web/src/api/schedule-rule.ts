/**
 * 排班规则 / 预约规则 / 医务人员相关API
 */
import request from './request'
import type { ScheduleRule, ScheduleSlot, SlotDailySummary, AppointmentConfig, MedicalStaff, PageResult } from '@/types'

export const scheduleRuleApi = {
  // 排班规则列表（含例外日）
  getRuleList: (storeId?: number): Promise<ScheduleRule[]> => {
    return request.get('/schedule-rules', { params: { storeId } })
  },

  // 创建排班规则
  createRule: (data: Partial<ScheduleRule>): Promise<number> => {
    return request.post('/schedule-rules', data)
  },

  // 修改排班规则
  updateRule: (id: number, data: Partial<ScheduleRule>): Promise<void> => {
    return request.put(`/schedule-rules/${id}`, data)
  },

  // 删除排班规则
  deleteRule: (id: number): Promise<void> => {
    return request.delete(`/schedule-rules/${id}`)
  },

  // 查询某日期的可约时段
  getSlots: (date: string, storeId?: number): Promise<ScheduleSlot[]> => {
    return request.get('/schedule-rules/slots', { params: { storeId, date } })
  },

  // 查询日期范围内的可约日期（去重）
  getAvailableDates: (startDate: string, endDate: string, storeId?: number): Promise<string[]> => {
    return request.get('/schedule-rules/available-dates', { params: { storeId, startDate, endDate } })
  },

  // 按天聚合每日名额（日历 A=已约 / B=当天总可约容量；无排班日期不返回）
  getSlotDailySummary: (startDate: string, endDate: string, storeId?: number): Promise<SlotDailySummary[]> => {
    return request.get('/schedule-rules/slot-daily-summary', { params: { startDate, endDate, storeId } })
  }
}

export const appointmentConfigApi = {
  // 读取预约规则配置（无配置返回默认值）
  getConfig: (storeId?: number): Promise<AppointmentConfig> => {
    return request.get('/appointment-config', { params: { storeId } })
  },

  // 保存预约规则配置
  saveConfig: (data: AppointmentConfig): Promise<void> => {
    return request.put('/appointment-config', data)
  }
}

export const medicalStaffApi = {
  // 医务人员列表
  getStaffList: (params: {
    keyword?: string
    staffRole?: number
    status?: number
    page?: number
    size?: number
  }): Promise<PageResult<MedicalStaff>> => {
    return request.get('/medical-staff', { params })
  },

  // 新增医务人员
  createStaff: (data: Partial<MedicalStaff>): Promise<MedicalStaff> => {
    return request.post('/medical-staff', data)
  },

  // 修改医务人员
  updateStaff: (id: number, data: Partial<MedicalStaff>): Promise<void> => {
    return request.put(`/medical-staff/${id}`, data)
  },

  // 启用/禁用
  changeStatus: (id: number, status: number): Promise<void> => {
    return request.put(`/medical-staff/${id}/status`, { status })
  },

  // 重置登录密码
  resetPassword: (id: number, password?: string): Promise<void> => {
    return request.put(`/medical-staff/${id}/password`, { loginPassword: password })
  },

  // 删除医务人员
  deleteStaff: (id: number): Promise<void> => {
    return request.delete(`/medical-staff/${id}`)
  }
}
