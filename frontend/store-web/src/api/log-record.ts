/**
 * 日志记录相关API
 */
import request from './request'
import type { OperationLog, OperationLogQuery, PageResult } from '@/types'

export const logRecordApi = {
  // 获取日志列表（分页）
  getLogList: (params: OperationLogQuery): Promise<PageResult<OperationLog>> => {
    return request.get('/operation-logs', { params })
  },

  // 获取日志详情
  getLogDetail: (id: number): Promise<OperationLog> => {
    return request.get(`/operation-logs/${id}`)
  }
}
