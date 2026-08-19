<template>
  <div class="login-page">
    <div class="login-box">
      <h2>医院管理系统登录</h2>
      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-radio-group v-model="form.loginType" size="large" class="login-type">
            <el-radio-button :value="1">用户名登录</el-radio-button>
            <el-radio-button :value="2">手机号登录</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.account"
            :placeholder="form.loginType === 1 ? '用户名' : '手机号'"
            size="large"
            :prefix-icon="User"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            :prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreLogin' })
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  account: '',
  password: '',
  loginType: 2
})

const handleLogin = async () => {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const loginData = {
      username: form.loginType === 1 ? form.account : undefined,
      phone: form.loginType === 2 ? form.account : undefined,
      password: form.password,
      loginType: form.loginType
    }
    await userStore.login(loginData)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    router.push('/dashboard')
  }
})
</script>

<style scoped lang="scss">
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .login-box {
    width: 400px;
    padding: 40px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);

    h2 {
      text-align: center;
      margin-bottom: 30px;
      color: #333;
    }

    .login-btn {
      width: 100%;
    }
  }
}
</style>
