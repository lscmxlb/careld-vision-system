import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import uviewPlus from 'uview-plus'
import App from './App.vue'
import { getToken } from '@/utils/auth'

const PAGE_LOGIN = '/pages/login/index'

function isLoginPage(url?: string) {
  if (!url) return false
  return url.split('?')[0].replace(/^\//, '') === 'pages/login/index'
}

let redirecting = false

function redirectToLogin() {
  if (redirecting) return
  redirecting = true
  uni.reLaunch({
    url: PAGE_LOGIN,
    complete: () => {
      setTimeout(() => {
        redirecting = false
      }, 300)
    },
  })
}

export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  app.use(uviewPlus)

  // 未登录保护：深链直接进入业务页时统一拉回登录页
  ;(['navigateTo', 'redirectTo', 'reLaunch', 'switchTab'] as const).forEach((api) => {
    uni.addInterceptor(api, {
      invoke(args: { url?: string }) {
        if (isLoginPage(args.url) || getToken()) return args
        redirectToLogin()
        return false
      },
    })
  })

  return { app }
}

export { redirectToLogin }
