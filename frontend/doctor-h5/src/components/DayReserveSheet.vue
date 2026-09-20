<template>
  <view v-if="visible" class="day-root">
    <view class="mask" />
    <view class="sheet">
      <view class="sheet-header">
        <text class="sheet-title">{{ date }} 预约详情</text>
        <text class="sheet-close" @click="close">✕</text>
      </view>
      <scroll-view class="sheet-body" scroll-y>
        <view v-if="loading" class="day-loading">加载中…</view>
        <view v-else class="day-list">
          <ReserveCard v-for="row in sortedList" :key="row.id" :row="row" show-date readonly />
          <view v-if="!sortedList.length" class="day-empty">该日期暂无预约记录</view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ReserveCard from '@/components/ReserveCard.vue'
import type { Reserve } from '@/types'

const props = defineProps<{
  visible: boolean
  date: string
  list: Reserve[]
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

/** 默认排序：有效预约在上、已取消（含爽约）在下，组内按开始时间升序 */
const sortedList = computed(() =>
  [...(props.list || [])].sort((a, b) => {
    const bucket = (s: number) => (s === 4 ? 1 : 0)
    if (bucket(a.status) !== bucket(b.status)) {
      return bucket(a.status) - bucket(b.status)
    }
    const byTime = (a.timeSlotStart || '').localeCompare(b.timeSlotStart || '')
    return byTime !== 0 ? byTime : (a.id || 0) - (b.id || 0)
  }),
)

function close() {
  emit('update:visible', false)
}
</script>

<style lang="scss" scoped>
.day-root {
  position: relative;
  /* 高于底部 tabbar(998)：弹层打开时遮罩盖住整页与导航栏，只能操作弹层本身 */
  z-index: 1000;
}

/* 弹层唯一出口：显眼的红色关闭按钮 */
.day-root .sheet-close {
  color: $app-danger;
  font-weight: 700;
}

/* 浮在底部 tabbar 上方并留出间距，避免弹层与导航栏连成一片（tabbar 高 54px，见 pages.json） */
.day-root .sheet {
  bottom: calc(54px + env(safe-area-inset-bottom) + 16px);
  border-radius: 28rpx;
}

.day-list {
  padding-bottom: 24rpx;
}

.day-loading {
  text-align: center;
  padding: 60rpx 0;
  font-size: 25rpx;
  color: #9ca3af;
}

.day-empty {
  text-align: center;
  padding: 80rpx 0;
  font-size: 26rpx;
  color: #9ca3af;
}
</style>
