/**
 * 科室管理相关API
 */
import request from './request'
import type { Department, DepartmentQuery, PageResponse } from '@/types'

export const departmentApi = {
  // 获取科室列表（分页）
  getDepartmentList: (params: DepartmentQuery): Promise<PageResponse<Department>> => {
    return request.get('/departments', { params })
  },

  // 获取科室详情
  getDepartmentDetail: (id: number): Promise<Department> => {
    return request.get(`/departments/${id}`)
  },

  // 创建科室
  createDepartment: (data: Partial<Department>): Promise<Department> => {
    return request.post('/departments', data)
  },

  // 更新科室
  updateDepartment: (id: number, data: Partial<Department>): Promise<Department> => {
    return request.put(`/departments/${id}`, data)
  },

  // 更新科室状态
  updateDepartmentStatus: (id: number, status: number): Promise<void> => {
    return request.patch(`/departments/${id}/status`, { status })
  },

  // 删除科室
  deleteDepartment: (id: number): Promise<void> => {
    return request.delete(`/departments/${id}`)
  }
}
