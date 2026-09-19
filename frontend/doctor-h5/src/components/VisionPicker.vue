<template>
  <view class="vision-row" :class="{ 'vr-disabled': disabled }">
    <text class="vr-label">{{ label }}</text>
    <picker
      class="vr-control"
      mode="selector"
      :range="VISION_MAIN_OPTIONS"
      :value="mainIndex"
      :disabled="disabled"
      @change="onMainChange"
    >
      <view class="vr-cell" :class="{ 'vr-empty': !parsed.main }">
        <text class="vr-text">{{ parsed.main || '5.3-4.0' }}</text>
        <view class="vr-arrow" />
      </view>
    </picker>
    <picker
      v-if="showSub"
      class="vr-control"
      mode="selector"
      :range="VISION_SUB_OPTIONS"
      :value="subIndex"
      :disabled="disabled"
      @change="onSubChange"
    >
      <view class="vr-cell">
        <text class="vr-text">{{ parsed.sub }}</text>
        <view class="vr-arrow" />
      </view>
    </picker>
    <text v-if="!disabled && parsed.main" class="vr-clear" @click="clear">✕</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { VISION_MAIN_OPTIONS, VISION_SUB_OPTIONS, combineVision, parseVision } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    label: string
    disabled?: boolean
    /** 是否显示副值选择（档案裸眼视力只用主值） */
    showSub?: boolean
  }>(),
  { disabled: false, showSub: true },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void
}>()

const parsed = computed(() => parseVision(props.modelValue))
const mainIndex = computed(() => {
  const index = VISION_MAIN_OPTIONS.indexOf(parsed.value.main)
  return index < 0 ? 0 : index
})
const subIndex = computed(() => {
  const index = VISION_SUB_OPTIONS.indexOf(parsed.value.sub)
  return index < 0 ? 0 : index
})

function onMainChange(e: { detail: { value: string | number } }) {
  const main = VISION_MAIN_OPTIONS[Number(e.detail.value)]
  emit('update:modelValue', combineVision(main, props.showSub ? parsed.value.sub : ''))
}

function onSubChange(e: { detail: { value: string | number } }) {
  if (!parsed.value.main) return
  const sub = VISION_SUB_OPTIONS[Number(e.detail.value)]
  emit('update:modelValue', combineVision(parsed.value.main, sub))
}

function clear() {
  emit('update:modelValue', undefined)
}
</script>

<style lang="scss" scoped>
.vision-row {
  display: flex;
  align-items: center;
  min-height: 92rpx;
  padding: 14rpx 0;
  border-bottom: 2rpx solid #eef1f5;
}

.vision-row:last-child {
  border-bottom: none;
}

.vr-disabled {
  opacity: 0.65;
}

.vr-label {
  width: 110rpx;
  flex-shrink: 0;
  font-size: 27rpx;
  color: #5b6572;
}

.vr-control {
  flex: 1;
  min-width: 0;
}

.vr-control + .vr-control {
  margin-left: 14rpx;
}

.vr-cell {
  display: flex;
  align-items: center;
  width: 100%;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f8fafc;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.vr-text {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vr-empty .vr-text {
  color: #c2c9d1;
}

.vr-arrow {
  width: 14rpx;
  height: 14rpx;
  flex: none;
  margin-left: 12rpx;
  border-top: 3rpx solid #94a3b8;
  border-right: 3rpx solid #94a3b8;
  transform: rotate(135deg);
}

.vr-clear {
  font-size: 24rpx;
  color: #b7bfc9;
  margin-left: 6rpx;
  padding: 8rpx;
}
</style>
