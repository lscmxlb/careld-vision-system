<template>
  <view class="page">
    <!-- 步骤条 -->
    <view class="steps">
      <view v-for="(label, i) in STEP_LABELS" :key="label" class="step-item">
        <view class="step-dot" :class="{ 'step-dot-active': step >= i + 1, 'step-dot-done': step > i + 1 }">
          <text v-if="step > i + 1">✓</text>
          <text v-else>{{ i + 1 }}</text>
        </view>
        <text class="step-label" :class="{ 'step-label-active': step >= i + 1 }">{{ label }}</text>
        <view v-if="i < STEP_LABELS.length - 1" class="step-line" :class="{ 'step-line-active': step > i + 1 }"></view>
      </view>
    </view>

    <!-- 第 1 步：选择儿童 -->
    <view v-if="step === 1" class="step-body">
      <view v-if="children.length" class="list">
        <view
          v-for="item in children"
          :key="item.id"
          class="card pick-card"
          :class="{ 'pick-active': selectedChildId === item.id, 'pick-disabled': !canPick(item) }"
          @click="pickChild(item)"
        >
          <view class="pick-head">
            <text class="pick-name">{{ item.name || '未命名' }}</text>
            <text class="remain">{{ item.remainingCount ?? 0 }}<text class="remain-unit">次</text></text>
          </view>
          <view class="pick-meta">
            <text>{{ item.gender === 1 ? '男' : item.gender === 0 ? '女' : '未知' }}</text>
            <text class="dot">·</text>
            <text>{{ ageLabel(item) }}</text>
            <text class="dot">·</text>
            <text>{{ item.phone || '—' }}</text>
          </view>
          <view v-if="!canPick(item)" class="pick-tip">{{ pickTip(item) }}</view>
        </view>
      </view>
      <view v-else-if="!loaded" class="loading-tip">加载中…</view>
      <view v-else class="empty">
        <text class="empty-icon">👶</text>
        <text class="empty-text">还没有添加任何档案</text>
        <view class="empty-btn" @click="goAddChild">添加儿童档案</view>
      </view>
    </view>

    <!-- 第 2 步：选择时间 -->
    <view v-else-if="step === 2" class="step-body">
      <view v-if="dateLoaded && !availableDays.length" class="empty">
        <text class="empty-icon">📅</text>
        <text class="empty-text">近 30 天暂无可约日期</text>
      </view>

      <template v-else>
        <scroll-view class="date-scroll" scroll-x :show-scrollbar="false">
          <view class="date-list">
            <view
              v-for="day in dateStrip"
              :key="day.date"
              class="date-item"
              :class="{ 'date-active': selectedDate === day.date, 'date-disabled': !day.enabled }"
              @click="pickDate(day)"
            >
              <text class="date-week">{{ day.week }}</text>
              <text class="date-day">{{ day.label }}</text>
            </view>
          </view>
        </scroll-view>

        <view class="slot-wrap">
          <view v-if="slotLoading" class="loading-tip">加载时段中…</view>
          <view v-else-if="!slots.length" class="empty">
            <text class="empty-icon">🕐</text>
            <text class="empty-text">该日期暂无可约时段</text>
          </view>
          <view v-else class="slot-list">
            <view
              v-for="slot in slots"
              :key="slot.id"
              class="slot-item"
              :class="{ 'slot-active': selectedSlotId === slot.id, 'slot-disabled': slotDisabled(slot) }"
              @click="pickSlot(slot)"
            >
              <text class="slot-time">{{ formatHm(slot.slotStartTime) }} - {{ formatHm(slot.slotEndTime) }}</text>
              <text class="slot-remain">
                {{ slotRemainText(slot) }}
              </text>
            </view>
          </view>
        </view>
      </template>
    </view>

    <!-- 第 3 步：确认预约 -->
    <view v-else class="step-body">
      <view class="card">
        <view class="section-title">预约信息</view>
        <view class="kv"><text class="kv-key">儿童姓名</text><text class="kv-value">{{ selectedChild?.name || '—' }}</text></view>
        <view class="kv"><text class="kv-key">养护日期</text><text class="kv-value">{{ selectedDate }}（{{ weekdayLabel(selectedDate) }}）</text></view>
        <view class="kv"><text class="kv-key">养护时段</text><text class="kv-value">{{ selectedSlotText }}</text></view>
        <view class="kv"><text class="kv-key">当前可用次数</text><text class="kv-value">{{ selectedChild?.remainingCount ?? 0 }} 次</text></view>
      </view>

      <view class="card">
        <view class="field">
          <text class="field-label">备注</text>
          <textarea v-model="remark" class="field-textarea" :auto-height="true" placeholder="选填，如儿童近期状况" placeholder-class="field-placeholder" maxlength="100" />
        </view>
      </view>

      <view class="confirm-tip">预约成功后将从该儿童档案扣减 1 次可用次数；如需改约请提前取消（距开始不足 {{ cancelHours }} 小时不可取消）。</view>
    </view>

    <!-- 底部操作条：第 1 步点选儿童卡片即进入下一步，无按钮 -->
    <view v-if="step > 1" class="sticky-bar sticky-bar-on-tab">
      <view class="btn btn-plain" @click="step -= 1">上一步</view>
      <view
        v-if="step < 3"
        class="btn btn-primary"
        :class="{ 'is-disabled': !canNext }"
        @click="nextStep"
      >
        下一步
      </view>
      <view
        v-else
        class="btn btn-primary"
        :class="{ 'is-disabled': submitting }"
        @click="submit"
      >
        {{ submitting ? '提交中…' : '确认预约' }}
      </view>
    </view>

    <!-- 预约成功 -->
    <view v-if="successVisible" class="mask">
      <view class="success-card">
        <view class="success-icon">✓</view>
        <text class="success-title">预约成功</text>
        <text class="success-sub">{{ successText }}</text>
        <view class="success-actions">
          <view class="btn btn-plain" @click="goMyReserve">查看我的预约</view>
          <view class="btn btn-primary" @click="backToChild">返回档案</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { appointmentConfigApi, reserveApi, scheduleRuleApi } from '@/api/reserve'
