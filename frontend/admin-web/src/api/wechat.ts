/**
 * 微信公众号通知配置API（管理后台系统设置，总部维护）
 */
import request from './request'

export interface WechatConfig {
  appId: string
  appSecretConfigured: boolean
  appSecretMasked: string
  templateId: string
  /** template=公众号模板消息 / subscribe=公众号订阅通知 */
  templateType: string
  /** 模板字段映射：摘要,儿童姓名,通知类型,备注 */
  fields: string
  /** 字段语义（与 fields 一一对应） */
  fieldRoles: string
  accountName: string
  realSendReady: boolean
  /** 公众号回调公网地址（公众平台「服务器配置」URL 填这个） */
  callbackUrl: string
  /** 公众号服务器配置 Token（公众平台「服务器配置」Token 填这个） */
  serverToken: string
  /** 回调公网地址前缀，如 https://h5.careld.net */
  publicBaseUrl: string
}

export interface WechatConfigSaveRequest {
  appId: string
  /** 留空表示不修改已保存的 AppSecret */
  appSecret?: string
  templateId: string
  templateType: string
  fields: string
  fieldRoles?: string
  accountName: string
  /** 回调公网地址前缀，留空保持不变 */
  publicBaseUrl?: string
  /** 留空保持不变；填新的则覆盖服务器配置 Token */
  serverToken?: string
}

export interface WechatTemplateItem {
  templateId?: string
  title?: string
  content?: string
  example?: string
  [key: string]: unknown
}

export interface WechatProbeResult {
  appId: string
  tokenOk: boolean
  tokenErrcode?: unknown
  tokenErrmsg?: string
  hint?: string
  configuredTemplateId: string
  configuredTemplateType: string
  templateMessageTemplates: WechatTemplateItem[]
  subscribeTemplates: WechatTemplateItem[]
  matchedType: 'template' | 'subscribe' | 'none'
  matchedTitle?: string
  matchedContent?: string
  matchedFields?: string[]
  /** 已关注该公众号的用户 openid（联调期选一个做测试发送） */
  followers?: string[]
  followerTotal?: number
  followerHint?: string
}

export interface WechatTestResult {
  errcode?: number
  errmsg?: string
  hint?: string
  _stage?: string
  _api?: string
  _requestBody?: string
  openid?: string
  templateId?: string
  templateType?: string
  fields?: string
  fieldRoles?: string
  [key: string]: unknown
}

export const wechatApi = {
  getConfig: (): Promise<WechatConfig> => request.get('/notify/wechat/config'),

  saveConfig: (data: WechatConfigSaveRequest): Promise<WechatConfig> =>
    request.put('/notify/wechat/config', data),

  probe: (): Promise<WechatProbeResult> => request.get('/notify/wechat/config/probe'),

  testSend: (openid: string, content?: string): Promise<WechatTestResult> =>
    request.post('/notify/wechat/config/test', { openid, content }),

  /** 重新生成服务器配置 Token（公众平台「服务器配置」需同步替换） */
  regenerateServerToken: (): Promise<WechatConfig> => request.post('/notify/wechat/config/server-token')
}
