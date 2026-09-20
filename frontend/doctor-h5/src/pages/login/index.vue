<template>
  <view class="login">
    <view class="login-hero">
      <view class="hero-logo">
        <image class="hero-logo-img" src="/static/logo.png" mode="aspectFit" />
      </view>
      <view class="hero-title">Careld 儿童视力养护诊约助手</view>
      <view class="hero-sub">医生工作端 · 移动终端</view>
    </view>

    <view class="login-card">
      <view class="card-head">账号登录</view>

      <view class="input-wrap">
        <text class="input-icon">📱</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          placeholder-class="ph"
          :adjust-position="true"
        />
      </view>

      <view class="input-wrap">
        <text class="input-icon">🔒</text>
        <input
          v-model="form.password"
          class="input"
          :password="!showPassword"
          placeholder="请输入登录密码"
          placeholder-class="ph"
          @confirm="handleLogin"
        />
        <text class="pwd-toggle" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </text>
      </view>

      <view class="login-btn" :class="{ 'is-disabled': !canSubmit || loading }" @click="handleLogin">
        {{ loading ? '登录中…' : '登 录' }}
      </view>

      <view class="login-tip">
        登录账号为本人手机号，密码由管理员在医院端「医务人员管理」中设置
      </view>
    </view>

    <view class="login-foot">Careld Vision System · 医生端</view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'
import { toast } from '@/utils/request'

const userStore = useUserStore()
const form = reactive({ phone: '', password: '' })
const showPassword = ref(false)
const loading = ref(false)

const canSubmit = computed(() => /^1[3-9]\d{9}$/.test(form.phone) && form.password.length >= 6)

onLoad(() => {
  // 已登录直接进工作台
  if (getToken()) {
    uni.switchTab({ url: '/pages/home/index' })
  }
})

async function handleLogin() {
  if (loading.value) return
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    toast('请输入正确的11位手机号')
    return
  }
  if (!form.password) {
    toast('请输入登录密码')
    return
  }
  loading.value = true
  try {
    await userStore.loginByPassword(form.phone, form.password)
    toast('登录成功')
    setTimeout(() => {
      uni.switchTab({ url: '/pages/home/index' })
    }, 300)
  } catch {
    // 错误提示已在请求层处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  background: linear-gradient(180deg, #2563eb 0%, #1d4ed8 42%, #f4f6f9 42%, #f4f6f9 100%);
  padding: 0 48rpx;
}

.login-hero {
  padding: 180rpx 0 96rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-logo {
  position: relative;
  width: 132rpx;
  height: 132rpx;
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}

.hero-logo-img {
  width: 88rpx;
  height: 88rpx;
}

.hero-title {
  font-size: 44rpx;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2rpx;
}

.hero-sub {
  margin-top: 14rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.82);
}

.login-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 40rpx 36rpx 48rpx;
  box-shadow: 0 16rpx 48rpx rgba(15, 23, 42, 0.1);
}

.card-head {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 40rpx;
}

.input-wrap {
  display: flex;
  align-items: center;
  height: 100rpx;
  background: #f5f7fa;
  border-radius: 16rpx;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
}

.input-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
}

.input {
  flex: 1;
  min-width: 0;
  font-size: 30rpx;
  color: #1f2937;
  height: 100%;
}

.ph {
  color: #b7bfc9;
  font-size: 30rpx;
}

.pwd-toggle {
  font-size: 26rpx;
  color: #2563eb;
  padding-left: 16rpx;
}

.login-btn {
  margin-top: 40rpx;
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  letter-spacing: 4rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 24rpx rgba(37, 99, 235, 0.32);
}

.login-btn.is-disabled {
  background: #dfe5ea;
  color: #a9b1bd;
  box-shadow: none;
}

.login-tip {
  margin-top: 32rpx;
  font-size: 23rpx;
  line-height: 1.6;
  color: #9ca3af;
}

.login-foot {
  text-align: center;
  padding: 80rpx 0 40rpx;
  font-size: 22rpx;
  color: #b7bfc9;
}
</style>
