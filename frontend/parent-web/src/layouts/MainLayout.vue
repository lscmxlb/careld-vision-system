<template>
  <div class="mobile-layout">
    <!-- 顶部导航 -->
    <div class="mobile-header">
      <div class="header-title">{{ currentTitle }}</div>
      <el-icon :size="22" class="header-icon" @click="$router.push('/mine')">
        <Avatar />
      </el-icon>
    </div>

    <!-- 主内容 -->
    <div class="mobile-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>

    <!-- 底部导航 -->
    <div class="mobile-footer">
      <div
        v-for="item in navItems"
        :key="item.path"
        class="nav-item"
        :class="{ active: isActiveNav(item.path) }"
        @click="$router.push(item.path)"
      >
        <el-icon :size="22">
          <component :is="item.icon" />
        </el-icon>
        <span>{{ item.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  User, Document, TrendCharts, Calendar, Avatar
} from '@element-plus/icons-vue'

const route = useRoute()

const navItems = [
  { path: '/profile', label: '档案', icon: User },
  { path: '/report', label: '报告', icon: Document },
  { path: '/appointment', label: '预约', icon: Calendar },
  { path: '/trend', label: '趋势', icon: TrendCharts },
  { path: '/mine', label: '我的', icon: Avatar }
]

const currentTitle = computed(() => {
  const titles: Record<string, string> = {
    '/profile': '子女档案',
    '/report': '检测报告',
    '/trend': '视力趋势',
    '/appointment': '预约养护',
    '/appointment/list': '我的预约',
    '/mine': '个人中心'
  }
  return titles[route.path] || 'Careld家长端'
})

const isActiveNav = (path: string) => {
  if (path === '/appointment') {
    return route.path.startsWith('/appointment')
  }
  return route.path === path
}
</script>

<style scoped lang="scss">
.mobile-layout {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 60px;

  .mobile-header {
    height: 50px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 100;

    .header-title {
      font-size: 18px;
      font-weight: bold;
    }

    .header-icon {
      cursor: pointer;
      opacity: 0.9;

      &:active {
        opacity: 0.7;
      }
    }
  }

  .mobile-content {
    padding-top: 50px;
    min-height: calc(100vh - 110px);
  }

  .mobile-footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    height: 60px;
    background: #fff;
    border-top: 1px solid #e8e8e8;
    display: flex;
    justify-content: space-around;
    align-items: center;
    z-index: 100;

    .nav-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      color: #999;
      font-size: 11px;
      cursor: pointer;
      padding: 4px 0;
      transition: color 0.2s;

      &.active {
        color: #667eea;
      }

      span {
        margin-top: 2px;
      }
    }
  }
}

// 页面切换动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
