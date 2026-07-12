/**
 * TV设备相关API
 */
import request from './request'
import type { Device, DeviceBindRequest, PageResult } from '@/types'

export const deviceApi = {
  // 获取设备列表
  getDeviceList: (params: {
    storeId?: number
    status?: number
    page?: number
    size?: number
  }): Promise<PageResult<Device>> => {
    return request.get('/devices', { params })
  },

  // 获取设备详情
  getDeviceDetail: (id: number): Promise<Device> => {
    return request.get(`/devices/${id}`)
  },

  // 绑定设备
  bindDevice: (data: DeviceBindRequest): Promise<Device> => {
    return request.post('/devices/bind', data)
  },

  // 解绑设备
  unbindDevice: (id: number): Promise<void> => {
    return request.post(`/devices/${id}/unbind`)
  },

  // 同步设备
  syncDevice: (id: number): Promise<void> => {
    return request.post(`/devices/${id}/sync`)
  }
}
