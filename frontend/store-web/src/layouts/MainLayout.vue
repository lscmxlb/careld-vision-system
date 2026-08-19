<template>
  <el-container class="main-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <span>医院管理系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/child">
          <el-icon><User /></el-icon>
          <span>儿童档案</span>
        </el-menu-item>
        <el-menu-item index="/schedule">
          <el-icon><Calendar /></el-icon>
          <span>排班预约</span>
        </el-menu-item>
        <el-menu-item index="/vision">
          <el-icon><View /></el-icon>
          <span>视力记录</span>
        </el-menu-item>
        <el-menu-item index="/device" v-if="isManager">
          <el-icon><Monitor /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item index="/department" v-if="isManager">
          <el-icon><OfficeBuilding /></el-icon>
          <span>科室管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">{{ $route.meta?.title || '医院管理' }}</div>
        <div class="header-right">
          <span class="store-name">{{ userStore.userInfo?.storeName }}</span>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ userStore.userInfo?.realName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  User,
  Calendar,
  View,
  Monitor,
  ArrowDown,
  OfficeBuilding,
  DataBoard
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const isManager = computed(() => userStore.userInfo?.userType === 2)

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;

  .sidebar {
    background-color: #001529;

    .logo {
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 16px;
      font-weight: bold;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .el-menu {
      border-right: none;
    }
  }

  .header {
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .header-left {
      font-size: 16px;
      font-weight: bold;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 20px;

      .store-name {
        color: #666;
      }

      .user-info {
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  .main-content {
    background: #f5f5f5;
    padding: 20px;
  }
}
</style>
