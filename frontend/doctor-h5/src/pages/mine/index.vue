<template>
  <view class="page">
    <view class="hero">
      <view class="avatar">{{ avatarText }}</view>
      <view class="hero-info">
        <text class="hero-name">{{ displayName }}</text>
        <text class="hero-role">{{ roleText }}</text>
      </view>
    </view>

    <view class="card">
      <view class="kv"><text class="kv-key">账号</text><text class="kv-value">{{ user?.username || '—' }}</text></view>
      <view class="kv kv-clickable" @click="openPasswordEdit">
        <text class="kv-key">修改密码</text>
        <text class="kv-value kv-blue">去修改<text class="kv-arrow">›</text></text>
      </view>
      <view class="kv"><text class="kv-key">姓名</text><text class="kv-value">{{ user?.realName || '—' }}</text></view>
      <view class="kv kv-clickable" @click="openPhoneEdit">
        <text class="kv-key">手机号码</text>
        <text class="kv-value">{{ user?.phone || '—' }}<text class="kv-arrow">›</text></text>
      </view>
      <view class="kv"><text class="kv-key">身份</text><text class="kv-value">{{ roleText }}</text></view>
      <view class="kv"><text class="kv-key">所在医院</text><text class="kv-value">{{ user?.storeName || '—' }}</text></view>
    </view>

    <view class="card">
      <view class="entry" @click="showAbout = true">
        <text class="entry-label">关于</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry entry-last" @click="handleLogout">
        <text class="entry-label entry-danger">退出登录</text>
        <text class="entry-arrow">›</text>
      </view>
    </view>

    <view class="version">Careld 诊约助手医生端 V2.0.1</view>

    <view v-if="showAbout" class="mask" @click="showAbout = false">
      <view class="sheet" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">关于</text>
          <text class="sheet-close" @click="showAbout = false">✕</text>
        </view>
        <view class="sheet-body">
          <view class="about-line">Careld 儿童视力养护管理系统 · 医生端</view>
          <view class="about-line about-sub">版本 v2.0.1（H5 / uni-app）</view>
          <view class="about-line about-sub">登录密码可在「我的」页面自助修改。</view>
          <view class="about-line about-sub about-phone">系统服务支持电话：400-999-3608</view>
        </view>
        <view class="sheet-footer">
          <view class="btn btn-primary btn-block" @click="showAbout = false">知道了</view>
        </view>
      </view>
    </view>

    <view v-if="phoneVisible" class="mask" @click="phoneVisible = false">
      <view class="sheet" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">修改手机号</text>
          <text class="sheet-close" @click="phoneVisible = false">✕</text>
        </view>
        <view class="sheet-body">
          <view class="edit-tip">手机号即登录账号，修改成功后需使用新手机号重新登录。</view>
          <input
            v-model="phoneForm.phone"
            class="edit-input"
            type="number"
            maxlength="11"
            placeholder="请输入新手机号"
            placeholder-class="edit-placeholder"
          />
        </view>
        <view class="sheet-footer">
          <view class="btn btn-plain" @click="phoneVisible = false">取消</view>
          <view class="btn btn-primary" :class="{ 'is-disabled': phoneSubmitting }" @click="submitPhone">
            {{ phoneSubmitting ? '提交中…' : '确认修改' }}
          </view>
        </view>
      </view>
    </view>

    <view v-if="pwdVisible" class="mask" @click="pwdVisible = false">
      <view class="sheet" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">修改密码</text>
          <text class="sheet-close" @click="pwdVisible = false">✕</text>
        </view>
        <view class="sheet-body">
          <input
            v-model="pwdForm.oldPassword"
            class="edit-input"
            password
            placeholder="请输入旧密码"
            placeholder-class="edit-placeholder"
          />
          <input
            v-model="pwdForm.newPassword"
            class="edit-input"
            password
            placeholder="请输入新密码（至少 6 位）"
            placeholder-class="edit-placeholder"
          />
          <input
            v-model="pwdForm.confirmPassword"
            class="edit-input"
            password
            placeholder="请再次输入新密码"
            placeholder-class="edit-placeholder"
          />
        </view>
        <view class="sheet-footer">
          <view class="btn btn-plain" @click="pwdVisible = false">取消</view>
          <view class="btn btn-primary" :class="{ 'is-disabled': pwdSubmitting }" @click="submitPassword">
            {{ pwdSubmitting ? '提交中…' : '确认修改' }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { userApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { userRoleText } from '@/utils/dict'
import { toast } from '@/utils/request'

const userStore = useUserStore()
const showAbout = ref(false)

const phoneVisible = ref(false)
const phoneSubmitting = ref(false)
const phoneForm = ref({ phone: '' })

const pwdVisible = ref(false)
const pwdSubmitting = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const user = computed(() => userStore.userInfo)

const displayName = computed(() => userStore.displayName)

const roleText = computed(() => userRoleText(user.value?.userType, user.value?.staffRole))

const avatarText = computed(() => {
  const name = displayName.value
  return name ? name.slice(0, 1) : '医'
})

function openPhoneEdit() {
  phoneForm.value.phone = user.value?.phone || ''
  phoneVisible.value = true
}

async function submitPhone() {
  const phone = phoneForm.value.phone.trim()
  if (!/^1\d{10}$/.test(phone)) {
    toast('请输入正确的 11 位手机号')
    return
  }
  if (phone === user.value?.phone) {
    toast('新手机号与当前手机号相同')
    return
  }
  phoneSubmitting.value = true
  try {
    await userApi.changePhone({ phone })
    phoneVisible.value = false
    toast('手机号修改成功，请重新登录')
    setTimeout(async () => {
      await userStore.logout()
      uni.reLaunch({ url: '/pages/login/index' })
    }, 1200)
  } catch {
    // 错误提示已由请求层弹出
  } finally {
    phoneSubmitting.value = false
  }
}

function openPasswordEdit() {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdVisible.value = true
}

async function submitPassword() {
  const { oldPassword, newPassword, confirmPassword } = pwdForm.value
  if (!oldPassword) {
    toast('请输入旧密码')
    return
  }
  if (!newPassword || newPassword.length < 6) {
    toast('新密码至少 6 位')
    return
  }
  if (newPassword !== confirmPassword) {
    toast('两次输入的新密码不一致')
    return
  }
  pwdSubmitting.value = true
  try {
    await userApi.changePassword({ oldPassword, newPassword })
    pwdVisible.value = false
    toast('密码修改成功')
  } catch {
    // 错误提示已由请求层弹出（如原密码错误）
  } finally {
    pwdSubmitting.value = false
  }
}

function handleLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确认退出当前账号吗？',
    confirmText: '退出',
    success: async (res) => {
      if (!res.confirm) return
      await userStore.logout()
      uni.reLaunch({ url: '/pages/login/index' })
    },
  })
}

