<template>
  <div class="login-page">
    <div class="login-header">
      <h1>Careld</h1>
      <p>可尔欧得视力养护</p>
    </div>

    <div class="login-form">
      <h2>家长登录</h2>

      <!-- 登录方式切换 -->
      <div class="login-tabs">
        <span
          :class="{ active: loginMode === 'password' }"
          @click="loginMode = 'password'"
        >密码登录</span>
        <span
          :class="{ active: loginMode === 'sms' }"
          @click="loginMode = 'sms'"
        >验证码登录</span>
      </div>

      <el-form>
        <el-form-item>
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
            size="large"
            maxlength="11"
            clearable
          >
            <template #prefix>
              <el-icon><Phone /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 密码登录 -->
        <el-form-item v-if="loginMode === 'password'">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <!-- 验证码登录 -->
        <el-form-item v-else>
          <el-input
            v-model="form.code"
            placeholder="请输入验证码"
            size="large"
            maxlength="6"
          >
            <template #prefix>
              <el-icon><Message /></el-icon>
            </template>
            <template #append>
              <el-button
                :disabled="countdown > 0"
                @click="sendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </template>
          </el-input>
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
defineOptions({ name: 'ParentLogin' })
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Phone, Lock, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const countdown = ref(0)
const loginMode = ref<'password' | 'sms'>('password')

const form = reactive({
  phone: '',
  password: '',
  code: ''
})

const sendCode = () => {
  if (!form.phone || form.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  // TODO: 调用发送验证码API
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
    }
  }, 1000)
  ElMessage.success('验证码已发送')
}

const handleLogin = async () => {
  if (!form.phone || form.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  if (loginMode.value === 'password' && !form.password) {
    ElMessage.warning('请输入密码')
    return
  }

  if (loginMode.value === 'sms' && !form.code) {
    ElMessage.warning('请输入验证码')
    return
  }

  loading.value = true
  try {
    await userStore.login(form.phone, form.password || form.code)
    ElMessage.success('登录成功')
    router.push('/profile')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败，请检查手机号和密码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    router.push('/profile')
  }
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 20px;

  .login-header {
    text-align: center;
    color: #fff;
    margin-bottom: 40px;

    h1 {
      font-size: 36px;
      margin-bottom: 10px;
      letter-spacing: 2px;
    }

    p {
      font-size: 16px;
      opacity: 0.9;
    }
  }

  .login-form {
    background: #fff;
    border-radius: 16px;
    padding: 30px 24px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

    h2 {
      text-align: center;
      margin-bottom: 20px;
      color: #333;
      font-size: 20px;
    }

    .login-tabs {
      display: flex;
      justify-content: center;
      gap: 24px;
      margin-bottom: 24px;

      span {
        font-size: 14px;
        color: #999;
        cursor: pointer;
        padding-bottom: 8px;
        border-bottom: 2px solid transparent;
        transition: all 0.3s;

        &.active {
          color: #1890ff;
          border-bottom-color: #1890ff;
        }
      }
    }

    .login-btn {
      width: 100%;
      margin-top: 8px;
    }
  }
}
</style>
