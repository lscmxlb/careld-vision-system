/**
 * 通知服务相关API（短信 / 微信模板消息 / 短信余额续费）
 */
import request from './request'
import type {
  NotifyConfig,
  NotifyRecordPage,
  NotifyRecordQuery,
  NotifyAccount,
  PayStatus,
  RechargeOrder
} from '@/types'

export const notifyApi = {
  // 通知服务配置（通道开关 + 通知类型）
  getConfig: (storeId?: number): Promise<NotifyConfig> => {
    return request.get('/notify/config', { params: { storeId } })
  },

  // 保存通知服务配置
  saveConfig: (data: NotifyConfig, storeId?: number): Promise<NotifyConfig> => {
    return request.put('/notify/config', data, { params: { storeId } })
  },

  // 通知记录（含当前查询条件下的费用总额与可用余额）
  getRecords: (params: NotifyRecordQuery & { storeId?: number }): Promise<NotifyRecordPage> => {
    return request.get('/notify/records', { params })
  },

  // 短信费用账户
  getAccount: (storeId?: number): Promise<NotifyAccount> => {
    return request.get('/notify/account', { params: { storeId } })
  },

  // ==================== 充值（微信扫码支付） ====================

  // 支付环境状态（真实支付是否就绪 / 是否模拟模式）
  getPayStatus: (): Promise<PayStatus> => {
    return request.get('/notify/pay/status')
  },

  // 创建充值订单（返回二维码链接）
  createPayOrder: (amount: number, storeId?: number): Promise<RechargeOrder> => {
    return request.post('/notify/pay/orders', { amount }, { params: { storeId } })
  },

  // 查询单个订单（轮询支付结果）
  getPayOrder: (orderNo: string, storeId?: number): Promise<RechargeOrder> => {
    return request.get(`/notify/pay/orders/${orderNo}`, { params: { storeId } })
  },

  // 模拟支付（仅联调环境可用）
  mockPayOrder: (orderNo: string, storeId?: number): Promise<RechargeOrder> => {
    return request.post(`/notify/pay/orders/${orderNo}/mock-pay`, undefined, { params: { storeId } })
  }
}
