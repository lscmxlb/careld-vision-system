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
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '数据看板', icon: 'DataBoard' }
        },
        {
          path: 'child',
          name: 'Child',
          component: () => import('@/views/child/index.vue'),
          meta: { title: '儿童档案', icon: 'User' }
        },
        {
          path: 'schedule',
          name: 'Schedule',
          component: () => import('@/views/schedule/index.vue'),
          meta: { title: '排班预约', icon: 'Calendar' }
        },
        {
          path: 'vision',
          name: 'Vision',
          component: () => import('@/views/vision/index.vue'),
          meta: { title: '视力记录', icon: 'View' }
        },
        {
          path: 'device',
          name: 'Device',
          component: () => import('@/views/device/index.vue'),
          meta: { title: '设备管理', icon: 'Monitor', managerOnly: true }
        },
        {
          path: 'department',
          name: 'Department',
          component: () => import('@/views/department/index.vue'),
          meta: { title: '科室管理', icon: 'OfficeBuilding', managerOnly: true }
        }
      ]
    }
  ]
})

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  if (to.meta?.public) {
    next()
    return
  }
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }
  // 权限控制：店长才能访问特定页面
  if (to.meta?.managerOnly && userStore.userInfo?.userType !== 2) {
    next('/dashboard')
    return
  }
  next()
})

export default router