onShow(() => {
  userStore.fetchProfile()
})
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 60rpx;
}

.hero {
  display: flex;
  align-items: center;
  padding: 56rpx 40rpx 64rpx;
  background: linear-gradient(135deg, #60a5fa, #2563eb, #1d4ed8);
}

.avatar {
  width: 128rpx;
  height: 128rpx;
  flex: none;
  line-height: 128rpx;
  text-align: center;
  font-size: 52rpx;
  font-weight: 600;
  color: #1d4ed8;
  background: #fff;
  border-radius: 50%;
}

.hero-info {
  display: flex;
  flex-direction: column;
  margin-left: 28rpx;
  min-width: 0;
}

.hero-name {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
}

.hero-role {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.card {
  margin: 20rpx 24rpx;
}

.kv {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
  font-size: 28rpx;
}

.kv-key {
  width: 180rpx;
  flex: none;
  color: #94a3b8;
}

.kv-value {
  flex: 1;
  text-align: right;
  color: #1e293b;
}

.kv-clickable:active {
  opacity: 0.6;
}

.kv-blue {
  color: #2563eb;
}

.kv-arrow {
  margin-left: 8rpx;
  font-size: 32rpx;
  color: #cbd5e1;
}

.edit-tip {
  padding-bottom: 16rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #94a3b8;
}

.edit-input {
  height: 88rpx;
  margin-bottom: 16rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
  color: #1e293b;
  background: #f8fafc;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.edit-input:last-child {
  margin-bottom: 0;
}

.edit-placeholder {
  color: #cbd5e1;
}

.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
}

.entry-last {
  border-bottom: none;
}

.entry-label {
  font-size: 29rpx;
  color: #1e293b;
}

.entry-danger {
  color: #ef4444;
}

.entry-arrow {
  font-size: 34rpx;
  color: #cbd5e1;
}

.version {
  margin-top: 48rpx;
  text-align: center;
  font-size: 24rpx;
  color: #cbd5e1;
}

.about-line {
  font-size: 28rpx;
  color: #1e293b;
  line-height: 1.8;
}

.about-sub {
  font-size: 25rpx;
  color: #94a3b8;
}

.about-phone {
  margin-top: 16rpx;
}
</style>