import { useUserStore } from '@/stores/user'
import { formatHm, todayStr, weekdayLabel } from '@/utils/format'
import { toast } from '@/utils/request'
import { takeReserveChildId } from '@/utils/reserveIntent'
import type { Child, ScheduleSlot } from '@/types'

const STEP_LABELS = ['选择儿童', '选择时间', '确认预约']

const userStore = useUserStore()

const step = ref(1)
const children = ref<Child[]>([])
const loaded = ref(false)

const selectedChildId = ref(0)
const selectedDate = ref('')
const selectedSlotId = ref('')
const remark = ref('')

const availableDays = ref<string[]>([])
const dateLoaded = ref(false)
const slots = ref<ScheduleSlot[]>([])
const slotLoading = ref(false)
const submitting = ref(false)
const cancelHours = ref(24)

const successVisible = ref(false)
const successText = ref('')

const selectedChild = computed(() => children.value.find((c) => c.id === selectedChildId.value) || null)
const selectedSlot = computed(() => slots.value.find((s) => s.id === selectedSlotId.value) || null)
const selectedSlotText = computed(() =>
  selectedSlot.value ? `${formatHm(selectedSlot.value.slotStartTime)} - ${formatHm(selectedSlot.value.slotEndTime)}` : '—',
)

const dateStrip = computed(() => {
  const enabledSet = new Set(availableDays.value)
  const today = new Date()
  return Array.from({ length: 30 }, (_, i) => {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const date = `${y}-${m}-${day}`
    return {
      date,
      label: `${m}-${day}`,
      week: i === 0 ? '今天' : ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()],
      enabled: enabledSet.has(date),
    }
  })
})

const canNext = computed(() => !!selectedDate.value && !!selectedSlotId.value)

function ageLabel(item: Child) {
  if (typeof item.age === 'number' && item.age >= 0) return `${item.age}岁`
  return item.birthDate || '年龄未知'
}

