/**
 * 操作日志相关API
 */
import request from './request'
import type { OperationLog, OperationLogQuery, PageResponse } from '@/types'

export const operationLogApi = {
  // 获取操作日志列表（分页）
  getOperationLogList: (params: OperationLogQuery): Promise<PageResponse<OperationLog>> => {
    return request.get('/operation-logs', { params })
  },

  // 获取操作日志详情
  getOperationLogDetail: (id: number): Promise<OperationLog> => {
    return request.get(`/operation-logs/${id}`)
  }
}
