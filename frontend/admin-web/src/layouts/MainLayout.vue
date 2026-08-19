<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <img v-if="appStore.systemSettings.logoUrl" :src="appStore.systemSettings.logoUrl" alt="Logo" class="logo-img" v-show="!appStore.sidebarCollapsed">
        <img v-else src="/src/assets/logo.svg" alt="Careld" class="logo-img" v-show="!appStore.sidebarCollapsed">
        <span class="logo-text" v-show="!appStore.sidebarCollapsed">{{ appStore.systemSettings.systemName || 'Careld运营中心' }}</span>
        <el-icon :size="24" v-if="appStore.sidebarCollapsed"><Monitor /></el-icon>
      </div>
      <el-menu
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
      >
        <!-- 动态渲染菜单 -->
        <template v-for="menu in permStore.menus" :key="menu.id">
          <!-- 目录类型: 有子菜单 -->
          <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.menuPath || String(menu.id)">
            <template #title>
              <el-icon v-if="menu.menuIcon"><component :is="menu.menuIcon" /></el-icon>
              <span>{{ menu.menuName }}</span>
            </template>
            <el-menu-item
              v-for="child in menu.children"
              :key="child.id"
              :index="child.menuPath"
            >
              <el-icon v-if="child.menuIcon"><component :is="child.menuIcon" /></el-icon>
              <template #title>{{ child.menuName }}</template>
            </el-menu-item>
          </el-sub-menu>
          <!-- 菜单类型: 无子菜单 -->
          <el-menu-item v-else :index="menu.menuPath">
            <el-icon v-if="menu.menuIcon"><component :is="menu.menuIcon" /></el-icon>
            <template #title>{{ menu.menuName }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-button
            type="text"
            @click="appStore.toggleSidebar()"
            class="collapse-btn"
          >
            <el-icon :size="20">
              <Fold v-if="!appStore.sidebarCollapsed" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <breadcrumb />
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :icon="UserFilled" />
              <span class="username">{{ userStore.userInfo?.realName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="settings">账号设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  HomeFilled,
  Shop,
  Monitor,
  UserFilled,
  Setting,
  Fold,
  Expand,
  ArrowDown,
  OfficeBuilding,
  DataAnalysis,
  Document
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import Breadcrumb from '@/components/Breadcrumb.vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const permStore = usePermissionStore()
const pendingCount = ref(0)
const userType = computed(() => userStore.userInfo?.userType || 1)

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      permStore.clear()
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
  display: flex;
}

.sidebar {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;

  .logo {
    height: 64px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: bold;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);

    .logo-img {
      width: 32px;
      height: 32px;
      margin-right: 8px;
    }

    .logo-text {
      white-space: nowrap;
    }
  }

  .el-menu {
    border-right: none;
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 64px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      color: #666;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 24px;

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username {
        font-size: 14px;
        color: #333;
      }
    }
  }
}

.main-content {
  flex: 1;
  padding: 24px;
  background-color: #f5f5f5;
  overflow: auto;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
