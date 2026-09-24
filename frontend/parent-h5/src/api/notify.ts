/**
 * 通知服务相关API（家长端微信公众号绑定）
 */
import { get, post } from '@/utils/request'
import type { WechatStatus, WechatBindQr } from '@/types'

export const notifyApi = {
  /** 查询本人微信公众号绑定状态 */
  wechatStatus: (): Promise<WechatStatus> => get<WechatStatus>('/notify/wechat/status'),

  /** 取扫码绑定二维码（带参二维码，扫码关注后自动绑定真实 openid） */
  wechatBindQr: (): Promise<WechatBindQr> => get<WechatBindQr>('/notify/wechat/bind-qr'),

  /** 绑定微信公众号（仅未配置公众号凭据时可用，写入模拟 openid） */
  bindWechat: (openid?: string): Promise<WechatStatus> =>
    post<WechatStatus>('/notify/wechat/bind', openid ? { openid } : {}),

  /** 解除微信公众号绑定 */
  unbindWechat: (): Promise<WechatStatus> => post<WechatStatus>('/notify/wechat/unbind', {})
}
