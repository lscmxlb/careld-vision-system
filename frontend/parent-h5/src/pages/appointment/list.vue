<template>
  <view class="page">
    <view class="filter-row">
      <scroll-view class="chip-scroll" scroll-x :show-scrollbar="false">
        <view class="chip-list">
          <view
            v-for="opt in FILTERS"
            :key="opt.label"
            class="chip"
            :class="{ 'chip-active': statusFilter === opt.value }"
            @click="changeFilter(opt.value)"
          >
            {{ opt.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view v-if="list.length" class="list">
      <view v-for="item in visibleList" :key="item.id" class="card reserve-card">
        <view class="rc-head">
          <text class="rc-name">{{ item.childName || '未知儿童' }}</text>
          <text class="tag" :class="statusTag(item)">{{ statusLabel(item) }}</text>
        </view>

        <view class="rc-time">
          <text class="rc-date">{{ item.scheduleDate }} {{ weekdayLabel(item.scheduleDate) }}</text>
          <text class="rc-slot">{{ formatHm(item.timeSlotStart) }} - {{ formatHm(item.timeSlotEnd) }}</text>
          <text v-if="item.adjustFlag === 1" class="rc-adjusted">已调整</text>
        </view>

        <view v-if="item.executorName" class="rc-meta">
          <text class="rc-meta-item">养护人：{{ item.executorName }}</text>
        </view>
        <view v-if="item.status === 4 && item.cancelReason" class="rc-meta">
          <text class="rc-meta-item rc-reason">取消原因：{{ item.cancelReason }}</text>
        </view>
        <view v-if="statusValue(item) === 5" class="rc-noshow-tip">已爽约，本次预约次数不退还</view>
        <view v-if="item.remark" class="rc-meta">
          <text class="rc-meta-item">备注：{{ item.remark }}</text>
        </view>

        <view v-if="item.status === 1" class="rc-actions">
          <view class="btn btn-sm btn-danger-plain" :class="{ 'is-disabled': !cancelable(item) }" @click="onCancel(item)">
            取消预约
          </view>
        </view>
      </view>

      <view v-if="visibleCount < list.length" class="loading-tip" @click="showMore">
        上拉加载更多（{{ list.length - visibleCount }}）
      </view>
      <view v-else class="loading-tip">— 共 {{ list.length }} 条 —</view>
    </view>

    <view v-else-if="loading" class="loading-tip">加载中…</view>
    <view v-else class="empty">
      <text class="empty-icon">📅</text>
      <text class="empty-text">暂无预约记录</text>
      <view class="empty-btn" @click="goAppointment">去预约养护</view>
    </view>

    <CancelSheet v-model:visible="cancelVisible" :row="cancelRow" :loading="cancelLoading" @confirm="submitCancel" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import CancelSheet from '@/components/CancelSheet.vue'
import { childApi } from '@/api/child'
import { appointmentConfigApi, reserveApi } from '@/api/reserve'
import { useUserStore } from '@/stores/user'
import { reserveStatusLabel, reserveStatusTag, reserveStatusValue } from '@/utils/dict'
import { formatHm, weekdayLabel } from '@/utils/format'
import { toast } from '@/utils/request'
import type { Child, Reserve } from '@/types'

const PAGE_SIZE = 20
const ACTIVE_STATUS = [1, 2]

const FILTERS = [
  { value: undefined as number | undefined, label: '全部' },
  { value: 1, label: '已预约' },
  { value: 2, label: '养护中' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已爽约' },
]

const userStore = useUserStore()

const statusFilter = ref<number | undefined>(undefined)
const list = ref<Reserve[]>([])
const loading = ref(false)
const visibleCount = ref(PAGE_SIZE)
/** 各门店家长取消时间窗（小时），键为 storeId */
const cancelHoursByStore = ref<Record<number, number>>({})
const children = ref<Child[]>([])
const DEFAULT_CANCEL_HOURS = 24

const cancelVisible = ref(false)
const cancelRow = ref<Reserve | null>(null)
const cancelLoading = ref(false)

const visibleList = computed(() => list.value.slice(0, visibleCount.value))

function statusValue(item: Reserve) {
  return reserveStatusValue(item)
}

function statusLabel(item: Reserve) {
  return reserveStatusLabel(item)
}

function statusTag(item: Reserve) {
  return reserveStatusTag(item)
}

function compareId(a: number | string, b: number | string) {
  const sa = String(a)
  const sb = String(b)
  return sa.length === sb.length ? sa.localeCompare(sb) : sa.length - sb.length
}

/** 待办（已预约/养护中）升序在前，历史记录倒序在后 */
function sortRows(rows: Reserve[]) {
  return [...rows].sort((a, b) => {
    const aActive = ACTIVE_STATUS.includes(a.status) ? 0 : 1
    const bActive = ACTIVE_STATUS.includes(b.status) ? 0 : 1
    if (aActive !== bActive) return aActive - bActive
    const ta = `${a.scheduleDate} ${formatHm(a.timeSlotStart)}`
    const tb = `${b.scheduleDate} ${formatHm(b.timeSlotStart)}`
    if (ta !== tb) return aActive === 0 ? ta.localeCompare(tb) : tb.localeCompare(ta)
    return aActive === 0 ? compareId(a.id, b.id) : compareId(b.id, a.id)
  })
}

async function load() {
  loading.value = true
  try {
    children.value = (await childApi.getChildList({ parentUserId: userStore.userId })) || []
    const childIds = children.value.map((c) => c.id)
    if (!childIds.length) {
      list.value = []
      return
    }
    await loadCancelHours()
    const results = await Promise.all(
      childIds.map((childId) =>
        reserveApi.getReserveList({
          childId,
          statuses: statusFilter.value != null ? String(statusFilter.value) : undefined,
          page: 1,
          size: 50,
        }),
      ),
    )
    list.value = sortRows(results.flatMap((r) => r?.list || []))
    visibleCount.value = PAGE_SIZE
  } finally {
    loading.value = false
  }
}

/** 取消时间窗按门店配置读取（家长无门店上下文，需显式传 storeId） */
async function loadCancelHours() {
  const storeIds = Array.from(new Set(children.value.map((c) => c.storeId).filter((id): id is number => id != null)))
  await Promise.all(
    storeIds.map(async (storeId) => {
      if (cancelHoursByStore.value[storeId] != null) return
      try {
        const config = await appointmentConfigApi.getConfig(storeId)
        cancelHoursByStore.value[storeId] = config?.parentCancelHours ?? DEFAULT_CANCEL_HOURS
      } catch {
        cancelHoursByStore.value[storeId] = DEFAULT_CANCEL_HOURS
      }
    }),
  )
}

function cancelHoursFor(item: Reserve): number {
  const storeId = children.value.find((c) => c.id === item.childId)?.storeId
  return (storeId != null && cancelHoursByStore.value[storeId]) || DEFAULT_CANCEL_HOURS
}

function reload() {
  visibleCount.value = PAGE_SIZE
  load()
}

function changeFilter(value?: number) {
  if (statusFilter.value === value) return
  statusFilter.value = value
  list.value = []
  reload()
}

function showMore() {
  visibleCount.value = Math.min(visibleCount.value + PAGE_SIZE, list.value.length)
}

/** 取消时间窗：距预约开始不足家长取消小时数时不可取消 */
function cancelable(item: Reserve): boolean {
  if (item.status !== 1) return false
  const start = new Date(`${item.scheduleDate}T${(item.timeSlotStart || '00:00:00').slice(0, 8)}`)
  if (Number.isNaN(start.getTime())) return false
  if (start.getTime() <= Date.now()) return false
  return Date.now() <= start.getTime() - cancelHoursFor(item) * 3600000
}

function cancelTip(item: Reserve): string {
  const start = new Date(`${item.scheduleDate}T${(item.timeSlotStart || '00:00:00').slice(0, 8)}`)
  if (Number.isNaN(start.getTime()) || start.getTime() <= Date.now()) return '已超过预约时段，不可取消'
  return `距预约开始不足 ${cancelHoursFor(item)} 小时，不可取消`
}

function onCancel(item: Reserve) {
  if (!cancelable(item)) {
    toast(cancelTip(item))
    return
  }
  cancelRow.value = item
  cancelVisible.value = true
}

async function submitCancel(reason: string) {
  if (!cancelRow.value || cancelLoading.value) return
  cancelLoading.value = true
  try {
    await reserveApi.cancelReserve(cancelRow.value.id, reason)
    cancelVisible.value = false
    uni.showToast({ title: '预约已取消，次数已退还', icon: 'none', duration: 2500 })
    await load()
  } catch {
    // 错误提示已在请求层处理
  } finally {
    cancelLoading.value = false
  }
}

function goAppointment() {
  uni.switchTab({ url: '/pages/appointment/index' })
}

onShow(() => {
  reload()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

onReachBottom(showMore)
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.filter-row {
  background: #fff;
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.chip-scroll {
  white-space: nowrap;
}

.chip-list {
  display: inline-flex;
}

.chip {
  display: inline-block;
  padding: 10rpx 26rpx;
  margin-right: 12rpx;
  font-size: 26rpx;
  color: #475569;
  background: #f1f5f9;
  border-radius: 30rpx;
}

.chip-active {
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
}

.list {
  padding: 20rpx 24rpx;
}

.reserve-card {
  margin-bottom: 20rpx;
}

.rc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rc-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.rc-time {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  font-size: 28rpx;
}

.rc-date {
  color: #475569;
  margin-right: 16rpx;
}

.rc-slot {
  font-weight: 600;
  color: #0d9488;
}

.rc-adjusted {
  margin-left: 16rpx;
  font-size: 22rpx;
  color: #3b82f6;
}

.rc-meta {
  display: flex;
  flex-wrap: wrap;
  margin-top: 12rpx;
}

.rc-meta-item {
  font-size: 24rpx;
  color: #94a3b8;
}

.rc-reason {
  color: #e07a5f;
}

.rc-noshow-tip {
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 8rpx;
}

.rc-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f1f5f9;
}

.empty-btn {
  margin-top: 32rpx;
  padding: 20rpx 64rpx;
  font-size: 28rpx;
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
  border-radius: 44rpx;
}
</style>
