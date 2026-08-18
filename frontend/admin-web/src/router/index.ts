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
          path: 'organization',
          redirect: '/organization/centers',
          children: [
            {
              path: 'centers',
              name: 'OpsCenters',
              component: () => import('@/views/organization/centers.vue'),
              meta: { title: '运营中心', icon: 'OfficeBuilding' }
            },
            {
              path: 'agents',
              name: 'Agents',
              component: () => import('@/views/organization/agents.vue'),
              meta: { title: '代理商管理', icon: 'Connection' }
            }
          ]
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
            }
          ]
        },
        {
          path: 'device',
          redirect: '/device/list',
          children: [
            {
              path: 'list',
              name: 'DeviceList',
              component: () => import('@/views/device/index.vue'),
              meta: { title: '设备列表', icon: 'Monitor' }
            },
            {
              path: 'types',
              name: 'DeviceTypes',
              component: () => import('@/views/device/types.vue'),
              meta: { title: '设备类型', icon: 'Cpu' }
            }
          ]
        },
        {
          path: 'user',
          name: 'User',
          component: () => import('@/views/user/index.vue'),
          meta: { title: '用户管理', icon: 'UserFilled' }
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
