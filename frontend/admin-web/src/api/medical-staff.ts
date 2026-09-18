/**
 * 医务人员相关API（与医院端共用同一后端接口，数据同源）
 */
import request from './request'
import type { MedicalStaff, MedicalStaffQuery, PageResponse } from '@/types'

export const medicalStaffApi = {
  // 医务人员列表（总部用户传 storeId 按医院筛选）
  getStaffList: (params: MedicalStaffQuery): Promise<PageResponse<MedicalStaff>> => {
    return request.get('/medical-staff', { params })
  },

  // 新增医务人员（不传密码则默认 4009993608）
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

  // 删除
  deleteStaff: (id: number): Promise<void> => {
    return request.delete(`/medical-staff/${id}`)
  }
}
