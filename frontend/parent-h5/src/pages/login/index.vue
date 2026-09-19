<template>
  <view class="login">
    <view class="login-hero">
      <view class="hero-logo">
        <view class="logo-ring"></view>
        <view class="logo-dot"></view>
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
        <text class="input-icon">🔑</text>
        <input
          v-model="form.code"
          class="input"
          type="number"
          maxlength="6"
          placeholder="请输入短信验证码"
          placeholder-class="ph"
          @confirm="handleLogin"
        />
        <text
          class="code-btn"
          :class="{ 'code-btn-disabled': countdown > 0 || sending }"
          @click="handleSendCode"
        >
          {{ countdown > 0 ? `${countdown}s 后重发` : sending ? '发送中…' : '获取验证码' }}
        </text>
      </view>

      <view class="login-btn" :class="{ 'is-disabled': !canSubmit || loading }" @click="handleLogin">
        {{ loading ? '登录中…' : '登 录' }}
      </view>

      <view class="login-tip">
        未注册的手机号将自动创建家长账号；登录后请先完善儿童档案，待医院审核通过即可预约养护。
      </view>
    </view>

    <view class="login-foot">Careld Vision System · 家长端</view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'
import { toast } from '@/utils/request'

const userStore = useUserStore()
const form = reactive({ phone: '', code: '' })
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const canSubmit = computed(() => /^1[3-9]\d{9}$/.test(form.phone) && form.code.length >= 4)

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

onLoad(() => {
  if (getToken()) {
    uni.switchTab({ url: '/pages/appointment/list' })
  }
})

onUnload(() => {
  if (timer) clearInterval(timer)
})

async function handleSendCode() {
  if (countdown.value > 0 || sending.value) return
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    toast('请输入正确的11位手机号')
    return
  }
  sending.value = true
  try {
    await userStore.sendSms(form.phone)
    toast('验证码已发送，请注意查收')
    startCountdown()
  } catch {
    // 错误提示已在请求层处理（含频控提示）
  } finally {
    sending.value = false
  }
}

async function handleLogin() {
  if (loading.value) return
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    toast('请输入正确的11位手机号')
    return
  }
  if (!form.code) {
    toast('请输入短信验证码')
    return
  }
  loading.value = true
  try {
    await userStore.loginBySms(form.phone, form.code)
    // 登录后自动认领同手机号未绑定档案（含默认名回填家长姓名）
    await userStore.claimMyChildren()
    toast('登录成功')
    setTimeout(() => {
      uni.switchTab({ url: '/pages/appointment/list' })
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

.logo-ring {
  width: 62rpx;
  height: 62rpx;
  border: 8rpx solid #fff;
  border-radius: 50%;
}

.logo-dot {
  position: absolute;
  width: 20rpx;
  height: 20rpx;
  border-radius: 50%;
  background: #fff;
  right: 34rpx;
  bottom: 34rpx;
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

.code-btn {
  flex: none;
  padding-left: 20rpx;
  font-size: 26rpx;
  color: #14b8a6;
}

.code-btn-disabled {
  color: #b7bfc9;
}

.login-btn {
  margin-top: 40rpx;
  height: 96rpx;
  border-radius: 48rpx;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
  letter-spacing: 4rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 24rpx rgba(20, 184, 166, 0.32);
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
