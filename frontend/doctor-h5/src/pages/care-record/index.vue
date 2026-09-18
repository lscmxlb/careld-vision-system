<template>
  <view class="page">
    <view class="search-bar">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="儿童姓名 / 家长姓名 / 手机号"
          placeholder-class="ph"
          confirm-type="search"
          @confirm="reload"
        />
        <text v-if="keyword" class="search-clear" @click="clearKeyword">✕</text>
      </view>
      <view class="more-toggle" @click="showMoreFilter = !showMoreFilter">
        {{ showMoreFilter ? '收起' : '筛选' }}
      </view>
    </view>

    <view v-if="showMoreFilter" class="more-filter">
      <view class="mf-row">
        <text class="mf-label">儿童姓名</text>
        <input v-model="childName" class="mf-input" type="text" placeholder="不限" placeholder-class="ph" confirm-type="search" @confirm="reload" />
      </view>
      <view class="mf-row">
        <text class="mf-label">家长姓名</text>
        <input v-model="parentName" class="mf-input" type="text" placeholder="不限" placeholder-class="ph" confirm-type="search" @confirm="reload" />
      </view>
      <view class="mf-row">
        <text class="mf-label">养护次数大于</text>
        <input v-model.number="minCareCount" class="mf-input" type="number" placeholder="不限" placeholder-class="ph" @confirm="reload" />
        <text class="mf-unit">次</text>
      </view>
      <view class="mf-actions">
        <view class="btn btn-sm btn-plain" @click="reset">重置</view>
        <view class="btn btn-sm btn-primary" @click="reload">查询</view>
      </view>
    </view>

    <view v-if="list.length" class="list">
      <view v-for="row in list" :key="row.id" class="card record-card" @click="goDetail(row)">
        <view class="rc-head">
          <view class="rc-name-wrap">
            <text class="rc-name">{{ row.childName || `儿童#${row.childId}` }}</text>
            <text class="tag" :class="row.childGender === 1 ? 'tag-primary' : 'tag-grey'">
              {{ row.childGender === 1 ? '男' : row.childGender === 0 ? '女' : '—' }}
            </text>
          </view>
          <text class="tag" :class="CARE_STATUS_MAP[row.status]?.tag || 'tag-grey'">
            {{ CARE_STATUS_MAP[row.status]?.label || '未知' }}
          </text>
        </view>

        <view class="rc-time">
          <text class="rc-date">{{ row.careDate || '—' }}</text>
          <text v-if="row.timeSlot" class="rc-slot">{{ row.timeSlot }}</text>
        </view>

        <view class="rc-meta">
          <text class="rc-meta-item">{{ row.childPhone || '—' }}</text>
          <text class="rc-meta-item">养护 {{ row.careCount ?? 0 }} 次</text>
          <text class="rc-meta-item">剩余 {{ row.remainingCount ?? 0 }} 次</text>
        </view>

        <view class="rc-vision">
          <view class="rc-vision-item">
            <text class="rc-vision-label">首次视力</text>
            <text class="rc-vision-value">{{ displayVision(row.nakedVisionBoth) }}</text>
          </view>
          <text class="rc-vision-arrow">→</text>
          <view class="rc-vision-item">
            <text class="rc-vision-label">当前视力</text>
            <text class="rc-vision-value rc-vision-after">{{ displayVision(row.visionAfterBoth) }}</text>
          </view>
          <text v-if="row.executorName" class="rc-executor">{{ row.executorName }}</text>
        </view>
      </view>

      <view v-if="hasMore" class="loading-tip">{{ loading ? '加载中…' : '上拉加载更多' }}</view>
      <view v-else class="loading-tip">— 共 {{ total }} 条 —</view>
    </view>

    <view v-else-if="loading" class="loading-tip">加载中…</view>
    <view v-else class="empty">
      <text class="empty-icon">📈</text>
      <text class="empty-text">暂无养护记录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { careRecordApi } from '@/api/care-record'
