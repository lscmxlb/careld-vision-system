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
      <view class="kv kv-editable" @click="openNameEdit">
        <text class="kv-key">用户名称</text>
        <view class="kv-right">
          <text class="kv-value">{{ displayName }}</text>
          <text class="kv-action">修改</text>
        </view>
      </view>
      <view class="kv kv-editable" @click="openPhoneEdit">
        <text class="kv-key">手机号码</text>
        <view class="kv-right">
          <text class="kv-value">{{ userStore.phoneMask }}</text>
          <text class="kv-action">修改</text>
        </view>
      </view>
      <view class="kv kv-editable" @click="openPwdEdit">
        <text class="kv-key">登录密码</text>
        <view class="kv-right">
          <text class="kv-value">已设置</text>
          <text class="kv-action">修改</text>
        </view>
      </view>
      <!-- 暂时隐藏：微信公众号绑定入口（恢复时删除 v-if="false"） -->
      <view v-if="false" class="kv kv-editable" @click="openWechat">
        <text class="kv-key">微信公众号</text>
        <view class="kv-right">
          <text class="kv-value" :class="{ 'is-bound': wechat.bound }">{{ wechat.bound ? '已绑定' : '未绑定' }}</text>
          <text class="kv-action">{{ wechat.bound ? '查看' : '设置' }}</text>
        </view>
      </view>
      <view class="kv"><text class="kv-key">绑定儿童</text><text class="kv-value">{{ childCount }} 个</text></view>
      <view class="kv"><text class="kv-key">剩余可用次数</text><text class="kv-value">{{ totalRemaining }} 次</text></view>
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

    <!-- 严格模态：遮罩只负责挡住下层，点击/拖动都不关闭弹窗 -->
    <view v-if="showAbout" class="mask" @touchmove.stop.prevent="noop" />
    <view v-if="showAbout" class="sheet sheet-center">
      <view class="sheet-header">
        <text class="sheet-title">关于</text>
        <view class="sheet-close-btn" @click="showAbout = false">
          <text class="sheet-close-icon">✕</text>
        </view>
      </view>
      <view class="sheet-body">
        <view class="about-line">Careld 儿童视力养护管理系统 · 家长端</view>
        <view class="about-line about-sub">版本 v2.0.1（H5 / uni-app）</view>
        <view class="about-line about-sub about-contact">系统服务商：可尔欧得医疗科技</view>
        <view class="about-line about-sub">服务支持电话：400-999-3608</view>
      </view>
      <view class="sheet-footer">
        <view class="btn btn-primary btn-block" @click="showAbout = false">知道了</view>
      </view>
    </view>

    <view v-if="showNameEdit" class="mask" @touchmove.stop.prevent="noop" />
    <view v-if="showNameEdit" class="sheet sheet-center">
      <view class="sheet-header">
        <text class="sheet-title">修改用户名称</text>
        <view class="sheet-close-btn" @click="closeNameEdit">
          <text class="sheet-close-icon">✕</text>
        </view>
      </view>
      <view class="sheet-body">
        <view class="name-input-wrap">
          <input
            v-model="nameDraft"
            class="name-input"
            type="text"
            maxlength="20"
            placeholder="请输入用户名称"
            placeholder-class="name-ph"
            @confirm="saveName"
          />
        </view>
        <view class="about-line about-sub">用户名称将作为建立儿童档案时的家长姓名。</view>
      </view>
      <view class="sheet-footer">
        <view class="btn btn-plain" @click="closeNameEdit">取消</view>
        <view class="btn btn-primary" :class="{ 'is-disabled': savingName }" @click="saveName">
          {{ savingName ? '保存中…' : '保存' }}
        </view>
      </view>
    </view>

    <view v-if="showPhoneEdit" class="mask" @touchmove.stop.prevent="noop" />
    <view v-if="showPhoneEdit" class="sheet sheet-center">
      <view class="sheet-header">
        <text class="sheet-title">修改手机号码</text>
        <view class="sheet-close-btn" @click="closePhoneEdit">
          <text class="sheet-close-icon">✕</text>
        </view>
      </view>
      <view class="sheet-body">
        <view class="name-input-wrap">
          <input
            v-model="phoneDraft"
            class="name-input"
            type="number"
            maxlength="11"
            placeholder="请输入新手机号码"
            placeholder-class="name-ph"
          />
        </view>
        <view class="code-row">
          <input
            v-model="phoneCode"
            class="name-input code-input"
            type="number"
            maxlength="6"
            placeholder="请输入短信验证码"
            placeholder-class="name-ph"
          />
          <text
            class="code-btn"
            :class="{ 'code-btn-disabled': countdown > 0 || sendingCode }"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s 后重发` : sendingCode ? '发送中…' : '获取验证码' }}
          </text>
        </view>
        <view class="about-line about-sub">新手机号码需要通过短信验证，原手机号码无需验证；修改后请使用新手机号码登录。</view>
      </view>
      <view class="sheet-footer">
        <view class="btn btn-plain" @click="closePhoneEdit">取消</view>
        <view class="btn btn-primary" :class="{ 'is-disabled': savingPhone }" @click="savePhone">
          {{ savingPhone ? '保存中…' : '保存' }}
        </view>
      </view>
    </view>

    <view v-if="showPwdEdit" class="mask" @touchmove.stop.prevent="noop" />
    <view v-if="showPwdEdit" class="sheet sheet-center">
      <view class="sheet-header">
        <text class="sheet-title">修改登录密码</text>
        <view class="sheet-close-btn" @click="closePwdEdit">
          <text class="sheet-close-icon">✕</text>
        </view>
      </view>
      <view class="sheet-body">
        <view class="name-input-wrap">
          <input
            v-model="oldPwd"
            class="name-input"
            :password="!showPwd"
            maxlength="20"
            placeholder="请输入原密码"
            placeholder-class="name-ph"
          />
        </view>
        <view class="name-input-wrap">
          <input
            v-model="newPwd"
            class="name-input"
            :password="!showPwd"
            maxlength="20"
            placeholder="请输入新密码（6~20位）"
            placeholder-class="name-ph"
          />
        </view>
        <view class="name-input-wrap">
          <input
            v-model="confirmPwd"
            class="name-input"
            :password="!showPwd"
            maxlength="20"
            placeholder="请再次输入新密码"
            placeholder-class="name-ph"
            @confirm="savePassword"
          />
        </view>
        <view class="pwd-tools">
          <text class="pwd-compare" :class="pwdMatchClass">{{ pwdMatchText }}</text>
          <text class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏密码' : '显示密码' }}</text>
        </view>
      </view>
      <view class="sheet-footer">
        <view class="btn btn-plain" @click="closePwdEdit">取消</view>
        <view class="btn btn-primary" :class="{ 'is-disabled': savingPwd || pwdMismatch }" @click="savePassword">
          {{ savingPwd ? '保存中…' : '保存' }}
        </view>
      </view>
    </view>

    <view v-if="showWechat" class="mask" @touchmove.stop.prevent="noop" />
    <view v-if="showWechat" class="sheet sheet-center">
      <view class="sheet-header">
        <text class="sheet-title">微信公众号</text>
        <view class="sheet-close-btn" @click="closeWechat">
          <text class="sheet-close-icon">✕</text>
        </view>
      </view>
      <view class="sheet-body">
        <view class="wechat-account">{{ wechat.officialAccountName || '可尔欧得视力养护' }}</view>
        <view v-if="wechat.bound" class="wechat-bound">
          <text class="wechat-bound-line">已绑定（账号尾号 {{ wechat.openidTail || '—' }}）</text>
          <text class="wechat-bound-line wechat-bound-sub">绑定时间：{{ wechatBoundAtText }}</text>
          <text class="wechat-tip wechat-bound-tip">微信要求「先授权后下发」：请在微信内点下方按钮完成订阅授权（可在公众号内随时关闭），之后预约成功、取消、养护完成等消息才会推送给您。</text>
        </view>
        <template v-else>
          <view class="wechat-qr">
            <image v-if="qrImage" class="wechat-qr-img" :src="qrImage" mode="aspectFit" />
            <view v-else class="wechat-qr-img wechat-qr-empty">
              <text class="wechat-qr-empty-text">{{ qrLoading ? '二维码生成中…' : '二维码暂不可用' }}</text>
            </view>
            <text class="wechat-qr-tip">{{ qrTip }}</text>
            <text v-if="qrCountdownText" class="wechat-qr-sub">{{ qrCountdownText }}</text>
          </view>
          <view class="wechat-tip">{{ qrTipDetail }}</view>
          <view v-if="bindQr?.configured" class="wechat-refresh" @click="loadBindQr">
            <text class="wechat-refresh-text">二维码识别不了或已过期？点此刷新</text>
          </view>
        </template>
      </view>
      <view class="sheet-footer">
        <view class="btn btn-primary" :class="{ 'is-disabled': bindingWechat }" @click="openWechatAuth">
          {{ wechat.bound ? '开启消息通知授权' : '微信内一键绑定并授权' }}
        </view>
        <view v-if="wechat.bound" class="btn btn-plain" :class="{ 'is-disabled': bindingWechat }" @click="unbindWechat">
          {{ bindingWechat ? '处理中…' : '解除绑定' }}
        </view>
        <view v-else-if="showConfirmBind" class="btn btn-plain" :class="{ 'is-disabled': bindingWechat }" @click="bindWechat">
          {{ bindingWechat ? '绑定中…' : '我已关注，确认绑定' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow, onUnload } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { notifyApi, userApi } from '@/api'
import type { WechatBindQr, WechatStatus } from '@/types'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/request'

const userStore = useUserStore()
const showAbout = ref(false)
const childCount = ref(0)
const totalRemaining = ref(0)

function noop() {}

/* ---------------- 用户名称 ---------------- */
const showNameEdit = ref(false)
const nameDraft = ref('')
const savingName = ref(false)

function openNameEdit() {
  nameDraft.value = userStore.userInfo?.realName || ''
  showNameEdit.value = true
}

function closeNameEdit() {
  showNameEdit.value = false
}

async function saveName() {
  if (savingName.value) return
  const name = nameDraft.value.trim()
  if (!name) {
    toast('请输入用户名称')
    return
  }
  if (name === (userStore.userInfo?.realName || '')) {
    closeNameEdit()
    return
  }
  savingName.value = true
  try {
    await userApi.updateMyProfile({ realName: name })
    await userStore.fetchProfile()
    toast('用户名称已保存')
    closeNameEdit()
  } catch {
    // 错误提示已在请求层处理
  } finally {
    savingName.value = false
  }
}

/* ---------------- 手机号码 ---------------- */
const showPhoneEdit = ref(false)
const phoneDraft = ref('')
const phoneCode = ref('')
const savingPhone = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

function openPhoneEdit() {
  phoneDraft.value = ''
  phoneCode.value = ''
  showPhoneEdit.value = true
}

function closePhoneEdit() {
  showPhoneEdit.value = false
}

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
  if (countdown.value > 0 || sendingCode.value) return
  if (!/^1[3-9]\d{9}$/.test(phoneDraft.value.trim())) {
    toast('请输入正确的11位手机号')
    return
  }
  sendingCode.value = true
  try {
    await userStore.sendSms(phoneDraft.value.trim())
    toast('验证码已发送，请注意查收')
    startCountdown()
  } catch {
    // 错误提示已在请求层处理（含频控提示）
  } finally {
    sendingCode.value = false
  }
}

async function savePhone() {
  if (savingPhone.value) return
  const phone = phoneDraft.value.trim()
  if (!/^1[3-9]\d{9}$/.test(phone)) {
    toast('请输入正确的11位手机号')
    return
  }
  if (phone === userStore.phone) {
    toast('新手机号与当前手机号相同')
    return
  }
  if (!phoneCode.value.trim()) {
    toast('请输入短信验证码')
    return
  }
  savingPhone.value = true
  try {
    await userStore.changePhone(phone, phoneCode.value.trim())
    toast('手机号码已修改')
    closePhoneEdit()
  } catch {
    // 错误提示已在请求层处理
  } finally {
    savingPhone.value = false
  }
}

/* ---------------- 登录密码 ---------------- */
const showPwdEdit = ref(false)
const oldPwd = ref('')
const newPwd = ref('')
const confirmPwd = ref('')
const showPwd = ref(false)
const savingPwd = ref(false)

const pwdMismatch = computed(() => !!confirmPwd.value && confirmPwd.value !== newPwd.value)
const pwdMatchText = computed(() => {
  if (!confirmPwd.value) return ''
  return pwdMismatch.value ? '两次输入的密码不一致' : '两次输入的密码一致'
})
const pwdMatchClass = computed(() => (pwdMismatch.value ? 'is-bad' : 'is-ok'))

function openPwdEdit() {
  oldPwd.value = ''
  newPwd.value = ''
  confirmPwd.value = ''
  showPwd.value = false
  showPwdEdit.value = true
}

function closePwdEdit() {
  showPwdEdit.value = false
}

async function savePassword() {
  if (savingPwd.value) return
  if (!oldPwd.value) {
    toast('请输入原密码')
    return
  }
  if (newPwd.value.length < 6 || newPwd.value.length > 20) {
    toast('新密码长度需为6~20位')
    return
  }
  if (pwdMismatch.value || !confirmPwd.value) {
    toast('两次输入的密码不一致')
    return
  }
  savingPwd.value = true
  try {
    await userStore.changePassword(oldPwd.value, newPwd.value)
    toast('登录密码已修改')
    closePwdEdit()
  } catch {
    // 原密码错误等提示已在请求层处理
  } finally {
    savingPwd.value = false
  }
}

/* ---------------- 微信公众号 ---------------- */
const showWechat = ref(false)
const bindingWechat = ref(false)
const wechat = ref<WechatStatus>({ bound: false })
const bindQr = ref<WechatBindQr | null>(null)
const qrLoading = ref(false)
const qrExpireAt = ref(0)
const nowTs = ref(Date.now())
let pollTimer: ReturnType<typeof setInterval> | null = null
let tickTimer: ReturnType<typeof setInterval> | null = null

const wechatBoundAtText = computed(() => {
  const raw = wechat.value.boundAt
  if (!raw) return '—'
  return String(raw).replace('T', ' ').slice(0, 16)
})

/** 公众号官方关注二维码（动态带参二维码不可用时的兜底展示） */
const OFFICIAL_QR = '/static/wechat-official-qr.jpg'

const qrImage = computed(() => {
  if (bindQr.value?.qrImageUrl) return bindQr.value.qrImageUrl
  return OFFICIAL_QR
})

const qrTip = computed(() =>
  bindQr.value?.configured ? '用微信扫一扫关注公众号，关注后自动完成绑定' : '微信扫一扫或长按识别，关注公众号',
)

const qrTipDetail = computed(() => {
  if (bindQr.value?.hint) return bindQr.value.hint
  if (bindQr.value?.configured) {
    return '绑定后孩子的建档、预约与养护完成通知将通过微信消息推送给您；已关注过公众号的家长，再扫一次即可完成绑定。'
  }
  return '关注公众号后，点击下方「我已关注，确认绑定」完成绑定。绑定后孩子的建档、预约与养护完成通知将通过微信消息推送给您。'
})

/** 只有公众号凭据未配置时才展示模拟绑定按钮（真实凭据下走扫码自动绑定） */
const showConfirmBind = computed(() => !!bindQr.value && !bindQr.value.configured)

const qrCountdownText = computed(() => {
  if (!qrExpireAt.value || !bindQr.value?.qrImageUrl) return ''
  const remain = Math.floor((qrExpireAt.value - nowTs.value) / 1000)
  if (remain <= 0) return '二维码已过期，请点下方刷新'
  return `二维码 ${Math.floor(remain / 60)} 分 ${String(remain % 60).padStart(2, '0')} 秒后失效`
})

function stopWatchers() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (tickTimer) {
    clearInterval(tickTimer)
    tickTimer = null
  }
}

/** 停留期间轮询绑定状态：扫码关注完成后自动提示成功 */
function startWatchers() {
  stopWatchers()
  tickTimer = setInterval(() => {
    nowTs.value = Date.now()
  }, 1000)
  pollTimer = setInterval(async () => {
    if (wechat.value.bound) {
      stopWatchers()
      return
    }
    await loadWechat()
    if (wechat.value.bound) {
      stopWatchers()
      bindQr.value = null
      toast('微信公众号绑定成功')
    }
  }, 3000)
}

async function loadWechat() {
  try {
    wechat.value = await notifyApi.wechatStatus()
  } catch {
    // 通知服务不可用时不影响「我的」页其它功能
  }
}

async function loadBindQr() {
  if (qrLoading.value) return
  qrLoading.value = true
  try {
    const res = await notifyApi.wechatBindQr()
    bindQr.value = res
    qrExpireAt.value = res.configured && res.qrImageUrl ? Date.now() + (res.expireSeconds || 600) * 1000 : 0
    nowTs.value = Date.now()
  } catch {
    bindQr.value = null
    qrExpireAt.value = 0
  } finally {
    qrLoading.value = false
  }
}

function openWechat() {
  showWechat.value = true
  bindQr.value = null
  qrExpireAt.value = 0
  loadWechat()
  loadBindQr()
  startWatchers()
}

function closeWechat() {
  showWechat.value = false
  stopWatchers()
}

async function bindWechat() {
  if (bindingWechat.value) return
  bindingWechat.value = true
  try {
    wechat.value = await notifyApi.bindWechat()
    toast('微信公众号已绑定，可接收通知')
  } catch {
    // 错误提示已在请求层处理
  } finally {
    bindingWechat.value = false
  }
}

function unbindWechat() {
  uni.showModal({
    title: '解除绑定',
    content: '解绑后将无法收到微信消息通知，确认解除绑定吗？',
    confirmText: '解除',
    success: async (res) => {
      if (!res.confirm || bindingWechat.value) return
      bindingWechat.value = true
      try {
        wechat.value = await notifyApi.unbindWechat()
        toast('已解除绑定')
      } catch {
        // 错误提示已在请求层处理
      } finally {
        bindingWechat.value = false
      }
    },
  })
}

/** 微信内授权页：静默网页授权绑定 openid + 开放标签拉起订阅授权（须在微信中打开） */
function openWechatAuth() {
  const inWechat = typeof navigator !== 'undefined' && /micromessenger/i.test(navigator.userAgent)
  if (!inWechat) {
    toast('请在微信中打开家长端后再操作')
    return
  }
  window.location.href = '/static/wechat-auth.html'
}

/* ---------------- 其它 ---------------- */
const displayName = computed(() =>
  userStore.userInfo?.realName || (userStore.phone ? `家长 ${userStore.phoneMask}` : '家长'),
)

const avatarText = computed(() => displayName.value.slice(0, 1))

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
  // 先按手机号认领未绑定档案（可能回填家长姓名），再刷新用户信息与儿童统计
  await userStore.claimMyChildren()
  await userStore.fetchProfile()
  loadChildren()
  loadWechat()
})

onUnload(() => {
  if (timer) clearInterval(timer)
  stopWatchers()
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

/* 本页是 tabBar 页：uni 底栏 z-index 998，弹层需更高，否则底部按钮被底栏挡住点不动 */
.mask {
  z-index: 1000;
}

.sheet {
  z-index: 1001;
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

.kv-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

.kv-action {
  flex: none;
  margin-left: 16rpx;
  font-size: 24rpx;
  color: #14b8a6;
}

.name-input-wrap {
  margin-bottom: 16rpx;
}

.name-input {
  height: 96rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: #1e293b;
  background: #f5f7fa;
  border-radius: 16rpx;
}

.name-ph {
  color: #b7bfc9;
  font-size: 30rpx;
}

.code-row {
  display: flex;
  align-items: center;
}

.code-input {
  flex: 1;
  min-width: 0;
}

.code-btn {
  flex: none;
  margin-left: 20rpx;
  font-size: 26rpx;
  color: #14b8a6;
}

.code-btn-disabled {
  color: #b7bfc9;
}

.pwd-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 40rpx;
}

.pwd-compare {
  font-size: 24rpx;
}

.pwd-compare.is-bad {
  color: #ef4444;
}

.pwd-compare.is-ok {
  color: #16a34a;
}

.pwd-toggle {
  font-size: 24rpx;
  color: #94a3b8;
}

.sheet-footer {
  display: flex;
  align-items: center;
}

.sheet-footer .btn {
  flex: 1;
}

.sheet-footer .btn + .btn {
  margin-left: 20rpx;
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

.about-contact {
  margin-top: 16rpx;
}

.kv-value.is-bound {
  color: #14b8a6;
}

.wechat-account {
  padding: 8rpx 0 20rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  color: #1e293b;
}

.wechat-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16rpx 0 24rpx;
}

.wechat-qr-img {
  width: 320rpx;
  height: 320rpx;
  padding: 16rpx;
  background: #fff;
  border: 2rpx solid #e2e8f0;
  border-radius: 16rpx;
}

.wechat-qr-tip {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.wechat-qr-sub {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #cbd5e1;
}

.wechat-qr-empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.wechat-qr-empty-text {
  font-size: 24rpx;
  color: #cbd5e1;
}

.wechat-refresh {
  padding: 8rpx 0 12rpx;
  text-align: center;
}

.wechat-refresh-text {
  font-size: 24rpx;
  color: #14b8a6;
}

.wechat-bound {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 0;
}

.wechat-bound-line {
  font-size: 27rpx;
  color: #14b8a6;
}

.wechat-bound-sub {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.wechat-bound-tip {
  margin-top: 14rpx;
  text-align: left;
}

.wechat-tip {
  padding: 4rpx 0 12rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #94a3b8;
}
</style>
