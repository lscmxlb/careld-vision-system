<template>
  <view v-if="visible" class="cancel-root">
    <!-- 严格模态：遮罩只负责挡住下层（含 tabbar），点击/拖动都不关闭弹窗 -->
    <view class="mask" @touchmove.stop.prevent="noop" />
    <view class="sheet sheet-center">
      <view class="sheet-header">
        <text class="sheet-title">取消预约</text>
        <text class="sheet-close" @click="close">✕</text>
      </view>
      <scroll-view class="sheet-body" scroll-y>
        <view v-if="row" class="cancel-target">
          <text class="ct-name">{{ row.childName }}</text>
          <text class="ct-slot">{{ row.scheduleDate }} {{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</text>
        </view>
        <view class="cancel-label">
          取消原因 <text class="req">*</text>
        </view>
        <textarea
          v-model="reason"
          class="cancel-input"
          maxlength="100"
          placeholder="请填写取消原因（必填）"
          placeholder-class="ph"
        />
        <view class="cancel-count">{{ reason.length }}/100</view>
        <view class="cancel-tip">取消后该预约次数将自动退还给儿童档案</view>
      </scroll-view>
      <view class="sheet-footer">
        <view class="btn btn-block btn-primary" :class="{ 'is-disabled': !reason.trim() || loading }" @click="submit">
          {{ loading ? '提交中…' : '确认取消预约' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Reserve } from '@/types'
import { formatHm } from '@/utils/format'

const props = defineProps<{
  visible: boolean
  row: Reserve | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', reason: string): void
}>()

const reason = ref('')

watch(
  () => props.visible,
  (value) => {
    if (value) reason.value = ''
  },
)

function close() {
  emit('update:visible', false)
}

function noop() {}

function submit() {
  const text = reason.value.trim()
  if (!text || props.loading) return
  emit('confirm', text)
}
</script>

<style lang="scss" scoped>
// 本页是 tabbar 页，弹层层级需高于 uni-tabbar（z-index: 998）才能挡住底部导航
.cancel-root {
  position: relative;
  z-index: 1000;
}

.cancel-target {
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 28rpx;
}

.ct-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
}

.ct-slot {
  display: block;
  margin-top: 8rpx;
  font-size: 25rpx;
  color: #5b6572;
}

.cancel-label {
  font-size: 27rpx;
  color: #1f2937;
  margin-bottom: 14rpx;
}

.req {
  color: #ef4444;
}

.cancel-input {
  width: 100%;
  height: 200rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 28rpx;
  color: #1f2937;
  box-sizing: border-box;
}

.ph {
  color: #b7bfc9;
  font-size: 28rpx;
}

.cancel-count {
  text-align: right;
  font-size: 22rpx;
  color: #b7bfc9;
  margin-top: 8rpx;
}

.cancel-tip {
  margin-top: 12rpx;
  font-size: 23rpx;
  color: #9ca3af;
}
</style>
