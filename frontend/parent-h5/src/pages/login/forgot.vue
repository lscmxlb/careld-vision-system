<template>
  <view class="page">
    <view class="card">
      <view class="card-head">重置登录密码</view>
      <view class="card-sub">输入注册时的手机号，验证短信验证码后即可设置新密码</view>

      <view class="input-wrap">
        <text class="input-icon">📱</text>
        <input
          v-model="form.phone"
          class="input"
          type="number"
          maxlength="11"
          placeholder="请输入手机号码"
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
        />
        <text
          class="code-btn"
          :class="{ 'code-btn-disabled': countdown > 0 || sending }"
          @click="handleSendCode"
        >
          {{ countdown > 0 ? `${countdown}s 后重发` : sending ? '发送中…' : '获取验证码' }}
        </text>
      </view>

      <view class="input-wrap">
        <text class="input-icon">🔒</text>
        <input
          v-model="form.newPassword"
          class="input"
          :password="!showPassword"
          maxlength="20"
          placeholder="请设置新密码（6~20位）"
          placeholder-class="ph"
        />
        <text class="pwd-toggle" @click="showPassword = !showPassword">
          {{ showPassword ? '隐藏' : '显示' }}
        </text>
      </view>

      <view class="input-wrap">
        <text class="input-icon">🔒</text>
        <input
          v-model="form.confirmPassword"
          class="input"
          :password="!showPassword"
          maxlength="20"
          placeholder="请再次输入新密码"
          placeholder-class="ph"
          @confirm="handleSubmit"
        />
      </view>

      <view v-if="pwdMatchText" class="pwd-compare" :class="pwdMatchClass">{{ pwdMatchText }}</view>

      <view class="submit-btn" :class="{ 'is-disabled': !canSubmit || loading }" @click="handleSubmit">
        {{ loading ? '提交中…' : '确认重置' }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onUnload } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/request'

const userStore = useUserStore()
const form = reactive({ phone: '', code: '', newPassword: '', confirmPassword: '' })
const showPassword = ref(false)
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const pwdMismatch = computed(
  () => !!form.confirmPassword && form.confirmPassword !== form.newPassword,
)
const pwdMatchText = computed(() => {
  if (!form.confirmPassword) return ''
  return pwdMismatch.value ? '两次输入的密码不一致' : '两次输入的密码一致'
})
const pwdMatchClass = computed(() => (pwdMismatch.value ? 'is-bad' : 'is-ok'))

const canSubmit = computed(
  () =>
    /^1[3-9]\d{9}$/.test(form.phone) &&
    form.code.length >= 4 &&
    form.newPassword.length >= 6 &&
    form.confirmPassword === form.newPassword,
)

onUnload(() => {
  if (timer) clearInterval(timer)
})

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

async function handleSubmit() {
  if (loading.value) return
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    toast('请输入正确的11位手机号')
    return
  }
  if (!form.code) {
    toast('请输入短信验证码')
    return
  }
  if (form.newPassword.length < 6) {
    toast('密码长度需为6~20位')
    return
  }
  if (pwdMismatch.value || !form.confirmPassword) {
    toast('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await userStore.resetPassword({
      phone: form.phone,
      code: form.code,
      newPassword: form.newPassword,
    })
    toast('密码重置成功，请使用新密码登录')
    setTimeout(() => {
      uni.navigateBack()
    }, 800)
  } catch {
    // 未注册等错误提示已在请求层处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #f4f6f9;
  padding: 40rpx 48rpx;
}

.card {
  background: #fff;
  border-radius: 28rpx;
  padding: 40rpx 36rpx;
  box-shadow: 0 16rpx 48rpx rgba(15, 23, 42, 0.08);
}

.card-head {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2937;
}

.card-sub {
  margin: 14rpx 0 36rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #9ca3af;
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

.pwd-toggle {
  flex: none;
  padding-left: 20rpx;
  font-size: 26rpx;
  color: #94a3b8;
}

.pwd-compare {
  margin-top: -8rpx;
  margin-bottom: 8rpx;
  font-size: 24rpx;
}

.pwd-compare.is-bad {
  color: #ef4444;
}

.pwd-compare.is-ok {
  color: #16a34a;
}

.submit-btn {
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

.submit-btn.is-disabled {
  background: #dfe5ea;
  color: #a9b1bd;
  box-shadow: none;
}
</style>
