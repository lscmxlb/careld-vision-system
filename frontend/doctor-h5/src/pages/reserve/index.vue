<template>
  <view class="reserve-list">
    <!-- 搜索 + 新建 -->
    <view class="topbar">
      <view class="search">
        <text class="search-icon">🔍</text>
        <input
          v-model="keyword"
          class="search-input"
          placeholder="儿童姓名 / 手机号"
          placeholder-class="ph"
          confirm-type="search"
          @confirm="reload"
        />
        <text v-if="keyword" class="search-clear" @click="clearKeyword">✕</text>
      </view>
      <view class="create-btn" @click="goCreate">新建</view>
    </view>

    <!-- 状态多选 -->
    <view class="filters">
      <view class="status-chips">
        <view
          v-for="opt in STATUS_OPTIONS"
          :key="opt.value"
          class="chip"
          :class="{ 'chip-on': statuses.includes(opt.value) }"
          @click="toggleStatus(opt.value)"
        >
          {{ opt.label }}
        </view>
        <text class="reset" @click="resetFilters">重置</text>
      </view>

      <scroll-view class="date-strip" scroll-x :scroll-left="stripScrollLeft" :show-scrollbar="false">
        <view class="strip-inner">
          <view class="date-cell" :class="{ 'date-on': !date }" @click="pickDate('')">
            <text class="dc-main">全部</text>
            <text class="dc-sub">不限</text>
          </view>
          <view
            v-for="d in dateOptions"
            :key="d.value"
            class="date-cell"
            :class="{ 'date-on': date === d.value }"
            @click="pickDate(d.value)"
          >
            <text class="dc-main">{{ d.label }}</text>
            <text class="dc-sub">{{ d.sub }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 列表 -->
    <view class="list-body">
      <view v-if="loading && !list.length" class="loading-tip">加载中…</view>

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
          <view class="empty-icon">📅</view>
          <text>没有符合条件的预约</text>
          <text class="empty-sub">换个日期或状态试试</text>
        </view>
        <view v-else class="list-foot">
          <text v-if="loading">加载中…</text>
          <text v-else-if="page * size >= total">共 {{ total }} 条 · 已全部加载</text>
          <text v-else @click="loadMore">上拉加载更多（{{ list.length }}/{{ total }}）</text>
        </view>
      </template>

      <view class="safe-bottom" />
    </view>

    <CancelSheet v-model:visible="cancelVisible" :row="cancelingRow" :loading="cancelLoading" @confirm="submitCancel" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import ReserveCard from '@/components/ReserveCard.vue'
import CancelSheet from '@/components/CancelSheet.vue'
import { appointmentConfigApi, reserveApi } from '@/api'
import { RESERVE_STATUS_OPTIONS } from '@/utils/dict'
import { addDays, formatDate, todayStr } from '@/utils/format'
import type { Reserve } from '@/types'

const STATUS_OPTIONS = RESERVE_STATUS_OPTIONS
const DEFAULT_STATUSES = [1, 2, 3, 4, 5]

const keyword = ref('')
const statuses = ref<number[]>([...DEFAULT_STATUSES])
const date = ref('')
const list = ref<Reserve[]>([])
const page = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)

const today = todayStr()
const dateOptions = Array.from({ length: 30 }, (_, i) => {
  const d = addDays(new Date(`${today}T00:00:00`), i)
  const value = formatDate(d)
  const label = i === 0 ? '今天' : i === 1 ? '明天' : value.slice(5).replace('-', '/')
  const sub = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
  return { value, label, sub }
})
const stripScrollLeft = ref(0)

/** 默认勾选状态来自预约规则配置 */
async function loadDefaultStatuses() {
  try {
    const config = await appointmentConfigApi.getConfig()
    const parsed = (config.defaultShowStatuses || '1,2,3,4,5')
      .split(',')
      .map((v) => Number(v))
      .filter((v) => v >= 1 && v <= 5)
    statuses.value = parsed.length ? parsed : [...DEFAULT_STATUSES]
  } catch {
    // 拉取失败按全部状态
  }
}

