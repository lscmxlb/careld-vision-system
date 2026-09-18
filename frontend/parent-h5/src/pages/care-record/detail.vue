<template>
  <view class="page">
    <view v-if="record" class="hero">
      <view class="hero-top">
        <text class="hero-name">{{ record.childName || `儿童#${record.childId}` }}</text>
        <text class="tag" :class="CARE_STATUS_MAP[record.status]?.tag || 'tag-grey'">
          {{ CARE_STATUS_MAP[record.status]?.label || '未知' }}
        </text>
      </view>
      <view class="hero-meta">
        <text>{{ record.careDate || '—' }}</text>
        <text v-if="record.timeSlot" class="hero-dot">·</text>
        <text v-if="record.timeSlot">{{ record.timeSlot }}</text>
        <text v-if="record.executorName" class="hero-dot">·</text>
        <text v-if="record.executorName">{{ record.executorName }}</text>
      </view>
    </view>

    <!-- 本次养护视力 -->
    <view v-if="record" class="card">
      <view class="section-title">本次养护视力</view>
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
    </view>

    <!-- 儿童档案信息 -->
    <view v-if="child" class="card">
      <view class="section-title">儿童基本信息[档案编号：{{ child.childCode || '-' }}]</view>
      <view class="kv"><text class="kv-key">儿童姓名</text><text class="kv-value">{{ child.name || '-' }}</text></view>
      <view class="kv"><text class="kv-key">性别</text><text class="kv-value">{{ child.gender === 1 ? '男' : '女' }}</text></view>
      <view class="kv">
        <text class="kv-key">出生日期</text>
        <text class="kv-value">{{ child.birthDate || '-' }}<text v-if="child.birthDate">（{{ child.age }}岁）</text></text>
      </view>
      <view class="kv"><text class="kv-key">所在学校</text><text class="kv-value">{{ child.school || '-' }}</text></view>
      <view class="kv"><text class="kv-key">家长姓名</text><text class="kv-value">{{ child.parentName || '-' }}</text></view>
      <view class="kv"><text class="kv-key">关系</text><text class="kv-value">{{ child.relation || '-' }}</text></view>
      <view class="kv"><text class="kv-key">手机号码</text><text class="kv-value">{{ child.phone || '-' }}</text></view>
      <view class="kv"><text class="kv-key">家庭地址</text><text class="kv-value">{{ child.homeAddress || '-' }}</text></view>
      <view class="kv"><text class="kv-key">视力状况</text><text class="kv-value">{{ child.eyeCondition || '-' }}</text></view>
      <view class="kv">
        <text class="kv-key">裸眼视力</text>
        <text class="kv-value">
          双眼 {{ displayVision(child.nakedVisionBoth) }}　左眼 {{ displayVision(child.nakedVisionLeft) }}　右眼 {{ displayVision(child.nakedVisionRight) }}
        </text>
      </view>
      <view class="kv"><text class="kv-key">主治医师</text><text class="kv-value">{{ child.doctorName || '-' }}</text></view>
      <view class="kv"><text class="kv-key">累计养护次数</text><text class="kv-value">{{ record?.careCount ?? '-' }}</text></view>
      <view class="kv"><text class="kv-key">可用次数</text><text class="kv-value">{{ child.remainingCount ?? 0 }}</text></view>
    </view>

    <!-- 关联入口 -->
    <view v-if="record" class="card entry-card">
      <view class="entry-row" @click="goTrend">
        <text class="entry-label">历次养护视力趋势</text>
        <text class="entry-arrow">›</text>
      </view>
      <view class="entry-row" @click="goReport">
        <text class="entry-label">本次养护检测报告</text>
        <text class="entry-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { careRecordApi } from '@/api/care-record'
import { displayVision, visionDelta } from '@/utils/format'
import { CARE_STATUS_MAP } from '@/utils/dict'
import type { CareRecord, Child } from '@/types'

const recordId = ref(0)
const record = ref<CareRecord | null>(null)
const child = ref<Child | null>(null)
const loading = ref(false)

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

function goTrend() {
  if (child.value) uni.navigateTo({ url: `/pages/trend/index?childId=${child.value.id}` })
}

function goReport() {
  uni.navigateTo({ url: `/pages/report/index?childId=${record.value?.childId}&recordId=${recordId.value}` })
}

onLoad(async (options) => {
  recordId.value = Number(options?.id || 0)
  if (!recordId.value) return
  loading.value = true
  try {
    record.value = await careRecordApi.getRecordDetail(recordId.value)
    child.value = await childApi.getChildDetail(record.value.childId)
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 60rpx;
}

.hero {
  padding: 32rpx;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6, #0d9488);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-name {
  font-size: 38rpx;
  font-weight: 700;
  color: #fff;
}

.hero-meta {
  display: flex;
  align-items: center;
  margin-top: 14rpx;
  font-size: 27rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-dot {
  margin: 0 12rpx;
  color: rgba(255, 255, 255, 0.5);
}

.hero .tag {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
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

.kv {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 0;
  font-size: 27rpx;
}

.kv-key {
  width: 190rpx;
  flex: none;
  color: #94a3b8;
}

.kv-value {
  flex: 1;
  color: #1e293b;
}

.entry-card {
  padding: 8rpx 26rpx;
}

.entry-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
}

.entry-row:last-child {
  border-bottom: none;
}

.entry-label {
  font-size: 28rpx;
  color: #1e293b;
}

.entry-arrow {
  font-size: 34rpx;
  color: #cbd5e1;
}
</style>
