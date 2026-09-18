<template>
  <view class="reserve-card" @click="$emit('detail', row)">
    <view class="rc-head">
      <view class="rc-name-wrap">
        <text class="rc-name">{{ row.childName || '未知儿童' }}</text>
        <text v-if="showOverdue" class="tag tag-danger rc-overdue">逾期</text>
      </view>
      <text class="tag" :class="statusTag">{{ statusLabel }}</text>
    </view>

    <view class="rc-time">
      <text class="rc-date">{{ showDate ? row.scheduleDate : '今天' }}</text>
      <text class="rc-slot">{{ formatHm(row.timeSlotStart) }} - {{ formatHm(row.timeSlotEnd) }}</text>
      <text v-if="row.adjustFlag === 1" class="rc-adjusted">已调整</text>
    </view>

    <view class="rc-meta">
      <text class="rc-meta-item">家长：{{ row.parentName || '-' }}</text>
      <text class="rc-meta-item">{{ row.parentPhone || '' }}</text>
    </view>
    <view v-if="row.executorName" class="rc-meta">
      <text class="rc-meta-item">执行：{{ row.executorName }}</text>
    </view>
    <view v-if="statusValue === 4" class="rc-meta">
      <text class="rc-meta-item rc-reason">取消原因：{{ row.cancelReason || '-' }}</text>
    </view>

    <view v-if="!readonly" class="rc-actions" @click.stop>
      <view
        v-if="row.status === 1 || row.status === 2"
        class="btn btn-sm"
        :class="row.status === 2 ? 'btn-primary' : 'btn-primary'"
        :style="{ opacity: canCare ? 1 : 0.45 }"
        @click="onCare"
      >
        {{ row.status === 2 ? '完成养护' : '开始养护' }}
      </view>
      <view
        v-if="row.status === 1"
        class="btn btn-sm btn-plain"
        :class="{ 'is-disabled': !adjustable }"
        @click="emitIf('adjust', adjustable)"
      >
        调整
      </view>
      <view
        v-if="row.status === 1"
        class="btn btn-sm btn-plain"
        :class="{ 'is-disabled': !noShowable }"
        @click="emitIf('noshow', noShowable)"
      >
        爽约
      </view>
      <view
        v-if="row.status === 1"
        class="btn btn-sm btn-danger-plain"
        :class="{ 'is-disabled': !cancelable }"
        @click="emitIf('cancel', cancelable)"
      >
        取消
      </view>
      <view v-if="row.status === 3 || statusValue === 4" class="btn btn-sm btn-plain" @click="$emit('detail', row)">
        详情
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Reserve } from '@/types'
import { reserveStatusLabel, reserveStatusTag, reserveStatusValue } from '@/utils/dict'
import { formatHm } from '@/utils/format'
import { canAdjust, canCancel, canMarkNoShow, canOperateCare, isOverdue } from '@/utils/reserve-rules'
import { toast } from '@/utils/request'

const props = defineProps<{
  row: Reserve
  showDate?: boolean
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: 'care', row: Reserve): void
  (e: 'adjust', row: Reserve): void
  (e: 'noshow', row: Reserve): void
  (e: 'cancel', row: Reserve): void
  (e: 'detail', row: Reserve): void
}>()

const statusValue = computed(() => reserveStatusValue(props.row))
const statusLabel = computed(() => reserveStatusLabel(props.row))
const statusTag = computed(() => reserveStatusTag(props.row))
const showOverdue = computed(() => props.row.status === 1 && isOverdue(props.row))

const canCare = computed(() => canOperateCare(props.row))
const adjustable = computed(() => canAdjust(props.row))
const noShowable = computed(() => canMarkNoShow(props.row))
const cancelable = computed(() => canCancel(props.row))

function onCare() {
  if (!canCare.value) {
    toast('仅预约当天可开始养护')
    return
  }
  emit('care', props.row)
}

function emitIf(type: 'adjust' | 'noshow' | 'cancel', enabled: boolean) {
  if (!enabled) {
    if (type === 'adjust') toast('已超过预约时段，不可调整')
    else if (type === 'cancel') toast('已超过预约时段，不可取消，仅可标记爽约')
    else toast('仅预约当天或逾期后可标记爽约')
    return
  }
  if (type === 'adjust') emit('adjust', props.row)
  else if (type === 'noshow') emit('noshow', props.row)
  else emit('cancel', props.row)
}
</script>

<style lang="scss" scoped>
.reserve-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx 26rpx 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
}

.reserve-card + .reserve-card {
  margin-top: 20rpx;
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
  color: #1f2937;
  margin-right: 12rpx;
}

.rc-overdue {
  margin-right: 8rpx;
}

.rc-time {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: #1f2937;
}

.rc-date {
  color: #5b6572;
  margin-right: 16rpx;
}

.rc-slot {
  font-weight: 600;
  color: #1d4ed8;
}

.rc-adjusted {
  margin-left: 16rpx;
  font-size: 22rpx;
  color: #3b82f6;
}

.rc-meta {
  display: flex;
  flex-wrap: wrap;
  margin-top: 10rpx;
}

.rc-meta-item {
  font-size: 24rpx;
  color: #9ca3af;
  margin-right: 20rpx;
}

.rc-reason {
  color: #e07a5f;
}

.rc-actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 22rpx;
  padding-top: 20rpx;
  border-top: 2rpx solid #eef1f5;
}
</style>