async function fetchReserves(append = false) {
  loading.value = true
  try {
    const onlyCancelOrNoShow = statuses.value.length > 0 && statuses.value.every((s) => s === 4 || s === 5)
    const res = await reserveApi.getReserveList({
      keyword: keyword.value.trim() || undefined,
      statuses: statuses.value.length ? statuses.value.join(',') : undefined,
      date: date.value || undefined,
      // 未指定日期时默认只看今天及以后；只看已取消/已爽约需覆盖历史
      startDate: !date.value && !onlyCancelOrNoShow ? today : undefined,
      page: page.value,
      size,
    })
    list.value = append ? [...list.value, ...(res.list || [])] : res.list || []
    total.value = res.pagination?.total ?? list.value.length
  } catch {
    // 错误提示已在请求层处理
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  fetchReserves(false)
}

function loadMore() {
  if (loading.value || page.value * size >= total.value) return
  page.value += 1
  fetchReserves(true)
}

function toggleStatus(value: number) {
  const next = statuses.value.includes(value)
    ? statuses.value.filter((v) => v !== value)
    : [...statuses.value, value].sort((a, b) => a - b)
  statuses.value = next
  reload()
}

function pickDate(value: string) {
  date.value = value
  reload()
}

function clearKeyword() {
  keyword.value = ''
  reload()
}

function resetFilters() {
  keyword.value = ''
  date.value = ''
  stripScrollLeft.value = stripScrollLeft.value === 0 ? 0.1 : 0
  loadDefaultStatuses().then(reload)
}

onShow(() => {
  if (!list.value.length) {
    loadDefaultStatuses().then(reload)
  } else {
    reload()
  }
})

onPullDownRefresh(async () => {
  page.value = 1
  await fetchReserves(false)
  uni.stopPullDownRefresh()
})

onReachBottom(() => loadMore())

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
    reload()
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
        reload()
      } catch {
        // 错误提示已在请求层处理
      }
    },
  })
}
</script>

<style lang="scss" scoped>
.reserve-list {
  min-height: 100vh;
  background: #f4f6f9;
}

.topbar {
  display: flex;
  align-items: center;
  padding: 20rpx 28rpx 12rpx;
  background: #fff;
}

.search {
  flex: 1;
  display: flex;
  align-items: center;
  height: 72rpx;
  background: #f2f5f8;
  border-radius: 36rpx;
  padding: 0 24rpx;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  color: #1f2937;
  height: 100%;
}

.ph {
  color: #b7bfc9;
  font-size: 27rpx;
}

.search-clear {
  font-size: 26rpx;
  color: #b7bfc9;
  padding-left: 12rpx;
}

.create-btn {
  margin-left: 20rpx;
  height: 72rpx;
  padding: 0 32rpx;
  border-radius: 36rpx;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  color: #fff;
  font-size: 27rpx;
  display: flex;
  align-items: center;
}

.filters {
  background: #fff;
  padding: 8rpx 0 20rpx;
  box-shadow: 0 6rpx 16rpx rgba(15, 23, 42, 0.04);
}

.status-chips {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  padding: 12rpx 28rpx 8rpx;
  gap: 14rpx;
}

.chip {
  height: 56rpx;
  padding: 0 24rpx;
  border-radius: 28rpx;
  background: #f2f5f8;
  color: #5b6572;
  font-size: 25rpx;
  display: flex;
  align-items: center;
}

.chip-on {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 500;
}

.reset {
  margin-left: auto;
  font-size: 25rpx;
  color: #9ca3af;
}

.date-strip {
  white-space: nowrap;
  padding: 8rpx 0 4rpx;
}

.strip-inner {
  display: inline-flex;
  padding: 0 28rpx;
  gap: 16rpx;
}

.date-cell {
  min-width: 104rpx;
  padding: 12rpx 16rpx;
  border-radius: 14rpx;
  background: #f7f9fb;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

.date-on {
  background: #2563eb;
}

.dc-main {
  font-size: 26rpx;
  color: #1f2937;
  font-weight: 500;
}

.dc-sub {
  font-size: 21rpx;
  color: #9ca3af;
  margin-top: 4rpx;
}

.date-on .dc-main,
.date-on .dc-sub {
  color: #fff;
}

.list-body {
  padding: 24rpx 28rpx 0;
}

.list-foot {
  text-align: center;
  padding: 36rpx 0;
  font-size: 24rpx;
  color: #9ca3af;
}

.empty-sub {
  margin-top: 12rpx;
  font-size: 23rpx;
  color: #c2c9d1;
}
</style>
