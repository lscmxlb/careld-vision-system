/**
 * 组织架构相关API（总部/运营中心/代理商）
 */
import request from './request'
import type { BrandHq, OpsCenter, Agent, PageResponse } from '@/types'

export const orgApi = {
  // ===== 总部 =====
  getHqList: (params: {
    keyword?: string
    page?: number
    size?: number
  }): Promise<PageResponse<BrandHq>> => {
    return request.get('/org/hq', { params })
  },

  getAllHq: (): Promise<BrandHq[]> => {
    return request.get('/org/hq/all')
  },

  getHqById: (id: number): Promise<BrandHq> => {
    return request.get(`/org/hq/${id}`)
  },

  createHq: (data: Partial<BrandHq>): Promise<number> => {
    return request.post('/org/hq', data)
  },

  updateHq: (id: number, data: Partial<BrandHq>): Promise<void> => {
    return request.put(`/org/hq/${id}`, data)
  },

  deleteHq: (id: number): Promise<void> => {
    return request.delete(`/org/hq/${id}`)
  },

  // ===== 运营中心 =====
  getCenterList: (params: {
    hqId?: number
    keyword?: string
    page?: number
    size?: number
  }): Promise<PageResponse<OpsCenter>> => {
    return request.get('/org/centers', { params })
  },

  getAllCenters: (hqId?: number): Promise<OpsCenter[]> => {
    return request.get('/org/centers/all', { params: { hqId } })
  },

  getCenterById: (id: number): Promise<OpsCenter> => {
    return request.get(`/org/centers/${id}`)
  },

  createCenter: (data: Partial<OpsCenter>): Promise<number> => {
    return request.post('/org/centers', data)
  },

  updateCenter: (id: number, data: Partial<OpsCenter>): Promise<void> => {
    return request.put(`/org/centers/${id}`, data)
  },

  deleteCenter: (id: number): Promise<void> => {
    return request.delete(`/org/centers/${id}`)
  },

  // ===== 代理商 =====
  getAgentList: (params: {
    centerId?: number
    keyword?: string
    page?: number
    size?: number
  }): Promise<PageResponse<Agent>> => {
    return request.get('/org/agents', { params })
  },

  getAllAgents: (centerId?: number): Promise<Agent[]> => {
    return request.get('/org/agents/all', { params: { centerId } })
  },

  getAgentById: (id: number): Promise<Agent> => {
    return request.get(`/org/agents/${id}`)
  },

  createAgent: (data: Partial<Agent>): Promise<number> => {
    return request.post('/org/agents', data)
  },

  updateAgent: (id: number, data: Partial<Agent>): Promise<void> => {
    return request.put(`/org/agents/${id}`, data)
  },

  deleteAgent: (id: number): Promise<void> => {
    return request.delete(`/org/agents/${id}`)
  }
}