import { useUserStore } from '@/stores/user'
import { displayVision } from '@/utils/format'
import { CARE_STATUS_MAP } from '@/utils/dict'
import type { CareRecord } from '@/types'

const userStore = useUserStore()

const keyword = ref('')
const childName = ref('')
const parentName = ref('')
const minCareCount = ref<number | undefined>(undefined)
const showMoreFilter = ref(false)

const list = ref<CareRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const hasMore = ref(true)

async function load(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (reset) page.value = 1
    const res = await careRecordApi.getRecordPage({
      storeId: userStore.storeId,
      childName: childName.value || undefined,
      parentName: parentName.value || undefined,
      phone: keyword.value || undefined,
      minCareCount: minCareCount.value,
      page: page.value,
      size,
    })
    const rows = res?.list || []
    list.value = reset ? rows : [...list.value, ...rows]
    total.value = res?.pagination?.total ?? list.value.length
    hasMore.value = list.value.length < total.value
  } finally {
    loading.value = false
  }
}

function reload() {
  load(true)
}

function clearKeyword() {
  keyword.value = ''
  reload()
}

function reset() {
  keyword.value = ''
  childName.value = ''
  parentName.value = ''
  minCareCount.value = undefined
  reload()
}

function goDetail(row: CareRecord) {
  uni.navigateTo({ url: `/pages/care-record/detail?id=${row.id}` })
}

onShow(() => {
  reload()
})

onPullDownRefresh(async () => {
  await load(true)
  uni.stopPullDownRefresh()
})

onReachBottom(() => {
  if (!hasMore.value || loading.value) return
  page.value += 1
  load()
})
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 60rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx 0;
  background: #fff;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f4f6f9;
  border-radius: 36rpx;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #1e293b;
}

.ph {
  color: #94a3b8;
}

.search-clear {
  width: 40rpx;
  text-align: center;
  color: #94a3b8;
  font-size: 26rpx;
}

.more-toggle {
  margin-left: 20rpx;
  font-size: 26rpx;
  color: #2563eb;
}

.more-filter {
  padding: 16rpx 24rpx 20rpx;
  background: #fff;
}

.mf-row {
  display: flex;
  align-items: center;
  min-height: 80rpx;
  border-bottom: 1rpx solid #f8fafc;
}

.mf-label {
  width: 200rpx;
  flex: none;
  font-size: 27rpx;
  color: #475569;
}

.mf-input {
  flex: 1;
  font-size: 27rpx;
  color: #1e293b;
  text-align: right;
}

.mf-unit {
  margin-left: 10rpx;
  font-size: 26rpx;
  color: #64748b;
}

.mf-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20rpx;
}

.mf-actions .btn {
  margin-left: 16rpx;
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
}

.rc-name {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
  margin-right: 12rpx;
}

.rc-time {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
}

.rc-date {
  font-size: 28rpx;
  color: #1e293b;
  margin-right: 16rpx;
}

.rc-slot {
  font-size: 26rpx;
  color: #1d4ed8;
  font-weight: 600;
}

.rc-meta {
  display: flex;
  flex-wrap: wrap;
  margin-top: 12rpx;
}

.rc-meta-item {
  font-size: 24rpx;
  color: #94a3b8;
  margin-right: 24rpx;
}

.rc-vision {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f1f5f9;
}

.rc-vision-item {
  display: flex;
  flex-direction: column;
}

.rc-vision-label {
  font-size: 22rpx;
  color: #94a3b8;
}

.rc-vision-value {
  margin-top: 4rpx;
  font-size: 30rpx;
  font-weight: 600;
  color: #475569;
}

.rc-vision-after {
  color: #1d4ed8;
}

.rc-vision-arrow {
  margin: 0 24rpx;
  font-size: 28rpx;
  color: #cbd5e1;
  padding-top: 20rpx;
}

.rc-executor {
  margin-left: auto;
  font-size: 24rpx;
  color: #94a3b8;
  padding-top: 20rpx;
}
</style>
