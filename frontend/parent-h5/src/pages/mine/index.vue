<template>
  <view class="page">
    <view class="hero">
      <view class="avatar">{{ avatarText }}</view>
      <view class="hero-info">
        <text class="hero-name">{{ displayName }}</text>
        <text class="hero-role">家长账号</text>
      </view>
    </view>

    <view class="card">
      <view class="kv"><text class="kv-key">手机号码</text><text class="kv-value">{{ userStore.phoneMask }}</text></view>
      <view class="kv"><text class="kv-key">绑定孩子</text><text class="kv-value">{{ childCount }} 个</text></view>
      <view class="kv"><text class="kv-key">剩余可用次数</text><text class="kv-value">{{ totalRemaining }} 次</text></view>
    </view>

    <view class="card">
      <view class="entry" @click="goMyReserve">
        <text class="entry-label">我的预约</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goCareRecord">
        <text class="entry-label">养护记录</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry" @click="goTrend">
        <text class="entry-label">视力趋势</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry entry-last" @click="goReport">
        <text class="entry-label">检测报告</text>
        <text class="entry-arrow">›</text>
      </view>
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

    <view class="version">Careld 家长端 v2.0.1</view>

    <view v-if="showAbout" class="mask" @click="showAbout = false">
      <view class="sheet" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">关于</text>
          <text class="sheet-close" @click="showAbout = false">✕</text>
        </view>
        <view class="sheet-body">
          <view class="about-line">Careld 儿童视力养护管理系统 · 家长端</view>
          <view class="about-line about-sub">版本 v2.0.1（H5 / uni-app）</view>
          <view class="about-line about-sub">账号为手机号验证码登录，无需设置密码；如有疑问请联系医院前台。</view>
        </view>
        <view class="sheet-footer">
          <view class="btn btn-primary btn-block" @click="showAbout = false">知道了</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const showAbout = ref(false)
const childCount = ref(0)
const totalRemaining = ref(0)

const displayName = computed(() =>
  userStore.userInfo?.realName || (userStore.phone ? `家长 ${userStore.phoneMask}` : '家长'),
)

const avatarText = computed(() => displayName.value.slice(0, 1))

function goMyReserve() {
  uni.switchTab({ url: '/pages/appointment/list' })
}

function goCareRecord() {
  uni.switchTab({ url: '/pages/care-record/index' })
}

function goTrend() {
  uni.navigateTo({ url: '/pages/trend/index' })
}

function goReport() {
  uni.navigateTo({ url: '/pages/report/index' })
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

async function loadChildren() {
  try {
    const res = await childApi.getChildList({ parentUserId: userStore.userId })
    childCount.value = (res || []).length
    totalRemaining.value = (res || []).reduce((sum, c) => sum + (c.remainingCount ?? 0), 0)
  } catch {
    childCount.value = 0
    totalRemaining.value = 0
  }
}

onShow(async () => {
  await userStore.fetchProfile()
  loadChildren()
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
  background: linear-gradient(135deg, #2dd4bf, #14b8a6, #0d9488);
}

.avatar {
  width: 112rpx;
  height: 112rpx;
  flex: none;
  line-height: 112rpx;
  text-align: center;
  font-size: 44rpx;
  color: #0d9488;
  background: #fff;
  border-radius: 50%;
  font-weight: 600;
}

.hero-info {
  margin-left: 28rpx;
  display: flex;
  flex-direction: column;
}

.hero-name {
  font-size: 38rpx;
  font-weight: 700;
  color: #fff;
}

.hero-role {
  margin-top: 10rpx;
  font-size: 25rpx;
  color: rgba(255, 255, 255, 0.85);
}

.kv {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  font-size: 28rpx;
}

.kv-key {
  width: 220rpx;
  flex: none;
  color: #94a3b8;
}

.kv-value {
  flex: 1;
  color: #1e293b;
  text-align: right;
}

.entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0;
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
  font-size: 36rpx;
  color: #cbd5e1;
}

.version {
  text-align: center;
  padding: 48rpx 0 24rpx;
  font-size: 22rpx;
  color: #b7bfc9;
}

.about-line {
  padding: 12rpx 0;
  font-size: 27rpx;
  color: #1e293b;
}

.about-sub {
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
