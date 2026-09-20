<template>
  <view class="login">
    <view class="login-hero">
      <view class="hero-logo">
        <image class="hero-logo-img" src="/static/logo.png" mode="aspectFit" />
      </view>
      <view class="hero-title">Careld 儿童视力养护</view>
      <view class="hero-sub">家长服务端 · 查档案 · 约养护</view>
    </view>

    <view class="login-card">
      <view class="card-head">手机号登录</view>

      <view class="input-wrap">
        <text class="input-icon">📱</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          maxlength="11"
          placeholder="请输入手机号"
          placeholder-class="ph"
        />
      </view>

      <view class="input-wrap">
        <text class="input-icon">🔒</text>
        <input
          v-model="form.password"
          class="input"
          :password="!showPassword"
          maxlength="20"
          placeholder="请输入登录密码"
          placeholder-class="ph"
          @confirm="handleLogin"
        />
        <text class="pwd-toggle" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </text>
      </view>

      <view class="login-actions">
        <view class="btn btn-ghost" @click="goRegister">注册</view>
        <view class="btn btn-primary" :class="{ 'is-disabled': !canSubmit || loading }" @click="handleLogin">
          {{ loading ? '登录中…' : '登 录' }}
        </view>
      </view>

      <view class="forgot-row">
        <text class="forgot-btn" @click="goForgot">忘记密码？</text>
      </view>
    </view>

    <view class="login-foot">Careld Vision System · 家长端</view>
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
  if (getToken()) {
    uni.switchTab({ url: '/pages/appointment/list' })
  }
})

function goRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}

function goForgot() {
  uni.navigateTo({ url: '/pages/login/forgot' })
}

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
    // 登录后自动认领同手机号未绑定档案（含默认名回填家长姓名）
    await userStore.claimMyChildren()
    toast('登录成功')
    setTimeout(() => {
      uni.switchTab({ url: '/pages/appointment/list' })
    }, 300)
  } catch {
    // 未注册/其它身份等错误提示已由请求层弹出
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login {
  min-height: 100vh;
  background: linear-gradient(180deg, #14b8a6 0%, #0d9488 42%, #f4f6f9 42%, #f4f6f9 100%);
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
  padding: 40rpx 36rpx 40rpx;
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
  flex: none;
  padding-left: 20rpx;
  font-size: 26rpx;
  color: #94a3b8;
}

.login-actions {
  display: flex;
  align-items: center;
  margin-top: 40rpx;
}

.btn {
  flex: 1;
  height: 96rpx;
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-ghost {
  margin-right: 24rpx;
  color: #14b8a6;
  background: #fff;
  border: 2rpx solid #14b8a6;
  letter-spacing: 4rpx;
}

.btn-primary {
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
  letter-spacing: 4rpx;
  box-shadow: 0 10rpx 24rpx rgba(20, 184, 166, 0.32);
}

.btn-primary.is-disabled {
  background: #dfe5ea;
  color: #a9b1bd;
  box-shadow: none;
}

.forgot-row {
  margin-top: 28rpx;
  display: flex;
  justify-content: flex-end;
}

.forgot-btn {
  font-size: 26rpx;
  color: #14b8a6;
}

.login-foot {
  text-align: center;
  padding: 80rpx 0 40rpx;
  font-size: 22rpx;
  color: #b7bfc9;
}
</style>
