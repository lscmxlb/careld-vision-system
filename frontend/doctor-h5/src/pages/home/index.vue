<template>
  <view class="home">
    <!-- 顶部渐变区：门店 / 医生 / 今日统计 -->
    <view class="hero" :style="{ paddingTop: statusBarHeight + 24 + 'px' }">
      <view class="hero-top">
        <view class="hero-left">
          <text class="hero-store" :style="storeNameStyle">{{ storeNameText }}</text>
          <text class="hero-doctor">{{ userStore.displayName }}<text class="hero-role"> · {{ roleText }}</text></text>
        </view>
        <view class="hero-date">
          <text class="hd-day">{{ todayLabel }}</text>
          <text class="hd-week">{{ weekdayLabel(today) }}</text>
        </view>
      </view>
      <!-- 离屏测量用：按基准字号渲染医院名，用于计算单行自适应字号 -->
      <text class="hero-measure" :style="{ fontSize: baseFontPx + 'px' }">{{ storeNameText }}</text>
    </view>

    <view class="body">
      <!-- 数据统计：3 个球展示儿童档案 / 当前已预约 / 已完成养护 -->
      <view class="stats">
        <view v-for="item in statItems" :key="item.label" class="stats-item">
          <view class="stats-ball" :class="item.ball">
            <text class="stats-value" :class="valueSizeClass(item.value)">{{ item.value }}</text>
          </view>
          <text class="stats-label">{{ item.label }}</text>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="quick">
        <view class="quick-item" @click="goCreate">
          <view class="quick-icon qi-primary">预约</view>
          <text class="quick-text">添加预约</text>
        </view>
        <view class="quick-item" @click="goGrant">
          <view class="quick-icon qi-emerald">授权</view>
          <text class="quick-text">添加授权</text>
        </view>
        <view class="quick-item" @click="goChildForm">
          <view class="quick-icon qi-amber">档案</view>
          <text class="quick-text">添加档案</text>
        </view>
        <view class="quick-item" @click="goCareRecords">
          <view class="quick-icon qi-violet">记录</view>
          <text class="quick-text">养护记录</text>
        </view>
      </view>

      <!-- 预约日历：每日显示 A=已预约数 / B=当天可接受预约总数 -->
      <view class="cal-card">
        <view class="cal-header">
          <text class="cal-header-title">预约日历</text>
          <view class="cal-month-nav">
            <view class="cal-nav" @click="changeMonth(-1)">‹</view>
            <text class="cal-month-text">{{ calTitle }}</text>
            <view class="cal-nav" @click="changeMonth(1)">›</view>
          </view>
        </view>
        <view class="cal-week">
          <text v-for="w in WEEK_LABELS" :key="w" class="cal-week-item">{{ w }}</text>
        </view>
        <view class="cal-grid">
          <view v-for="(cell, idx) in calCells" :key="cell.date || `blank-${idx}`" class="cal-cell">
            <view v-if="cell.day" class="cal-box">
              <text class="cal-day" :class="{ 'cal-day-today': cell.date === today }">{{ cell.day }}</text>
              <text class="cal-ab" :class="{ 'cal-ab-empty': cell.ab === '-/-' }">{{ cell.ab }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="safe-bottom" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { scheduleRuleApi, statisticsApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { userRoleText } from '@/utils/dict'
import { todayStr, weekdayLabel } from '@/utils/format'
import type { SlotDailySummary, WorkbenchStats } from '@/types'

const userStore = useUserStore()
const statusBarHeight = ref(0)
try {
  statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 0
} catch {
  statusBarHeight.value = 0
}

const today = todayStr()
const todayLabel = computed(() => today.slice(5).replace('-', '/'))

const roleText = computed(() => userRoleText(userStore.userType, userStore.userInfo?.staffRole))

/* ---------------- 数据统计（儿童档案 / 当前已预约 / 已完成养护） ---------------- */
const workbenchStats = ref<WorkbenchStats>({ childCount: 0, reservedCount: 0, completedCareCount: 0 })

const statItems = computed(() => [
  { label: '儿童档案数量', value: workbenchStats.value.childCount, ball: 'sb-primary' },
  { label: '当前已预约数量', value: workbenchStats.value.reservedCount, ball: 'sb-emerald' },
  { label: '已完成养护次数', value: workbenchStats.value.completedCareCount, ball: 'sb-violet' },
])

/** 位数过多时缩小字号，保证数值完整显示在球内 */
function valueSizeClass(value: number): string {
  const len = String(value ?? 0).length
  if (len >= 4) return 'stats-value-xs'
  if (len === 3) return 'stats-value-sm'
  return ''
}

async function fetchWorkbenchStats() {
  try {
    workbenchStats.value = await statisticsApi.getWorkbenchStats(userStore.storeId)
  } catch {
    // 统计失败不阻断页面，保留上一次数据
  }
}

/* ---------------- 医院名称自适应字号（字多时自动缩小，保证单行且不遮挡右侧日期） ---------------- */
const STORE_BASE_RPX = 42
const STORE_MIN_RPX = 24

const storeNameText = computed(() => userStore.storeName || '未分配门店')
const baseFontPx = computed(() => uni.upx2px(STORE_BASE_RPX))
const storeFontPx = ref(0)
const storeNameStyle = computed(() => (storeFontPx.value ? { fontSize: `${storeFontPx.value}px` } : {}))

function fitStoreName() {
  nextTick(() => {
    const query = uni.createSelectorQuery()
    query.select('.hero-left').boundingClientRect()
    query.select('.hero-measure').boundingClientRect()
    query.exec((res) => {
      const leftRect = res?.[0] as { width?: number } | null
      const measureRect = res?.[1] as { width?: number } | null
      const availWidth = (leftRect?.width || 0) - 8
      const baseWidth = measureRect?.width || 0
      if (availWidth <= 0 || baseWidth <= 0) return
      // 同一字体下文字宽度与字号成正比，按可用宽度等比缩放
      const size = Math.min(baseFontPx.value, (baseFontPx.value * availWidth) / baseWidth)
      storeFontPx.value = Math.max(uni.upx2px(STORE_MIN_RPX), Math.floor(size))
    })
  })
}

watch(storeNameText, () => fitStoreName(), { immediate: true })

/* ---------------- 预约日历（A=当天已预约数 B=当天可接受预约总数；B 固定，不随 A 变化） ---------------- */
const WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

const now = new Date()
const calYear = ref(now.getFullYear())
const calMonth = ref(now.getMonth())
const summaryMap = ref<Record<string, SlotDailySummary>>({})

const calTitle = computed(() => `${calYear.value}年${calMonth.value + 1}月`)

const calCells = computed(() => {
  const firstWeekday = new Date(calYear.value, calMonth.value, 1).getDay()
  const leadBlanks = (firstWeekday + 6) % 7
  const daysInMonth = new Date(calYear.value, calMonth.value + 1, 0).getDate()
  const cells: { date: string; day: number; ab: string }[] = []
  for (let i = 0; i < leadBlanks; i++) {
    cells.push({ date: '', day: 0, ab: '' })
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const date = `${calYear.value}-${String(calMonth.value + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    const item = summaryMap.value[date]
    cells.push({ date, day: d, ab: item && item.total > 0 ? `${item.booked}/${item.total}` : '-/-' })
  }
  return cells
})

function changeMonth(step: number) {
  const target = new Date(calYear.value, calMonth.value + step, 1)
  calYear.value = target.getFullYear()
  calMonth.value = target.getMonth()
  summaryMap.value = {}
  fetchCalendar()
}

async function fetchCalendar() {
  const monthStart = `${calYear.value}-${String(calMonth.value + 1).padStart(2, '0')}-01`
  const daysInMonth = new Date(calYear.value, calMonth.value + 1, 0).getDate()
  const monthEnd = `${calYear.value}-${String(calMonth.value + 1).padStart(2, '0')}-${String(daysInMonth).padStart(2, '0')}`
  try {
    const res = await scheduleRuleApi.getSlotDailySummary(monthStart, monthEnd, userStore.storeId)
    const map: Record<string, SlotDailySummary> = {}
    ;(res || []).forEach((item) => {
      map[item.date] = item
    })
    summaryMap.value = map
  } catch {
    summaryMap.value = {}
  }
}

onShow(() => {
  fetchWorkbenchStats()
  fetchCalendar()
  userStore.fetchProfile().catch(() => undefined)
})

onPullDownRefresh(async () => {
  await Promise.all([fetchWorkbenchStats(), fetchCalendar()])
  uni.stopPullDownRefresh()
})

/* ---------------- 操作 ---------------- */
function goCreate() {
  uni.navigateTo({ url: '/pages/reserve/create' })
}

/** 添加档案：同儿童档案页「+建档」 */
function goChildForm() {
  uni.navigateTo({ url: '/pages/child/form' })
}

function goCareRecords() {
  uni.switchTab({ url: '/pages/care-record/index' })
}

/** 添加授权：先选儿童，再进入预约授权页 */
function goGrant() {
  uni.navigateTo({ url: '/pages/child/select' })
}
</script>

<style lang="scss" scoped>
.home {
  min-height: 100vh;
  background: #f4f6f9;
}

.hero {
  background: linear-gradient(150deg, #60a5fa 0%, #2563eb 55%, #1d4ed8 100%);
  padding: 0 32rpx 88rpx;
  border-radius: 0 0 36rpx 36rpx;
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-top: 24rpx;
}

.hero-left {
  min-width: 0;
  flex: 1;
}

.hero-store {
  display: block;
  font-size: 42rpx;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  /* 极端超长名称在最小字号下仍超宽时省略，避免半个字被硬裁 */
  text-overflow: ellipsis;
}

/* 离屏测量：与医院名同字体，nowrap 下宽度即文字自然宽度 */
.hero-measure {
  position: fixed;
  left: -9999px;
  top: 0;
  font-weight: 600;
  white-space: nowrap;
  visibility: hidden;
  pointer-events: none;
}

.hero-doctor {
  display: block;
  margin-top: 10rpx;
  font-size: 42rpx;
  font-weight: 600;
  color: #fff;
}

.hero-role {
  font-size: 24rpx;
  font-weight: 400;
  opacity: 0.85;
}

.hero-date {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding-top: 6rpx;
}

.hd-day {
  font-size: 28rpx;
  color: #fff;
  font-weight: 500;
}

.hd-week {
  font-size: 23rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 6rpx;
}

.body {
  padding: 0 28rpx;
  margin-top: -56rpx;
}

.stats {
  display: flex;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.stats-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stats-ball {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10rpx 20rpx rgba(15, 23, 42, 0.16), inset -4rpx -6rpx 14rpx rgba(255, 255, 255, 0.28);
}

.sb-primary {
  background: radial-gradient(circle at 32% 26%, #bfdbfe 0%, #3b82f6 52%, #1d4ed8 100%);
}

.sb-emerald {
  background: radial-gradient(circle at 32% 26%, #a7f3d0 0%, #10b981 52%, #047857 100%);
}

.sb-violet {
  background: radial-gradient(circle at 32% 26%, #ddd6fe 0%, #8b5cf6 52%, #6d28d9 100%);
}

.stats-value {
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1;
  color: #fff;
  text-shadow: 0 2rpx 4rpx rgba(15, 23, 42, 0.18);
}

.stats-value-sm {
  font-size: 24rpx;
}

.stats-value-xs {
  font-size: 20rpx;
}

.stats-label {
  margin-top: 14rpx;
  font-size: 23rpx;
  color: #5b6572;
}

.quick {
  display: flex;
  margin-top: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.quick-icon {
  width: 84rpx;
  height: 84rpx;
  border-radius: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: 600;
  letter-spacing: 2rpx;
  color: #fff;
}

.qi-primary {
  background: linear-gradient(135deg, #93c5fd, #2563eb);
}

.qi-emerald {
  background: linear-gradient(135deg, #6ee7b7, #10b981);
}

.qi-violet {
  background: linear-gradient(135deg, #a5b4fc, #6366f1);
}

.qi-amber {
  background: linear-gradient(135deg, #fcd34d, #f59e0b);
}

.quick-text {
  margin-top: 14rpx;
  font-size: 25rpx;
  color: #5b6572;
}

.cal-card {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx 24rpx 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.cal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cal-header-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
}

.cal-month-nav {
  display: flex;
  align-items: center;
}

.cal-nav {
  width: 48rpx;
  height: 48rpx;
  line-height: 44rpx;
  text-align: center;
  font-size: 34rpx;
  color: #2563eb;
  background: #eff6ff;
  border-radius: 50%;
}

.cal-month-text {
  min-width: 160rpx;
  margin: 0 16rpx;
  text-align: center;
  font-size: 26rpx;
  color: #1e293b;
}

.cal-week {
  display: flex;
  margin-top: 22rpx;
}

.cal-week-item {
  flex: 1;
  text-align: center;
  font-size: 22rpx;
  color: #94a3b8;
}

.cal-grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 8rpx;
}

.cal-cell {
  width: 14.2857%;
  padding: 5rpx 4rpx;
  box-sizing: border-box;
}

.cal-box {
  height: 88rpx;
  border-radius: 12rpx;
  background: #f8fafc;
  border: 1rpx solid #eef2f7;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.cal-day {
  font-size: 26rpx;
  color: #1e293b;
}

.cal-day-today {
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  text-align: center;
  font-size: 24rpx;
  color: #fff;
  background: #2563eb;
  border-radius: 50%;
}

.cal-ab {
  margin-top: 4rpx;
  font-size: 20rpx;
  color: #2563eb;
}

.cal-ab-empty {
  color: #cbd5e1;
}
</style>
