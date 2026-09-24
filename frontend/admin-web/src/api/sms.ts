/**
 * 短信服务配置API（阿里云短信，管理后台系统设置）
 */
import request from './request'

export interface SmsEventTemplate {
  /** 事件类型：reserve_cancelled/reserve_adjusted/child_created/care_completed */
  eventType: string
  templateCode: string
  templateFields: string
  templateRoles: string
}

export interface SmsConfig {
  enabled: boolean
  accessKeyId: string
  accessKeySecretConfigured: boolean
  accessKeySecretMasked: string
  signName: string
  templateCode: string
  templateParam: string
  noticeTemplateCode: string
  noticeTemplateParam: string
  noticeTemplateFields: string
  noticeTemplateRoles: string
  noticeTemplateEvents: string
  mockFallback: boolean
  enableNotice: boolean
  careReminderMinutes: number
  eventTemplates: SmsEventTemplate[]
}

export interface SmsConfigSaveRequest {
  enabled: boolean
  accessKeyId: string
  /** 留空表示不修改已保存的 Secret */
  accessKeySecret?: string
  signName: string
  templateCode: string
  templateParam: string
  noticeTemplateCode: string
  noticeTemplateParam: string
  noticeTemplateFields: string
  noticeTemplateRoles: string
  noticeTemplateEvents: string
  enableNotice: boolean
  careReminderMinutes: number
  eventTemplates: SmsEventTemplate[]
}

export const smsApi = {
  getConfig: (): Promise<SmsConfig> => request.get('/auth/sms/config'),

  saveConfig: (data: SmsConfigSaveRequest): Promise<void> => request.put('/auth/sms/config', data),

  /** 用验证码模板真实发送一条测试短信 */
  testSend: (phone: string): Promise<void> => request.post('/auth/sms/config/test', { phone }),

  /** 用通知模板真实发送一条样例通知短信（eventType 决定用主模板还是该事件的独立模板） */
  testSendNotice: (phone: string, eventType: string): Promise<string> =>
    request.post('/notify/sms/config/test', { phone, eventType }),

  /** 预览通知模板变量拼装结果（不真实发送） */
  previewNotice: (eventType: string): Promise<string> =>
    request.post('/notify/sms/config/test', { phone: '13800000000', dryRun: true, eventType })
}
