<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-left">
        <div class="brand">
          <img src="/src/assets/logo.svg" alt="Careld" class="brand-logo">
          <h1 class="brand-name">Careld</h1>
          <p class="brand-slogan">可尔欧得视力养护服务系统</p>
        </div>
        <div class="features">
          <div class="feature-item">
            <el-icon :size="24" color="#1890ff"><Monitor /></el-icon>
            <span>智能视力检测</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#52c41a"><DataAnalysis /></el-icon>
            <span>数据可视化管理</span>
          </div>
          <div class="feature-item">
            <el-icon :size="24" color="#faad14"><Connection /></el-icon>
            <span>多端协同办公</span>
          </div>
        </div>
      </div>
      <div class="login-right">
        <div class="login-form-wrapper">
          <h2 class="form-title">运营中心登录</h2>
          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="formData.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="formData.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item prop="captcha" v-if="showCaptcha">
              <div class="captcha-row">
                <el-input
                  v-model="formData.captcha"
                  placeholder="请输入验证码"
                  size="large"
                  :prefix-icon="Key"
                />
                <div class="captcha-image" @click="refreshCaptcha">
                  <img :src="captchaImage" alt="验证码" v-if="captchaImage">
                </div>
              </div>
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
          <div class="login-tips">
            <p>提示：运营中心账号由系统管理员分配</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminLogin' })
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, Monitor, DataAnalysis, Connection } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { authApi } from '@/api'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showCaptcha = ref(false)
const captchaImage = ref('')
const captchaKey = ref('')

const formData = reactive({
  username: '',
  password: '',
  captcha: '',
  loginType: 1 // 运营中心登录
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  captcha: [
    { required: showCaptcha.value, message: '请输入验证码', trigger: 'blur' }
  ]
}

const refreshCaptcha = async () => {
  try {
    const res = await authApi.getCaptcha()
    captchaKey.value = res.captchaKey
    captchaImage.value = res.captchaImage
  } catch (error) {
    console.error('获取验证码失败', error)
  }
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.login({
          ...formData,
          captchaKey: captchaKey.value
        })
        ElMessage.success('登录成功')
        router.push('/dashboard')
      } catch (error) {
        const err = error as { code?: number; message?: string }
        if (err.code === 1003) {
          showCaptcha.value = true
          await refreshCaptcha()
        }
        ElMessage.error(err.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    router.push('/dashboard')
  }
})
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  width: 900px;
  height: 500px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  overflow: hidden;
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 60px 40px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .brand {
    margin-bottom: 60px;

    .brand-logo {
      width: 80px;
      height: 80px;
      margin-bottom: 20px;
    }

    .brand-name {
      font-size: 36px;
      font-weight: bold;
      margin-bottom: 10px;
    }

    .brand-slogan {
      font-size: 16px;
      opacity: 0.9;
    }
  }

  .features {
    .feature-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
      font-size: 14px;
    }
  }
}

.login-right {
  width: 400px;
  padding: 60px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;

  .form-title {
    font-size: 24px;
    font-weight: bold;
    color: #333;
    margin-bottom: 30px;
    text-align: center;
  }

  .login-form {
    .captcha-row {
      display: flex;
      gap: 12px;

      .el-input {
        flex: 1;
      }

      .captcha-image {
        width: 120px;
        height: 40px;
        background: #f5f5f5;
        border-radius: 4px;
        cursor: pointer;
        overflow: hidden;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
    }

    .login-btn {
      width: 100%;
    }
  }

  .login-tips {
    margin-top: 20px;
    text-align: center;
    color: #999;
    font-size: 12px;
  }
}
</style>
