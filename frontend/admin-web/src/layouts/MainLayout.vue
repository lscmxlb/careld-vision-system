<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <img src="/src/assets/logo.svg" alt="Careld" class="logo-img" v-if="!appStore.sidebarCollapsed">
        <span class="logo-text" v-if="!appStore.sidebarCollapsed">Careld运营中心</span>
        <el-icon :size="24" v-else><Monitor /></el-icon>
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
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>

        <el-sub-menu index="/organization">
          <template #title>
            <el-icon><OfficeBuilding /></el-icon>
            <span>组织架构</span>
          </template>
          <el-menu-item index="/organization/centers">运营中心</el-menu-item>
          <el-menu-item index="/organization/agents">代理商管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/store">
          <template #title>
            <el-icon><Shop /></el-icon>
            <span>门店管理</span>
          </template>
          <el-menu-item index="/store/list">门店列表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/device">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>设备管理</span>
          </template>
          <el-menu-item index="/device/list">设备列表</el-menu-item>
          <el-menu-item index="/device/types">设备类型</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/user">
          <el-icon><UserFilled /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>

        <el-menu-item index="/statistics">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>统计报表</template>
        </el-menu-item>

        <el-menu-item index="/operation-log">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>

        <el-menu-item index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
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
import { ref } from 'vue'
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
import Breadcrumb from '@/components/Breadcrumb.vue'

const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const pendingCount = ref(0)

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
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
