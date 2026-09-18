<template>
  <view class="page">
    <view v-if="children.length > 1" class="filter-row">
      <scroll-view class="chip-scroll" scroll-x :show-scrollbar="false">
        <view class="chip-list">
          <view
            v-for="item in children"
            :key="item.id"
            class="chip"
            :class="{ 'chip-active': childId === item.id }"
            @click="changeChild(item.id)"
          >
            {{ item.name || `儿童#${item.id}` }}
          </view>
        </view>
      </scroll-view>
    </view>

    <template v-if="childId">
      <scroll-view v-if="records.length" class="record-scroll" scroll-x :show-scrollbar="false">
        <view class="record-list">
          <view
            v-for="row in records"
            :key="row.id"
            class="record-chip"
            :class="{ 'record-chip-active': record?.id === row.id }"
            @click="pickRecord(row)"
          >
            <text class="record-chip-date">{{ (row.careDate || '').slice(5) }}</text>
            <text class="record-chip-slot">{{ row.timeSlot || '—' }}</text>
          </view>
        </view>
      </scroll-view>

      <template v-if="record">
        <view class="card">
          <view class="report-head">
            <text class="report-name">{{ record.childName || `儿童#${record.childId}` }}</text>
            <text class="tag" :class="CARE_STATUS_MAP[record.status]?.tag || 'tag-grey'">
              {{ CARE_STATUS_MAP[record.status]?.label || '未知' }}
            </text>
          </view>
          <view class="report-meta">
            <text>{{ record.careDate || '—' }}</text>
            <text v-if="record.timeSlot" class="dot">·</text>
            <text v-if="record.timeSlot">{{ record.timeSlot }}</text>
            <text v-if="record.executorName" class="dot">·</text>
            <text v-if="record.executorName">{{ record.executorName }}</text>
          </view>
        </view>

        <view class="card">
          <view class="section-title">养护前后视力对比</view>
          <view class="vision-table">
            <view class="vt-head">
              <text class="vt-cell vt-key"></text>
              <text class="vt-cell">双眼</text>
              <text class="vt-cell">左眼</text>
              <text class="vt-cell">右眼</text>
            </view>
            <view class="vt-row">
              <text class="vt-cell vt-key">养护前</text>
              <text class="vt-cell">{{ displayVision(record.visionBeforeBoth) }}</text>
              <text class="vt-cell">{{ displayVision(record.visionBeforeLeft) }}</text>
              <text class="vt-cell">{{ displayVision(record.visionBeforeRight) }}</text>
            </view>
            <view class="vt-row">
              <text class="vt-cell vt-key">养护后</text>
              <text class="vt-cell vt-strong">{{ displayVision(record.visionAfterBoth) }}</text>
              <text class="vt-cell vt-strong">{{ displayVision(record.visionAfterLeft) }}</text>
              <text class="vt-cell vt-strong">{{ displayVision(record.visionAfterRight) }}</text>
            </view>
            <view class="vt-row">
              <text class="vt-cell vt-key">变化</text>
              <text class="vt-cell" :class="deltaClass(record.visionBeforeBoth, record.visionAfterBoth)">
                {{ deltaText(record.visionBeforeBoth, record.visionAfterBoth) }}
              </text>
              <text class="vt-cell" :class="deltaClass(record.visionBeforeLeft, record.visionAfterLeft)">
                {{ deltaText(record.visionBeforeLeft, record.visionAfterLeft) }}
              </text>
              <text class="vt-cell" :class="deltaClass(record.visionBeforeRight, record.visionAfterRight)">
                {{ deltaText(record.visionBeforeRight, record.visionAfterRight) }}
              </text>
            </view>
          </view>

          <view class="report-summary">{{ summaryText }}</view>
        </view>

        <view class="card">
          <view class="section-title">养护信息</view>
          <view class="kv"><text class="kv-key">执行医师/助理</text><text class="kv-value">{{ record.executorName || '—' }}</text></view>
          <view class="kv"><text class="kv-key">累计养护次数</text><text class="kv-value">{{ record.careCount ?? '—' }}</text></view>
          <view class="kv"><text class="kv-key">剩余可用次数</text><text class="kv-value">{{ record.remainingCount ?? '—' }}</text></view>
          <view class="kv"><text class="kv-key">备注</text><text class="kv-value">{{ record.remark || '—' }}</text></view>
        </view>
      </template>

      <view v-else class="empty">
        <text class="empty-icon">📋</text>
        <text class="empty-text">该儿童暂无养护记录</text>
      </view>
    </template>

    <view v-else-if="loading" class="loading-tip">加载中…</view>
    <view v-else class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无可查看的档案</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { careRecordApi } from '@/api/care-record'
