<template>
  <div class="mine-page">
    <!-- 用户信息卡片 -->
    <div class="user-card">
      <div class="user-avatar">
        <el-avatar :size="64" :icon="UserFilled" />
      </div>
      <div class="user-info">
        <h3>{{ userStore.userInfo?.realName || '家长' }}</h3>
        <p class="phone">{{ maskPhone(userStore.userInfo?.phone || '') }}</p>
      </div>
    </div>

    <!-- 数据统计 -->
    <div class="stats-row">
      <div class="stat-item" @click="$router.push('/profile')">
        <span class="stat-value">{{ childrenCount }}</span>
        <span class="stat-label">我的孩子</span>
      </div>
      <div class="stat-item" @click="$router.push('/appointment/list')">
        <span class="stat-value">{{ reserveCount }}</span>
        <span class="stat-label">我的预约</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ visitCount }}</span>
        <span class="stat-label">养护次数</span>
      </div>
    </div>

    <!-- 菜单列表 -->
    <div class="menu-card">
      <div class="menu-item" @click="$router.push('/appointment/list')">
        <el-icon :size="20" color="#1890ff"><Calendar /></el-icon>
        <span>我的预约</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="$router.push('/profile')">
        <el-icon :size="20" color="#52c41a"><User /></el-icon>
        <span>我的孩子</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="handleChangePassword">
        <el-icon :size="20" color="#faad14"><Lock /></el-icon>
        <span>修改密码</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
      <div class="menu-item" @click="showAbout = true">
        <el-icon :size="20" color="#722ed1"><InfoFilled /></el-icon>
        <span>关于我们</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="logout-section">
      <el-button type="danger" plain size="large" class="logout-btn" @click="handleLogout">
        退出登录
      </el-button>
    </div>

    <!-- 关于我们弹窗 -->
    <el-dialog v-model="showAbout" title="关于我们" width="85%">
      <div class="about-content">
        <h4>Careld 可尔欧得视力养护</h4>
        <p>专业的儿童青少年视力养护服务平台</p>
        <p>版本: v1.0.0</p>
        <el-divider />
        <p class="copyright">© 2024 Careld Vision Care. All rights reserved.</p>
      </div>
    </el-dialog>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPassword" title="修改密码" width="85%">
      <el-form :model="passwordForm" label-width="70px">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPassword = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ParentMine' })
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UserFilled, Calendar, User, Lock, InfoFilled, ArrowRight
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { childApi, reserveApi } from '@/api'

const router = useRouter()
const userStore = useUserStore()

// 统计
const childrenCount = ref(0)
const reserveCount = ref(0)
const visitCount = ref(0)

// 弹窗
const showAbout = ref(false)
const showPassword = ref(false)
const passwordLoading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 手机号脱敏
const maskPhone = (phone: string) => {
  if (!phone || phone.length < 11) return phone
  return phone.substring(0, 3) + '****' + phone.substring(7)
}

// 修改密码
const handleChangePassword = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showPassword.value = true
}

const submitPassword = async () => {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入旧密码')
    return
  }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }

  passwordLoading.value = true
  try {
    // TODO: 调用修改密码API
    ElMessage.success('密码修改成功')
    showPassword.value = false
  } catch {
    ElMessage.error('密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await userStore.logout()
    router.push('/login')
  } catch {
    // 用户取消
  }
}

// 加载统计
const fetchStats = async () => {
  try {
    const [children, reserves] = await Promise.all([
      childApi.getMyChildren(),
      reserveApi.getMyReservations()
    ])
    childrenCount.value = children.length
    reserveCount.value = reserves.filter(r => r.status === 1 || r.status === 3).length
    visitCount.value = reserves.filter(r => r.status === 4).length
  } catch {
    // 静默失败
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped lang="scss">
.mine-page {
  padding: 16px;

  .user-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    padding: 24px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 16px;

    .user-avatar {
      .el-avatar {
        border: 3px solid rgba(255, 255, 255, 0.3);
      }
    }

    .user-info {
      h3 {
        color: #fff;
        font-size: 20px;
        margin-bottom: 4px;
      }

      .phone {
        color: rgba(255, 255, 255, 0.8);
        font-size: 14px;
      }
    }
  }

  .stats-row {
    background: #fff;
    border-radius: 12px;
    padding: 20px 0;
    display: flex;
    justify-content: space-around;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .stat-item {
      text-align: center;
      cursor: pointer;

      .stat-value {
        display: block;
        font-size: 24px;
        font-weight: bold;
        color: #1890ff;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .menu-card {
    background: #fff;
    border-radius: 12px;
    overflow: hidden;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .menu-item {
      display: flex;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid #f5f5f5;
      cursor: pointer;
      transition: background 0.2s;

      &:last-child {
        border-bottom: none;
      }

      &:active {
        background: #f5f5f5;
      }

      span {
        flex: 1;
        margin-left: 12px;
        font-size: 15px;
        color: #333;
      }

      .arrow {
        color: #ccc;
      }
    }
  }

  .logout-section {
    margin-top: 24px;

    .logout-btn {
      width: 100%;
    }
  }

  .about-content {
    text-align: center;
    padding: 20px 0;

    h4 {
      font-size: 18px;
      margin-bottom: 8px;
    }

    p {
      color: #666;
      font-size: 14px;
      margin-bottom: 8px;
    }

    .copyright {
      font-size: 12px;
      color: #999;
    }
  }
}
</style>
