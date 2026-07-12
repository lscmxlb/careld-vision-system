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
          meta: { title: '数据看板', icon: 'HomeFilled' }
        },
        {
          path: 'store',
          redirect: '/store/list',
          children: [
            {
              path: 'list',
              name: 'StoreList',
              component: () => import('@/views/store/list.vue'),
              meta: { title: '门店列表', icon: 'Shop' }
            },
            {
              path: 'audit',
              name: 'StoreAudit',
              component: () => import('@/views/store/audit.vue'),
              meta: { title: '档案审核', icon: 'DocumentChecked' }
            }
          ]
        },
        {
          path: 'schedule',
          name: 'Schedule',
          component: () => import('@/views/schedule/index.vue'),
          meta: { title: '排班监控', icon: 'Calendar' }
        },
        {
          path: 'device',
          name: 'Device',
          component: () => import('@/views/device/index.vue'),
          meta: { title: '设备管理', icon: 'Monitor' }
        },
        {
          path: 'user',
          name: 'User',
          component: () => import('@/views/user/index.vue'),
          meta: { title: '用户管理', icon: 'UserFilled' }
        },
        {
          path: 'department',
          name: 'Department',
          component: () => import('@/views/department/index.vue'),
          meta: { title: '科室管理', icon: 'OfficeBuilding' }
        },
        {
          path: 'reserve',
          name: 'Reserve',
          component: () => import('@/views/reserve/index.vue'),
          meta: { title: '预约管理', icon: 'Tickets' }
        },
        {
          path: 'vision',
          name: 'Vision',
          component: () => import('@/views/vision/index.vue'),
          meta: { title: '视力记录', icon: 'View' }
        },
        {
          path: 'statistics',
          name: 'Statistics',
          component: () => import('@/views/statistics/index.vue'),
          meta: { title: '统计报表', icon: 'DataAnalysis' }
        },
        {
          path: 'operation-log',
          name: 'OperationLog',
          component: () => import('@/views/operation-log/index.vue'),
          meta: { title: '操作日志', icon: 'Document' }
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/settings/index.vue'),
          meta: { title: '系统设置', icon: 'Setting' }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
      meta: { title: '页面不存在', public: true }
    }
  ]
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - Careld运营中心`
  }

  // 公开页面直接放行
  if (to.meta?.public) {
    next()
    return
  }

  // 检查登录状态
  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  // 初始化用户信息
  if (!userStore.userInfo) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.clearToken()
      next('/login')
      return
    }
  }

  next()
})

export default router
