import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录', public: true }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/profile',
      children: [
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/index.vue'),
          meta: { title: '子女档案', icon: 'User' }
        },
        {
          path: 'report',
          name: 'Report',
          component: () => import('@/views/report/index.vue'),
          meta: { title: '检测报告', icon: 'Document' }
        },
        {
          path: 'trend',
          name: 'Trend',
          component: () => import('@/views/trend/index.vue'),
          meta: { title: '视力趋势', icon: 'TrendCharts' }
        },
        {
          path: 'appointment',
          name: 'Appointment',
          component: () => import('@/views/appointment/index.vue'),
          meta: { title: '预约养护', icon: 'Calendar' }
        },
        {
          path: 'appointment/list',
          name: 'AppointmentList',
          component: () => import('@/views/appointment/list.vue'),
          meta: { title: '我的预约', icon: 'List' }
        },
        {
          path: 'mine',
          name: 'Mine',
          component: () => import('@/views/mine/index.vue'),
          meta: { title: '个人中心', icon: 'UserFilled' }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta?.public) {
    next()
    return
  }
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }
  next()
})

export default router
