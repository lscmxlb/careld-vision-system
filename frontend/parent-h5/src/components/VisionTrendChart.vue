<template>
  <view class="trend-root">
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
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { careRecordApi } from '@/api/care-record'
import { parseVision, parseVisionValue } from '@/utils/format'
import type { CareRecord } from '@/types'

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

const props = defineProps<{
  childId: number
  storeId?: number
}>()

const emit = defineEmits<{
  (e: 'loaded', records: CareRecord[]): void
}>()

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

function compareId(a: number, b: number) {
  const sa = String(a)
  const sb = String(b)
  return sa.length === sb.length ? sa.localeCompare(sb) : sa.length - sb.length
}

async function load() {
  if (!props.childId) return
  loading.value = true
  try {
    const all = await careRecordApi.getRecordsByChild(props.childId, props.storeId)
    // 接口按日期倒序返回，图表按时间升序（第 1 次在最左）
    records.value = [...(all || [])].sort((a, b) =>
      a.careDate === b.careDate ? compareId(a.id, b.id) : a.careDate.localeCompare(b.careDate),
    )
    emit('loaded', records.value)
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

watch(() => [props.childId, props.storeId], load, { immediate: true })
</script>

<style lang="scss" scoped>
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
