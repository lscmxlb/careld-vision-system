/**
 * 短信服务配置API（阿里云短信，管理后台系统设置）
 */
import request from './request'

export interface SmsConfig {
  enabled: boolean
  accessKeyId: string
  accessKeySecretConfigured: boolean
  accessKeySecretMasked: string
  signName: string
  templateCode: string
  templateParam: string
  mockFallback: boolean
  enableNotice: boolean
  appointmentReminderHours: number
}

export interface SmsConfigSaveRequest {
  enabled: boolean
  accessKeyId: string
  /** 留空表示不修改已保存的 Secret */
  accessKeySecret?: string
  signName: string
  templateCode: string
  templateParam: string
  enableNotice: boolean
  appointmentReminderHours: number
}

export const smsApi = {
  getConfig: (): Promise<SmsConfig> => request.get('/auth/sms/config'),

  saveConfig: (data: SmsConfigSaveRequest): Promise<void> => request.put('/auth/sms/config', data),

  testSend: (phone: string): Promise<void> => request.post('/auth/sms/config/test', { phone })
}