function canPick(item: Child) {
  return item.auditStatus === 1 && (item.remainingCount ?? 0) > 0
}

function pickTip(item: Child) {
  if (item.auditStatus === 0) return '档案待医院审核，暂不可预约'
  if (item.auditStatus === 2) return '档案已被驳回，请修改后重新提交'
  if (!item.remainingCount) return '可用次数不足，请联系医院授权预约次数'
  return ''
}

function slotRemain(slot: ScheduleSlot) {
  return Math.max(0, (slot.maxCapacity ?? 0) - (slot.bookedCount ?? 0))
}

function slotDisabled(slot: ScheduleSlot) {
  return slot.status !== 1 || slotRemain(slot) <= 0
}

function slotRemainText(slot: ScheduleSlot) {
  if (slot.status !== 1) return '已关闭'
  const remain = slotRemain(slot)
  return remain > 0 ? `剩余${remain}人` : '预约已满'
}

async function loadChildren(childId?: number) {
  const res = await childApi.getChildList({ parentUserId: userStore.userId })
  children.value = res || []
  loaded.value = true
  if (childId && children.value.some((c) => c.id === childId)) {
    const target = children.value.find((c) => c.id === childId)!
    if (canPick(target)) {
      selectedChildId.value = childId
      step.value = 2
      loadAvailability()
    }
  }
}

async function loadAvailability() {
  const child = selectedChild.value
  if (!child?.storeId) return
  loadCancelHours(child.storeId)
  const start = todayStr()
  const end = new Date(new Date().getTime() + 29 * 86400000)
  const e = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')}`
  availableDays.value = (await scheduleRuleApi.getAvailableDates(start, e, child.storeId)) || []
  dateLoaded.value = true
  if (!selectedDate.value && availableDays.value.length) {
    selectedDate.value = availableDays.value[0]
    await loadSlots()
    await ensureDefaultSlots()
  }
}

/** 默认日期无可约时段（如当天时段已过）时自动顺延，最多探测 7 天 */
async function ensureDefaultSlots() {
  for (let i = 1; i < availableDays.value.length && i <= 7 && !slots.value.length; i++) {
    selectedDate.value = availableDays.value[i]
    await loadSlots()
  }
}

/** 取消时间窗按儿童归属门店配置读取（家长无门店上下文，需显式传 storeId） */
async function loadCancelHours(storeId: number) {
  try {
    const config = await appointmentConfigApi.getConfig(storeId)
    if (config?.parentCancelHours != null) cancelHours.value = config.parentCancelHours
  } catch {
    // 配置读取失败时保持默认 24 小时
  }
}

async function loadSlots() {
  const child = selectedChild.value
  if (!child?.storeId || !selectedDate.value) return
  slotLoading.value = true
  selectedSlotId.value = ''
  try {
    const res = await scheduleRuleApi.getSlots(selectedDate.value, child.storeId)
    const now = new Date()
    const isToday = selectedDate.value === todayStr()
    slots.value = (res || [])
      .filter((s) => !isToday || `${s.slotEndTime.slice(0, 8)}` > `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:00`)
      .sort((a, b) => a.slotStartTime.localeCompare(b.slotStartTime))
  } finally {
    slotLoading.value = false
  }
}

/** 点选儿童卡片即选中并进入下一步（选择时间） */
async function pickChild(item: Child) {
  if (!canPick(item)) {
    toast(pickTip(item))
    return
  }
  selectedChildId.value = item.id
  selectedDate.value = ''
  selectedSlotId.value = ''
  slots.value = []
  availableDays.value = []
  dateLoaded.value = false
  step.value = 2
  await loadAvailability()
}

function pickDate(day: { date: string; enabled: boolean }) {
  if (!day.enabled) {
    toast('该日期暂无可约时段')
    return
  }
  selectedDate.value = day.date
  loadSlots()
}

