/**
 * 设备类型相关API
 */
import request from './request'
import type { DeviceType, PageResponse } from '@/types'

export const deviceTypeApi = {
  getDeviceTypeList: (params: {
    keyword?: string
    page?: number
    size?: number
  }): Promise<PageResponse<DeviceType>> => {
    return request.get('/device-types', { params })
  },

  getAllDeviceTypes: (): Promise<DeviceType[]> => {
    return request.get('/device-types/all')
  },

  getDeviceTypeById: (id: number): Promise<DeviceType> => {
    return request.get(`/device-types/${id}`)
  },

  createDeviceType: (data: Partial<DeviceType>): Promise<number> => {
    return request.post('/device-types', data)
  },

  updateDeviceType: (id: number, data: Partial<DeviceType>): Promise<void> => {
    return request.put(`/device-types/${id}`, data)
  },

  deleteDeviceType: (id: number): Promise<void> => {
    return request.delete(`/device-types/${id}`)
  }
}
