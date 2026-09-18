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
      <view class="card summary-card">
        <view class="sum-item">
          <text class="sum-label">首次养护前</text>
          <text class="sum-value">{{ displayVision(firstVision) }}</text>
        </view>
        <view class="sum-item">
          <text class="sum-label">最近养护后</text>
          <text class="sum-value sum-after">{{ displayVision(lastVision) }}</text>
        </view>
        <view class="sum-item">
          <text class="sum-label">累计变化（双眼）</text>
          <text class="sum-value" :class="deltaClass">{{ deltaText }}</text>
        </view>
      </view>

      <view class="card">
        <view class="section-title">历次养护视力趋势</view>
        <VisionTrendChart :child-id="childId" @loaded="onLoaded" />
      </view>
    </template>

    <view v-else-if="loading" class="loading-tip">加载中…</view>
    <view v-else class="empty">
      <text class="empty-icon">📈</text>
      <text class="empty-text">暂无可查看的档案</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import VisionTrendChart from '@/components/VisionTrendChart.vue'
import { childApi } from '@/api/child'
import { useUserStore } from '@/stores/user'
import { displayVision, visionDelta } from '@/utils/format'
import type { CareRecord, Child } from '@/types'

const userStore = useUserStore()

const children = ref<Child[]>([])
const childId = ref(0)
const loading = ref(false)
const records = ref<CareRecord[]>([])

const firstVision = computed(() => records.value[0]?.visionBeforeBoth)
const lastVision = computed(() => records.value[records.value.length - 1]?.visionAfterBoth)

const deltaText = computed(() => visionDelta(firstVision.value, lastVision.value) || '—')

const deltaClass = computed(() => {
  const text = deltaText.value
  if (text === '持平') return 'sum-flat'
  if (text.startsWith('+')) return 'sum-up'
  if (text.startsWith('-')) return 'sum-down'
  return ''
})

function onLoaded(rows: CareRecord[]) {
  records.value = rows
}

function changeChild(id: number) {
  if (childId.value === id) return
  childId.value = id
  records.value = []
}

onLoad(async (options) => {
  loading.value = true
  try {
    const res = await childApi.getChildList({ parentUserId: userStore.userId })
    children.value = res || []
    const fromOptions = Number(options?.childId || 0)
    childId.value = fromOptions && children.value.some((c) => c.id === fromOptions)
      ? fromOptions
      : children.value[0]?.id || 0
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

.summary-card {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
}

.sum-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.sum-label {
  font-size: 22rpx;
  color: #94a3b8;
}

.sum-value {
  margin-top: 10rpx;
  font-size: 34rpx;
  font-weight: 700;
  color: #475569;
}

.sum-after {
  color: #0d9488;
}

.sum-up {
  color: #14b8a6;
}

.sum-down {
  color: #ef4444;
}

.sum-flat {
  color: #94a3b8;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16rpx;
}
</style>