function pickSlot(slot: ScheduleSlot) {
  if (slotDisabled(slot)) {
    toast(slot.status !== 1 ? '该时段已关闭' : '该时段预约已满，请选择其他时段')
    return
  }
  selectedSlotId.value = slot.id
}

/** 第 2 步 → 第 3 步（第 1 步由点选儿童卡片直接进入第 2 步） */
async function nextStep() {
  if (!selectedDate.value) {
    toast('请选择养护日期')
    return
  }
  if (!selectedSlotId.value) {
    toast('请选择养护时段')
    return
  }
  const blocked = await checkDailyLimit()
  if (blocked) return
  step.value = 3
}

/** 每日一约前置拦截：所选儿童当天已有有效预约时禁止提交 */
async function checkDailyLimit(): Promise<boolean> {
  const child = selectedChild.value
  if (!child) return false
  const res = await reserveApi.getReserveList({
    childId: child.id,
    date: selectedDate.value,
    statuses: '1,2',
    page: 1,
    size: 20,
  })
  const rows = res?.list || []
  if (rows.length) {
    const slot = rows[0]
    uni.showModal({
      title: '当天已有预约',
      content: `${child.name} 在 ${selectedDate.value} ${formatHm(slot.timeSlotStart)}-${formatHm(slot.timeSlotEnd)} 已有预约，每天仅可预约一次。`,
      showCancel: false,
      confirmText: '知道了',
    })
    return true
  }
  return false
}

async function submit() {
  if (submitting.value) return
  const child = selectedChild.value
  if (!child || !selectedSlotId.value) {
    toast('预约信息不完整')
    return
  }
  if ((child.remainingCount ?? 0) <= 0) {
    toast('可用次数不足，请联系医院授权预约次数')
    return
  }
  submitting.value = true
  try {
    await reserveApi.createReserveV2({
      childId: child.id,
      slotId: selectedSlotId.value,
      remark: remark.value || undefined,
    })
    // 重新拉取档案，展示扣减后的最新剩余次数
    const latest = await childApi.getChildDetail(child.id)
    const remain = latest?.remainingCount ?? Math.max(0, (child.remainingCount ?? 1) - 1)
    successText.value = `${selectedDate.value} ${selectedSlotText.value}\n剩余可用次数：${remain} 次`
    successVisible.value = true
  } catch {
    // 错误提示已在请求层处理（含满额/每日一约/次数不足）
  } finally {
    submitting.value = false
  }
}

function resetFlow() {
  step.value = 1
  selectedChildId.value = 0
  selectedDate.value = ''
  selectedSlotId.value = ''
  slots.value = []
  availableDays.value = []
  dateLoaded.value = false
  remark.value = ''
}

function goMyReserve() {
  successVisible.value = false
  resetFlow()
  uni.switchTab({ url: '/pages/appointment/list' })
}

function backToChild() {
  successVisible.value = false
  resetFlow()
  uni.switchTab({ url: '/pages/child/index' })
}

function goAddChild() {
  uni.navigateTo({ url: '/pages/child/form' })
}

