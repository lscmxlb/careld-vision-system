/**
 * uni.request Promise 封装
 * 语义对齐 PC 端 axios 拦截器：注入 Bearer token、统一错误提示、401 跳登录
 */
import { clearAuth, getToken } from './auth'

const BASE_URL = '/api/v1'
const TIMEOUT = 30000
const PAGE_LOGIN = '/pages/login/index'

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface RequestOptions {
  url: string
  method?: Method
  /** 请求体（POST/PUT） */
  data?: Record<string, unknown>
  /** 查询参数（GET） */
  params?: Record<string, unknown>
  /** 不自动弹出错误提示（由调用方自行处理） */
  silent?: boolean
  /** 不注入 token（登录类接口） */
  noAuth?: boolean
  /** 401 时不自动跳登录页 */
  noAuthRedirect?: boolean
}

interface Envelope<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

let lastToast = { text: '', at: 0 }
let redirecting = false

export function toast(message: string) {
  if (!message) return
  const now = Date.now()
  // 同一条错误 1 秒内只提示一次，避免多接口并发时重复弹窗
  if (lastToast.text === message && now - lastToast.at < 1000) return
  lastToast = { text: message, at: now }
  uni.showToast({ title: message, icon: 'none', duration: 2200 })
}

function redirectToLogin() {
  if (redirecting) return
  redirecting = true
  clearAuth()
  const pages = getCurrentPages()
  const current = pages.length ? (pages[pages.length - 1] as unknown as { route?: string }) : undefined
  if (current && current.route === 'pages/login/index') {
    redirecting = false
    return
  }
  uni.reLaunch({
    url: PAGE_LOGIN,
    complete: () => {
      setTimeout(() => {
        redirecting = false
      }, 300)
    },
  })
}

/** 序列化查询参数（数组按逗号拼接，对齐后端 statuses=1,2,3 口径） */
export function buildQuery(params?: Record<string, unknown>): string {
  if (!params) return ''
  const parts: string[] = []
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value === undefined || value === null || value === '') return
    if (Array.isArray(value)) {
      if (!value.length) return
      parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(value.join(','))}`)
    } else {
      parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    }
  })
  return parts.length ? `?${parts.join('&')}` : ''
}

function statusMessage(status: number): string {
  const map: Record<number, string> = {
    400: '请求参数有误',
    401: '登录已过期，请重新登录',
    403: '没有权限访问',
    404: '请求的资源不存在',
    405: '请求方式不被支持',
    500: '服务器内部错误',
  }
  return map[status] || `请求失败（${status}）`
}

export function request<T>(options: RequestOptions): Promise<T> {
  const { url, method = 'GET', data, params, silent, noAuth, noAuthRedirect } = options
  const fullUrl = (url.startsWith('http') ? url : BASE_URL + url) + buildQuery(params)

  return new Promise<T>((resolve, reject) => {
    const header: Record<string, string> = { 'Content-Type': 'application/json' }
    const token = getToken()
    if (!noAuth && token) header.Authorization = `Bearer ${token}`

    uni.request({
      url: fullUrl,
      method,
      data: data as never,
      header,
      timeout: TIMEOUT,
      success: (res) => {
        const status = res.statusCode
        const body = res.data as Envelope<T> | string | undefined

        if (status >= 200 && status < 300) {
          // 后端统一信封 { code, message, data }
          if (body && typeof body === 'object' && 'code' in body) {
            const envelope = body as Envelope<T>
            if (envelope.code === 200) {
              resolve(envelope.data)
              return
            }
            const msg = envelope.message || '请求失败'
            if (!silent) toast(msg)
            reject(Object.assign(new Error(msg), { code: envelope.code, status }))
            return
          }
          resolve(body as T)
          return
        }

        const bodyMessage =
          body && typeof body === 'object' && 'message' in body
            ? (body as Envelope<T>).message
            : undefined
        const msg = bodyMessage || statusMessage(status)

        if (status === 401) {
          if (!silent) toast(msg)
          if (!noAuthRedirect) redirectToLogin()
        } else if (!silent) {
          toast(msg)
        }
        reject(Object.assign(new Error(msg), { status }))
      },
      fail: () => {
        const msg = '网络连接失败，请检查网络'
        if (!silent) toast(msg)
        reject(new Error(msg))
      },
    })
  })
}

export const get = <T>(url: string, params?: Record<string, unknown>, options?: Partial<RequestOptions>) =>
  request<T>({ url, method: 'GET', params, ...options })

export const post = <T>(url: string, data?: Record<string, unknown>, options?: Partial<RequestOptions>) =>
  request<T>({ url, method: 'POST', data, ...options })

export const put = <T>(url: string, data?: Record<string, unknown>, options?: Partial<RequestOptions>) =>
  request<T>({ url, method: 'PUT', data, ...options })

export const del = <T>(url: string, options?: Partial<RequestOptions>) =>
  request<T>({ url, method: 'DELETE', ...options })

export { redirectToLogin }
