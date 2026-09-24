/**
 * 微信支付（短信服务充值）配置与充值记录API（管理后台系统设置，总部维护）
 */
import request from './request'

export interface PayConfig {
  enabled: boolean
  mchId: string
  apiV3KeyConfigured: boolean
  apiV3KeyMasked: string
  serialNo: string
  /** 商户 API 私钥文件路径（apiclient_key.pem） */
  privateKeyPath: string
  /** 微信支付公钥文件路径（pub_key.pem，公钥模式验签用） */
  publicKeyPath: string
  /** 微信支付公钥 ID（形如 PUB_KEY_ID_…） */
  publicKeyId: string
  appId: string
  notifyUrl: string
  /** 回调地址（网络可达时由系统按回调域名前缀自动拼装） */
  suggestedNotifyUrl: string
  /** 模拟支付开关（仅联调期使用，正式运营必须关闭） */
  mockEnabled: boolean
  realReady: boolean
  notReadyReason: string
}

export interface PayConfigSaveRequest {
  enabled: boolean
  mchId: string
  /** 留空表示不修改已保存的 APIv3 密钥 */
  apiV3Key?: string
  serialNo: string
  privateKeyPath: string
  publicKeyPath: string
  publicKeyId: string
  appId: string
  notifyUrl: string
  mockEnabled: boolean
}

export interface RechargeOrder {
  orderNo: string
  storeId: number
  storeName: string
  amount: number
  /** 0=待支付 1=已支付 2=已关闭 */
  status: number
  transactionId?: string
  tradeState?: string
  mock?: boolean
  expireAt?: string
  paidAt?: string
  createdAt?: string
}

export interface RechargeOrderPage {
  list: RechargeOrder[]
  pagination: { page: number; size: number; total: number; pages: number }
  /** 当前查询条件下已支付金额合计（元） */
  paidAmount: number
}

export interface RechargeOrderQuery {
  storeId?: number
  status?: number
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export interface PayProbeResult {
  mchId?: string
  appId?: string
  notifyUrl?: string
  realReady?: boolean
  /** 回调验签方式：微信支付公钥 / 平台证书 */
  verifyMode?: string
  privateKeyOk?: boolean
  privateKeyMessage?: string
  publicKeyOk?: boolean
  publicKeyMessage?: string
  publicKeyId?: string
  certificateCount?: number
  certificateMessage?: string
}

export const payApi = {
  /** 查询微信支付配置（APIv3 密钥只回掩码） */
  getConfig: (): Promise<PayConfig> => request.get('/notify/pay/admin/config'),

  /** 保存微信支付配置 */
  saveConfig: (data: PayConfigSaveRequest): Promise<PayConfig> =>
    request.put('/notify/pay/admin/config', data),

  /** 诊断：校验商户私钥可读性与平台证书下载 */
  probe: (): Promise<PayProbeResult> => request.get('/notify/pay/admin/probe'),

  /** 分页查询各医院扫码充值流水 */
  getOrders: (params: RechargeOrderQuery): Promise<RechargeOrderPage> =>
    request.get('/notify/pay/admin/orders', { params })
}