onShow(async () => {
  const intentChildId = takeReserveChildId()
  if (intentChildId) resetFlow()
  await loadChildren(intentChildId || undefined)
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding-bottom: calc(240rpx + env(safe-area-inset-bottom));
}

.steps {
  display: flex;
  align-items: center;
  padding: 32rpx 40rpx;
  background: #fff;
}

.step-item {
  display: flex;
  align-items: center;
  flex: 1;
}

.step-item:last-child {
  flex: none;
}

.step-dot {
  width: 48rpx;
  height: 48rpx;
  flex: none;
  line-height: 48rpx;
  text-align: center;
  font-size: 24rpx;
  color: #94a3b8;
  background: #f1f5f9;
  border-radius: 50%;
}

.step-dot-active {
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
}

.step-dot-done {
  color: #14b8a6;
  background: #ccfbf1;
}

.step-label {
  margin-left: 12rpx;
  font-size: 24rpx;
  color: #94a3b8;
  white-space: nowrap;
}

.step-label-active {
  color: #0f172a;
  font-weight: 600;
}

.step-line {
  flex: 1;
  height: 2rpx;
  margin: 0 12rpx;
  background: #e2e8f0;
}

.step-line-active {
  background: #99f6e4;
}

.step-body {
  padding: 20rpx 24rpx;
}

.list {
  padding: 0;
}

.pick-card {
  margin-bottom: 20rpx;
  border: 2rpx solid transparent;
}

.pick-active {
  border-color: #14b8a6;
  background: #f0fdfa;
}

.pick-disabled {
  opacity: 0.62;
}

.pick-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pick-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.remain {
  font-size: 34rpx;
  font-weight: 700;
  color: #14b8a6;
}

.remain-unit {
  font-size: 22rpx;
  font-weight: 400;
  margin-left: 4rpx;
}

.pick-meta {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #64748b;
}

.dot {
  margin: 0 10rpx;
  color: #cbd5e1;
}

.pick-tip {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #b45309;
}

.date-scroll {
  white-space: nowrap;
  padding-bottom: 8rpx;
}

.date-list {
  display: inline-flex;
}

.date-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 120rpx;
  padding: 16rpx 0;
  margin-right: 12rpx;
  background: #fff;
  border-radius: 14rpx;
  border: 2rpx solid transparent;
}

.date-active {
  border-color: #14b8a6;
  background: #f0fdfa;
}

.date-disabled {
  opacity: 0.42;
}

.date-week {
  font-size: 22rpx;
  color: #94a3b8;
}

.date-day {
  margin-top: 6rpx;
  font-size: 27rpx;
  color: #1e293b;
  font-weight: 600;
}

.slot-wrap {
  margin-top: 24rpx;
}

.slot-list {
  background: #fff;
  border-radius: 16rpx;
  overflow: hidden;
}

.slot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 28rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.slot-item:last-child {
  border-bottom: none;
}

.slot-active {
  background: #f0fdfa;
}

.slot-disabled {
  opacity: 0.45;
}

.slot-time {
  font-size: 30rpx;
  color: #1e293b;
  font-weight: 600;
}

.slot-remain {
  font-size: 24rpx;
  color: #94a3b8;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16rpx;
}

.kv {
  display: flex;
  align-items: center;
  padding: 14rpx 0;
  font-size: 28rpx;
}

.kv-key {
  width: 200rpx;
  flex: none;
  color: #94a3b8;
}

.kv-value {
  flex: 1;
  color: #1e293b;
  text-align: right;
}

/* 备注：与标签同行（左右结构），自适应高度避免撑高页面遮挡底部按钮 */
.field-textarea {
  flex: 1;
  min-width: 0;
  min-height: 64rpx;
  max-height: 160rpx;
  padding: 14rpx 16rpx;
  font-size: 28rpx;
  color: #1e293b;
  background: #f8fafc;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.field-placeholder {
  color: #cbd5e1;
}

.confirm-tip {
  margin-top: 24rpx;
  padding: 20rpx 24rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #b45309;
  background: #fffbeb;
  border-radius: 12rpx;
}

.empty-btn {
  margin-top: 32rpx;
  padding: 20rpx 64rpx;
  font-size: 28rpx;
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
  border-radius: 44rpx;
}

.success-card {
  position: absolute;
  left: 60rpx;
  right: 60rpx;
  top: 50%;
  transform: translateY(-50%);
  padding: 56rpx 40rpx 40rpx;
  background: #fff;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.success-icon {
  width: 108rpx;
  height: 108rpx;
  line-height: 108rpx;
  text-align: center;
  font-size: 56rpx;
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
  border-radius: 50%;
}

.success-title {
  margin-top: 28rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: #0f172a;
}

.success-sub {
  margin-top: 20rpx;
  font-size: 27rpx;
  line-height: 1.7;
  color: #64748b;
  text-align: center;
  white-space: pre-line;
}

.success-actions {
  display: flex;
  margin-top: 40rpx;
}

.success-actions .btn {
  margin: 0 12rpx;
}
</style>
