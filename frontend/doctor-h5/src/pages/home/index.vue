<template>
  <view class="home">
    <!-- 顶部渐变区：门店 / 医生 / 今日统计 -->
    <view class="hero" :style="{ paddingTop: statusBarHeight + 24 + 'px' }">
      <view class="hero-top">
        <view class="hero-left">
          <text class="hero-store">{{ userStore.storeName || '未分配门店' }}</text>
          <text class="hero-doctor">{{ userStore.displayName }}<text class="hero-role"> · {{ roleText }}</text></text>
        </view>
        <view class="hero-date">
          <text class="hd-day">{{ todayLabel }}</text>
          <text class="hd-week">{{ weekdayLabel(today) }}</text>
        </view>
      </view>
    </view>

    <view class="body">
      <!-- 预约日历：每日显示 已预约/可预约 -->
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
            <template v-if="cell.day">
              <text class="cal-day" :class="{ 'cal-day-today': cell.date === today }">{{ cell.day }}</text>
              <text class="cal-ab" :class="{ 'cal-ab-empty': cell.ab === '-/-' }">{{ cell.ab }}</text>
            </template>
          </view>
        </view>
      </view>

      <!-- 快捷入口 -->
      <view class="quick">
        <view class="quick-item" @click="goCreate">
          <view class="quick-icon qi-primary">＋</view>
          <text class="quick-text">新建预约</text>
        </view>
        <view class="quick-item" @click="goCareRecords">
          <view class="quick-icon qi-violet">✎</view>
          <text class="quick-text">养护记录</text>
        </view>
        <view class="quick-item" @click="goChildren">
          <view class="quick-icon qi-amber">☰</view>
          <text class="quick-text">儿童档案</text>
        </view>
      </view>

      <view class="section-title">
        <text>今日预约</text>
        <text class="section-more" @click="goReserveList">全部预约 ›</text>
      </view>

      <view v-if="loading" class="loading-tip">加载中…</view>

      <template v-else>
        <ReserveCard
          v-for="row in list"
          :key="row.id"
          :row="row"
          show-date
          @care="goCare"
          @adjust="goAdjust"
          @cancel="openCancel"
          @noshow="confirmNoShow"
          @detail="goDetail"
        />
        <view v-if="!list.length" class="empty">
          <view class="empty-icon">☺</view>
          <text>今天还没有预约安排</text>
        </view>
      </template>

      <view class="safe-bottom" />
    </view>

    <CancelSheet
      v-model:visible="cancelVisible"
      :row="cancelingRow"
      :loading="cancelLoading"
      @confirm="submitCancel"
    />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import ReserveCard from '@/components/ReserveCard.vue'
import CancelSheet from '@/components/CancelSheet.vue'
import { reserveApi, scheduleRuleApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { userRoleText } from '@/utils/dict'
import { todayStr, weekdayLabel } from '@/utils/format'
import type { Reserve, SlotDailySummary } from '@/types'

const userStore = useUserStore()
const statusBarHeight = ref(0)
try {
  statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 0
} catch {
  statusBarHeight.value = 0
}

const today = todayStr()
const todayLabel = computed(() => today.slice(5).replace('-', '/'))

const list = ref<Reserve[]>([])
const loading = ref(false)

const roleText = computed(() => userRoleText(userStore.userType, userStore.userInfo?.staffRole))

/* ---------------- 预约日历（A=已预约 B=可预约，为 0 显示“-”） ---------------- */
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
    cells.push({ date, day: d, ab: item ? `${abNum(item.booked)}/${abNum(item.available)}` : '-/-' })
  }
  return cells
})

function abNum(value: number) {
  return value > 0 ? String(value) : '-'
}

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

async function fetchToday() {
  loading.value = true
  try {
    const res = await reserveApi.getReserveList({ date: today, page: 1, size: 100 })
    list.value = (res.list || []).slice().sort((a, b) => (a.timeSlotStart || '').localeCompare(b.timeSlotStart || ''))
  } catch {
    // 错误提示已在请求层处理
  } finally {
    loading.value = false
  }
}

onShow(() => {
  fetchToday()
  fetchCalendar()
  userStore.fetchProfile().catch(() => undefined)
})

onPullDownRefresh(async () => {
  await fetchToday()
  await fetchCalendar()
  uni.stopPullDownRefresh()
})

/* ---------------- 操作 ---------------- */
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelingRow = ref<Reserve | null>(null)

function goCreate() {
  uni.navigateTo({ url: '/pages/reserve/create' })
}

function goAdjust(row: Reserve) {
  uni.navigateTo({ url: `/pages/reserve/create?mode=adjust&id=${row.id}` })
}

function goCare(row: Reserve) {
  uni.navigateTo({ url: `/pages/reserve/care?id=${row.id}` })
}

function goDetail(row: Reserve) {
  uni.navigateTo({ url: `/pages/reserve/detail?id=${row.id}` })
}

function goReserveList() {
  uni.switchTab({ url: '/pages/reserve/index' })
}

function goCareRecords() {
  uni.navigateTo({ url: '/pages/care-record/index' })
}

function goChildren() {
  uni.switchTab({ url: '/pages/child/index' })
}

function openCancel(row: Reserve) {
  cancelingRow.value = row
  cancelVisible.value = true
}

async function submitCancel(reason: string) {
  if (!cancelingRow.value) return
  cancelLoading.value = true
  try {
    await reserveApi.cancelReserve(cancelingRow.value.id, reason)
    uni.showToast({ title: '预约已取消，次数已退还', icon: 'none' })
    cancelVisible.value = false
    fetchToday()
  } catch {
    // 错误提示已在请求层处理
  } finally {
    cancelLoading.value = false
  }
}

function confirmNoShow(row: Reserve) {
  uni.showModal({
    title: '标记爽约',
    content: '标记爽约不退还预约次数（与取消不同），确认标记该预约为爽约吗？',
    confirmText: '确定标记',
    confirmColor: '#f59e0b',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await reserveApi.markNoShow(row.id)
        uni.showToast({ title: '已标记爽约（不退还次数）', icon: 'none' })
        fetchToday()
      } catch {
        // 错误提示已在请求层处理
      }
    },
  })
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
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
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

.cal-card {
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
  height: 76rpx;
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

.quick {
  display: flex;
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
  font-size: 40rpx;
  color: #fff;
}

.qi-primary {
  background: linear-gradient(135deg, #93c5fd, #2563eb);
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

.section-title {
  margin-top: 40rpx;
}
</style>
