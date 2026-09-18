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

    <!-- 视力趋势 -->
    <view class="card">
      <view class="section-title">养护视力趋势</view>

      <view v-if="chartGroups.length" class="legend">
        <view v-for="item in LEGEND" :key="item.name" class="legend-item">
          <view class="legend-dot" :style="{ background: item.color }"></view>
          <text class="legend-text">{{ item.name }}</text>
        </view>
      </view>

      <scroll-view v-if="chartGroups.length" class="trend-scroll" scroll-x :show-scrollbar="false">
        <view class="trend-inner" :style="{ width: chartGroups.length * 320 + 'rpx' }">
          <view class="trend-axis">
            <text class="trend-axis-label">视力</text>
            <text class="trend-axis-max">{{ axisMax.toFixed(1) }}</text>
            <text class="trend-axis-min">{{ axisMin.toFixed(1) }}</text>
          </view>
          <view class="trend-cols">
            <view v-for="(group, gi) in chartGroups" :key="gi" class="trend-col">
              <view class="trend-bars">
                <view v-for="(bar, bi) in group.bars" :key="bi" class="trend-bar-slot" :class="{ 'slot-gap': bi % 2 === 1 }">
                  <text class="trend-bar-value">{{ bar.label }}</text>
                  <view class="trend-bar" :style="{ height: bar.height + '%', background: bar.color }"></view>
                </view>
              </view>
              <view class="trend-x">
                <text class="trend-x-line">{{ group.careDate }}</text>
                <text class="trend-x-line">{{ group.timeSlot || '-' }}</text>
                <text class="trend-x-line">{{ group.executorName || '-' }}</text>
              </view>
            </view>
          </view>
        </view>
      </scroll-view>

      <view v-else-if="loading" class="loading-tip">加载中…</view>
      <view v-else class="empty"><text class="empty-icon">📈</text><text class="empty-text">暂无养护记录</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { careRecordApi } from '@/api/care-record'
import { displayVision, parseVision, parseVisionValue, visionDelta } from '@/utils/format'
import { CARE_STATUS_MAP } from '@/utils/dict'
import type { CareRecord, Child } from '@/types'

const SERIES = [
  { key: 'visionBeforeBoth', name: '前·双眼', color: '#a0cfff' },
  { key: 'visionAfterBoth', name: '后·双眼', color: '#409eff' },
  { key: 'visionBeforeLeft', name: '前·左眼', color: '#f3d19e' },
  { key: 'visionAfterLeft', name: '后·左眼', color: '#e6a23c' },
  { key: 'visionBeforeRight', name: '前·右眼', color: '#b3e19d' },
  { key: 'visionAfterRight', name: '后·右眼', color: '#67c23a' },
] as const

const LEGEND = [
  { name: '双眼 前/后', color: '#a0cfff' },
  { name: '左眼 前/后', color: '#f3d19e' },
  { name: '右眼 前/后', color: '#b3e19d' },
]

const recordId = ref(0)
const record = ref<CareRecord | null>(null)
const child = ref<Child | null>(null)
const records = ref<CareRecord[]>([])
const loading = ref(false)

/** Y 轴下限：最小值再下探 0.2（与 PC 端一致），上限固定 5.3 */
const axisMin = computed(() => {
  const values = records.value
    .flatMap((r) => SERIES.map((s) => parseVisionValue((r as unknown as Record<string, string | undefined>)[s.key])))
    .filter((v): v is number => v !== null)
  if (!values.length) return 4.0
  return Math.max(4.0, Math.floor((Math.min(...values) - 0.2) * 10) / 10)
})
const axisMax = 5.3

const chartGroups = computed(() =>
  records.value.map((r) => {
    const row = r as unknown as Record<string, string | undefined>
    return {
      careDate: r.careDate,
      timeSlot: r.timeSlot,
      executorName: r.executorName,
      bars: SERIES.map((s) => {
        const value = parseVisionValue(row[s.key])
        const height = value === null ? 2 : Math.min(100, Math.max(2, ((value - axisMin.value) / (axisMax - axisMin.value)) * 100))
        return { label: parseVision(row[s.key]).main || '-', height, color: s.color }
      }),
    }
  }),
)

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

onLoad(async (options) => {
  recordId.value = Number(options?.id || 0)
  if (!recordId.value) return
  loading.value = true
  try {
    record.value = await careRecordApi.getRecordDetail(recordId.value)
    const childId = record.value.childId
    const [childDetail, all] = await Promise.all([
      childApi.getChildDetail(childId),
      careRecordApi.getRecordsByChild(childId, record.value.storeId),
    ])
    child.value = childDetail
    // 接口按日期倒序返回，图表按时间升序（第 1 次在最左）
    records.value = [...(all || [])].sort((a, b) =>
      a.careDate === b.careDate ? a.id - b.id : a.careDate.localeCompare(b.careDate),
    )
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
  background: linear-gradient(135deg, #60a5fa, #2563eb, #1d4ed8);
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
  color: #1d4ed8;
}

.vt-up {
  color: #2563eb;
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

.legend {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 16rpx;
}

.legend-item {
  display: flex;
  align-items: center;
  margin: 0 24rpx 8rpx 0;
}

.legend-dot {
  width: 18rpx;
  height: 18rpx;
  border-radius: 4rpx;
  margin-right: 8rpx;
}

.legend-text {
  font-size: 22rpx;
  color: #64748b;
}

.trend-scroll {
  width: 100%;
  padding-bottom: 8rpx;
}

.trend-inner {
  display: flex;
  align-items: flex-end;
}

.trend-axis {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-end;
  width: 80rpx;
  flex: none;
  height: 520rpx;
  padding-right: 10rpx;
  box-sizing: border-box;
  position: sticky;
  left: 0;
  background: #fff;
}

.trend-axis-label,
.trend-axis-max,
.trend-axis-min {
  font-size: 20rpx;
  color: #94a3b8;
}

.trend-cols {
  display: flex;
  align-items: flex-end;
}

.trend-col {
  width: 320rpx;
  flex: none;
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 480rpx;
  border-bottom: 1rpx solid #e2e8f0;
}

.trend-bar-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  width: 42rpx;
  height: 100%;
  margin-right: 6rpx;
}

.slot-gap {
  margin-right: 18rpx;
}

.trend-bar-value {
  font-size: 20rpx;
  color: #64748b;
  margin-bottom: 4rpx;
}

.trend-bar {
  width: 34rpx;
  border-radius: 4rpx 4rpx 0 0;
  min-height: 4rpx;
}

.trend-x {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12rpx 0 0;
}

.trend-x-line {
  font-size: 20rpx;
  color: #94a3b8;
  line-height: 1.5;
}

.trend-x-line:first-child {
  color: #475569;
  font-weight: 600;
}
</style>
