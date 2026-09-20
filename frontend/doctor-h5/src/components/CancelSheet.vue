<template>
  <view v-if="visible" class="cancel-root">
    <!-- 严格模态：遮罩只负责挡住下层，点击/拖动都不关闭弹窗、也不让底层页面滚动 -->
    <view class="mask" @touchmove.stop.prevent="noop" />
    <view class="sheet sheet-center">
      <view class="sheet-header">
        <text class="sheet-title">取消预约</text>
        <view class="sheet-close-btn" @click="close">
          <text class="sheet-close-icon">✕</text>
        </view>
      </view>
      <scroll-view class="sheet-body" scroll-y>
        <view v-if="row" class="cancel-target">
          <text class="ct-name">{{ row.childName }}</text>
          <text class="ct-slot">{{ row.scheduleDate }} {{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</text>
        </view>
        <view class="cancel-row">
          <text class="cancel-label">取消原因</text>
          <view class="field-control">
            <picker mode="selector" :range="REASON_TYPE_LABELS" :value="reasonTypeIndex" @change="onReasonTypeChange">
              <view class="select-box">
                <text class="select-text">{{ REASON_TYPE_LABELS[reasonTypeIndex] }}</text>
                <view class="select-arrow" />
              </view>
            </picker>
          </view>
        </view>
        <view class="cancel-row">
          <text class="cancel-label">预约次数</text>
          <view class="field-control">
            <picker mode="selector" :range="REFUND_LABELS" :value="refundIndex < 0 ? 0 : refundIndex" @change="onRefundChange">
              <view class="select-box">
                <text class="select-text" :class="{ 'field-placeholder': refundIndex < 0 }">
                  {{ refundIndex < 0 ? '请选择' : REFUND_LABELS[refundIndex] }}
                </text>
                <view class="select-arrow" />
              </view>
            </picker>
          </view>
        </view>
        <view class="cancel-label">备注</view>
        <textarea
          v-model="reason"
          class="cancel-input"
          maxlength="100"
          placeholder="请输入备注（选填）"
          placeholder-class="ph"
        />
        <view class="cancel-count">{{ reason.length }}/100</view>
        <view class="cancel-tip">{{ tipText }}</view>
      </scroll-view>
      <view class="sheet-footer">
        <view class="btn btn-block btn-primary" :class="{ 'is-disabled': loading }" @click="submit">
          {{ loading ? '提交中…' : '确认取消预约' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Reserve } from '@/types'
import { formatHm } from '@/utils/format'

/** 取消原因：首项为医院原因（默认选中），index → 后端 cancelReasonType 1家长 2医院 */
const REASON_TYPE_LABELS = ['医院原因，无法接待', '家长原因，主动要求取消预约']
const REASON_TYPE_VALUES = [2, 1]
const REFUND_LABELS = ['返还预约次数至原账号', '预约次数扣减不予返还']

const props = defineProps<{
  visible: boolean
  row: Reserve | null
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', payload: { cancelReason: string; cancelReasonType: number; refundFlag: number }): void
}>()

const reason = ref('')
const reasonTypeIndex = ref(0)
/** -1 = 未选择（必选项，不能默认带出） */
const refundIndex = ref(-1)

const tipText = computed(() => {
  if (refundIndex.value < 0) return '请选择预约次数是否返还后再提交'
  return refundIndex.value === 0 ? '取消后该预约次数将返还至原账号' : '取消后该预约次数不予返还'
})

watch(
  () => props.visible,
  (value) => {
    if (value) {
      reason.value = ''
      reasonTypeIndex.value = 0
      refundIndex.value = -1
    }
  },
)

function onReasonTypeChange(e: any) {
  reasonTypeIndex.value = Number(e.detail.value) || 0
}

function onRefundChange(e: any) {
  refundIndex.value = Number(e.detail.value) || 0
}

function close() {
  emit('update:visible', false)
}

/** 遮罩 touchmove 占位处理：阻止底层页面滚动（严格模态） */
function noop() {}

function submit() {
  if (props.loading) return
  if (refundIndex.value < 0) {
    uni.showToast({ title: '请选择预约次数是否返还', icon: 'none' })
    return
  }
  emit('confirm', {
    cancelReason: reason.value.trim(),
    cancelReasonType: REASON_TYPE_VALUES[reasonTypeIndex.value],
    refundFlag: refundIndex.value === 0 ? 1 : 0,
  })
}
</script>

<style lang="scss" scoped>
.cancel-root {
  position: relative;
  /* 高于底部 tabbar(998)：否则弹层底部「确认取消预约」被 tabbar 遮挡不可点 */
  z-index: 1000;
}

/* 右上角关闭按钮：红底红叉圆钮，醒目且是唯一关闭入口（点遮罩无效） */
.sheet-close-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sheet-close-icon {
  font-size: 36rpx;
  font-weight: 700;
  color: #ef4444;
  line-height: 1;
}

/* 居中弹窗不贴底，去掉安全区留白 */
.sheet-center .sheet-footer {
  padding-bottom: 16rpx;
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

.cancel-row {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
}

.cancel-label {
  font-size: 27rpx;
  color: #1f2937;
  margin-bottom: 14rpx;
}

.cancel-row .cancel-label {
  flex: none;
  margin-bottom: 0;
  margin-right: 20rpx;
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
