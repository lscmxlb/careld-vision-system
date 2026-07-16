/**
 * Axios 请求封装
 */
import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 创建 axios 实例
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message, data } = response.data
    if (code === 200) {
      return data
    }
    // 业务异常（HTTP 200 但 code 非 200）：优先展示后端返回的具体信息
    const msg = message || '请求失败'
    ElMessage.error(msg)
    return Promise.reject(Object.assign(new Error(msg), { code, message, data }))
  },
  (error) => {
    const { response } = error
    if (response) {
      // 后端有响应：优先使用响应体里的 message，避免把业务错误误报为“网络错误”
      const msg = response.data?.message
      switch (response.status) {
        case 401:
          ElMessage.error(msg || '登录已过期，请重新登录')
          useUserStore().logout()
          window.location.href = '/login'
          break
        case 403:
          ElMessage.error(msg || '没有权限访问')
          break
        case 404:
          ElMessage.error(msg || '请求的资源不存在')
          break
        case 500:
          ElMessage.error(msg || '服务器内部错误')
          break
        default:
          ElMessage.error(msg || `请求失败（${response.status}）`)
      }
    } else {
      // 无响应：才是真正的网络问题
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  }
)

export default request
