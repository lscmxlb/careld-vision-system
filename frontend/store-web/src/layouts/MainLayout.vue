<template>
  <el-container class="main-layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <img :src="logoImg" alt="Careld" class="logo-img">
        <span>Careld诊约助手服务</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :default-openeds="['archive', 'appointment', 'system']"
        router
        background-color="#001529"
        text-color="#fff"
        active-text-color="#1890ff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>数据中心</span>
        </el-menu-item>
        <el-sub-menu index="archive">
          <template #title>
            <el-icon><Folder /></el-icon>
            <span>档案管理</span>
          </template>
          <el-menu-item index="/child">儿童档案</el-menu-item>
          <el-menu-item index="/care-record">养护记录</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="appointment">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>预约管理</span>
          </template>
          <el-menu-item index="/appointment-record">预约记录</el-menu-item>
          <el-menu-item index="/schedule">预约管理</el-menu-item>
          <el-menu-item index="/schedule-rule">排班设置</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system" v-if="isManager">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统设置</span>
          </template>
          <el-menu-item index="/basic-info">基础信息</el-menu-item>
          <el-menu-item index="/medical-staff">医务人员</el-menu-item>
          <el-menu-item v-if="canViewLogs" index="/log-record">日志记录</el-menu-item>
        </el-sub-menu>
      </el-menu>
      <div class="sidebar-footer">
        <div class="footer-line">系统服务电话</div>
        <div class="footer-phone">400 999 3608</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <img :src="hisLogo" alt="" class="header-logo">
          <span>{{ userStore.userInfo?.storeName }}</span>
        </div>
        <div class="header-right">
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
  Calendar,
  ArrowDown,
  DataBoard,
  Folder,
  Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import logoImg from '@/assets/logo.png'
import hisLogo from '@/assets/his.png'

const router = useRouter()
const userStore = useUserStore()
const isManager = computed(() => userStore.userInfo?.userType === 2)
// 医护同为 userType=2，日志记录按后端 operationlog:view 权限决定是否展示
const canViewLogs = computed(() => userStore.userInfo?.permissions?.includes('operationlog:view') ?? false)

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
    display: flex;
    flex-direction: column;

    .logo {
      flex: none;
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      color: #fff;
      font-size: 16px;
      font-weight: bold;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);

      .logo-img {
        width: 30px;
        height: 30px;
        flex: none;
      }
    }

    .el-menu {
      border-right: none;
      flex: 1;
      overflow-y: auto;
    }

    .sidebar-footer {
      flex: none;
      text-align: center;
      padding: 12px 0 16px;
      border-top: 1px solid rgba(255, 255, 255, 0.1);

      .footer-line {
        font-size: 13px;
        line-height: 20px;
        color: rgba(255, 255, 255, 0.55);
      }

      .footer-phone {
        font-size: 13px;
        line-height: 20px;
        font-weight: 600;
        color: rgba(255, 255, 255, 0.85);
      }
    }
  }

  .header {
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: auto;
    padding: 18px 20px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 28px;
      font-weight: 700;
      line-height: 1.3;
      color: #001529;

      .header-logo {
        width: 30px;
        height: 30px;
        flex: none;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 20px;

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