import { useUserStore } from '@/stores/user'
import { displayVision, visionDelta } from '@/utils/format'
import { CARE_STATUS_MAP } from '@/utils/dict'
import type { CareRecord, Child } from '@/types'

const userStore = useUserStore()

const children = ref<Child[]>([])
const childId = ref(0)
const loading = ref(false)
const records = ref<CareRecord[]>([])
const record = ref<CareRecord | null>(null)

const summaryText = computed(() => {
  const row = record.value
  if (!row) return ''
  const text = visionDelta(row.visionBeforeBoth, row.visionAfterBoth)
  if (!text || text === '持平') return '本次养护后双眼视力与养护前持平'
  return text.startsWith('+')
    ? `本次养护后双眼视力提升 ${text.slice(1)}`
    : `本次养护后双眼视力变化 ${text}`
})

function deltaText(before?: string, after?: string) {
  return visionDelta(before, after) || '-'
}

function deltaClass(before?: string, after?: string) {
  const text = visionDelta(before, after)
  if (text === '持平') return 'vt-flat'
  if (text.startsWith('+')) return 'vt-up'
  if (text.startsWith('-')) return 'vt-down'
  return ''
}

function pickRecord(row: CareRecord) {
  record.value = row
}

function compareId(a: number, b: number) {
  const sa = String(a)
  const sb = String(b)
  return sa.length === sb.length ? sa.localeCompare(sb) : sa.length - sb.length
}

async function loadRecords(targetId: number, preferRecordId?: number) {
  const child = children.value.find((c) => c.id === targetId)
  const res = await careRecordApi.getRecordPage({ childId: targetId, storeId: child?.storeId, page: 1, size: 100 })
  records.value = (res?.list || []).sort((a, b) => {
    if (a.careDate !== b.careDate) return b.careDate.localeCompare(a.careDate)
    return compareId(b.id, a.id)
  })
  record.value = preferRecordId
    ? records.value.find((r) => r.id === preferRecordId) || records.value[0] || null
    : records.value[0] || null
}

async function changeChild(id: number) {
  if (childId.value === id) return
  childId.value = id
  record.value = null
  records.value = []
  await loadRecords(id)
}

onLoad(async (options) => {
  loading.value = true
  try {
    const res = await childApi.getChildList({ parentUserId: userStore.userId })
    children.value = res || []
    const fromOptions = Number(options?.childId || 0)
    const initial = fromOptions && children.value.some((c) => c.id === fromOptions)
      ? fromOptions
      : children.value[0]?.id || 0
    childId.value = initial
    if (initial) {
      await loadRecords(initial, Number(options?.recordId || 0) || undefined)
    }
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.filter-row {
  background: #fff;
  padding: 16rpx 0;
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

.record-scroll {
  white-space: nowrap;
  padding-bottom: 8rpx;
}

.record-list {
  display: inline-flex;
}

.record-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14rpx 24rpx;
  margin-right: 14rpx;
  background: #fff;
  border-radius: 14rpx;
  border: 2rpx solid transparent;
}

.record-chip-active {
  border-color: #14b8a6;
  background: #f0fdfa;
}

.record-chip-date {
  font-size: 26rpx;
  font-weight: 600;
  color: #1e293b;
}

.record-chip-slot {
  margin-top: 4rpx;
  font-size: 22rpx;
  color: #94a3b8;
}

.report-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.report-name {
  font-size: 34rpx;
  font-weight: 700;
  color: #0f172a;
}

.report-meta {
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

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16rpx;
}

.vision-table {
  border: 1rpx solid #eef1f5;
  border-radius: 12rpx;
  overflow: hidden;
}

.vt-head,
.vt-row {
  display: flex;
  align-items: center;
  border-bottom: 1rpx solid #eef1f5;
}

.vt-row:last-child {
  border-bottom: none;
}

.vt-head {
  background: #f8fafc;
}

.vt-cell {
  flex: 1;
  padding: 18rpx 8rpx;
  text-align: center;
  font-size: 26rpx;
  color: #475569;
}

.vt-key {
  flex: 0 0 130rpx;
  color: #94a3b8;
}

.vt-strong {
  font-weight: 600;
  color: #0d9488;
}

.vt-up {
  color: #14b8a6;
  font-weight: 600;
}

.vt-down {
  color: #ef4444;
  font-weight: 600;
}

.vt-flat {
  color: #94a3b8;
}

.report-summary {
  margin-top: 20rpx;
  padding: 18rpx 22rpx;
  font-size: 26rpx;
  color: #0d9488;
  background: #f0fdfa;
  border-radius: 12rpx;
}

.kv {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 0;
  font-size: 27rpx;
}

.kv-key {
  width: 200rpx;
  flex: none;
  color: #94a3b8;
}

.kv-value {
  flex: 1;
  color: #1e293b;
}
</style>
