/**
 * TV设备相关API
 */
import request from './request'
import type { Device, DeviceBindRequest, PageResponse } from '@/types'

export const deviceApi = {
  // 获取设备列表
  getDeviceList: (params: {
    storeId?: number
    status?: number
    keyword?: string
    page?: number
    size?: number
  }): Promise<PageResponse<Device>> => {
    return request.get('/devices', { params })
  },

  // 获取设备详情
  getDeviceDetail: (id: number): Promise<Device> => {
    return request.get(`/devices/${id}`)
  },

  // 新增设备
  createDevice: (data: Partial<Device>): Promise<number> => {
    return request.post('/devices', data)
  },

  // 更新设备
  updateDevice: (id: number, data: Partial<Device>): Promise<void> => {
    return request.put(`/devices/${id}`, data)
  },

  // 删除设备
  deleteDevice: (id: number): Promise<void> => {
    return request.delete(`/devices/${id}`)
  },

  // 绑定设备
  bindDevice: (data: DeviceBindRequest): Promise<Device> => {
    return request.post('/devices/bind', data)
  },

  // 解绑设备
  unbindDevice: (id: number): Promise<void> => {
    return request.post(`/devices/${id}/unbind`)
  },

  // 更新设备校准数据
  updateCalibration: (id: number, data: {
    calibrationStatus: number
    calibrationData: {
      pixelPerMm: number
      screenWidthMm: number
      screenHeightMm: number
      calibrationTime: string
      calibratedBy: string
    }
  }): Promise<void> => {
    return request.put(`/devices/${id}/calibration`, data)
  }
}
