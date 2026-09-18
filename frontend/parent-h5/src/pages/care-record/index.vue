<template>
  <view class="page">
    <view class="filter-row">
      <scroll-view class="chip-scroll" scroll-x :show-scrollbar="false">
        <view class="chip-list">
          <view
            v-for="opt in childFilters"
            :key="String(opt.value)"
            class="chip"
            :class="{ 'chip-active': childId === opt.value }"
            @click="changeChild(opt.value)"
          >
            {{ opt.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view v-if="list.length" class="list">
      <view v-for="row in visibleList" :key="row.id" class="card record-card" @click="goDetail(row)">
        <view class="rc-head">
          <view class="rc-name-wrap">
            <text class="rc-name">{{ row.childName || `儿童#${row.childId}` }}</text>
            <text class="tag" :class="CARE_STATUS_MAP[row.status]?.tag || 'tag-grey'">
              {{ CARE_STATUS_MAP[row.status]?.label || '未知' }}
            </text>
          </view>
          <text class="rc-date">{{ row.careDate || '—' }}</text>
        </view>

        <view class="rc-time">
          <text v-if="row.timeSlot" class="rc-slot">实际养护 {{ row.timeSlot }}</text>
          <text v-if="row.executorName" class="rc-executor">{{ row.executorName }}</text>
        </view>

        <view class="rc-vision">
          <view class="rv-col">
            <text class="rv-title">养护前</text>
            <view class="rv-line"><text class="rv-key">双眼</text><text class="rv-val">{{ displayVision(row.visionBeforeBoth) }}</text></view>
            <view class="rv-line"><text class="rv-key">左眼</text><text class="rv-val">{{ displayVision(row.visionBeforeLeft) }}</text></view>
            <view class="rv-line"><text class="rv-key">右眼</text><text class="rv-val">{{ displayVision(row.visionBeforeRight) }}</text></view>
          </view>
          <view class="rv-arrow">→</view>
          <view class="rv-col">
            <text class="rv-title">养护后</text>
            <view class="rv-line"><text class="rv-key">双眼</text><text class="rv-val rv-after">{{ displayVision(row.visionAfterBoth) }}</text></view>
            <view class="rv-line"><text class="rv-key">左眼</text><text class="rv-val rv-after">{{ displayVision(row.visionAfterLeft) }}</text></view>
            <view class="rv-line"><text class="rv-key">右眼</text><text class="rv-val rv-after">{{ displayVision(row.visionAfterRight) }}</text></view>
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
      <text class="empty-icon">📈</text>
      <text class="empty-text">暂无养护记录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { careRecordApi } from '@/api/care-record'
import { childApi } from '@/api/child'
import { useUserStore } from '@/stores/user'
import { displayVision } from '@/utils/format'
import { CARE_STATUS_MAP } from '@/utils/dict'
import type { CareRecord, Child } from '@/types'

const PAGE_SIZE = 10

const userStore = useUserStore()

const children = ref<Child[]>([])
const childId = ref<number | undefined>(undefined)
const list = ref<CareRecord[]>([])
const loading = ref(false)
const visibleCount = ref(PAGE_SIZE)

const childFilters = computed(() => [
  { value: undefined as number | undefined, label: '全部' },
  ...children.value.map((c) => ({ value: c.id as number | undefined, label: c.name || `儿童#${c.id}` })),
])

const visibleList = computed(() => list.value.slice(0, visibleCount.value))

function compareId(a: number, b: number) {
  const sa = String(a)
  const sb = String(b)
  return sa.length === sb.length ? sa.localeCompare(sb) : sa.length - sb.length
}

async function load() {
  loading.value = true
  try {
    if (!children.value.length) {
      const res = await childApi.getChildList({ parentUserId: userStore.userId })
      children.value = res || []
    }
    const targets = childId.value
      ? children.value.filter((c) => c.id === childId.value)
      : children.value
    if (!targets.length) {
      list.value = []
      return
    }
    const results = await Promise.all(
      targets.map((c) => careRecordApi.getRecordPage({ childId: c.id, page: 1, size: 100 })),
    )
    list.value = results
      .flatMap((r) => r?.list || [])
      .sort((a, b) => {
        if (a.careDate !== b.careDate) return b.careDate.localeCompare(a.careDate)
        return compareId(b.id, a.id)
      })
    visibleCount.value = PAGE_SIZE
  } finally {
    loading.value = false
  }
}

function reload() {
  visibleCount.value = PAGE_SIZE
  load()
}

function changeChild(value?: number) {
  if (childId.value === value) return
  childId.value = value
  list.value = []
  reload()
}

function showMore() {
  visibleCount.value = Math.min(visibleCount.value + PAGE_SIZE, list.value.length)
}

function goDetail(row: CareRecord) {
  uni.navigateTo({ url: `/pages/care-record/detail?id=${row.id}` })
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

.record-card {
  margin-bottom: 20rpx;
}

.rc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rc-name-wrap {
  display: flex;
  align-items: center;
  min-width: 0;
}

.rc-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
  margin-right: 12rpx;
}

.rc-date {
  font-size: 26rpx;
  color: #64748b;
}

.rc-time {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12rpx;
}

.rc-slot {
  font-size: 26rpx;
  color: #0d9488;
  font-weight: 600;
}

.rc-executor {
  font-size: 24rpx;
  color: #94a3b8;
}

.rc-vision {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f1f5f9;
}

.rv-col {
  flex: 1;
}

.rv-title {
  display: block;
  font-size: 22rpx;
  color: #94a3b8;
  margin-bottom: 8rpx;
}

.rv-line {
  display: flex;
  align-items: center;
  margin-top: 4rpx;
}

.rv-key {
  width: 70rpx;
  flex: none;
  font-size: 24rpx;
  color: #94a3b8;
}

.rv-val {
  font-size: 28rpx;
  font-weight: 600;
  color: #475569;
}

.rv-after {
  color: #0d9488;
}

.rv-arrow {
  flex: none;
  margin: 0 20rpx;
  font-size: 28rpx;
  color: #cbd5e1;
}
</style>
