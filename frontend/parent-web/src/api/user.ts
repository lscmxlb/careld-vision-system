/**
 * 用户相关 API
 */
import request from './request'

export const userApi = {
  /** 修改本人密码（校验旧密码） */
  changePassword: (oldPassword: string, newPassword: string): Promise<void> => {
    return request.post('/users/me/password', { oldPassword, newPassword })
  }
}
