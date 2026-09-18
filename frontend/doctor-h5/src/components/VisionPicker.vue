<template>
  <view class="vision-row" :class="{ 'vr-disabled': disabled }">
    <text class="vr-label">{{ label }}</text>
    <picker
      mode="selector"
      :range="VISION_MAIN_OPTIONS"
      :value="mainIndex"
      :disabled="disabled"
      @change="onMainChange"
    >
      <view class="vr-cell" :class="{ 'vr-empty': !parsed.main }">
        <text class="vr-text">{{ parsed.main || '5.3-4.0' }}</text>
        <text class="vr-arrow">▾</text>
      </view>
    </picker>
    <picker
      v-if="showSub"
      mode="selector"
      :range="VISION_SUB_OPTIONS"
      :value="subIndex"
      :disabled="disabled"
      @change="onSubChange"
    >
      <view class="vr-cell vr-cell-sub">
        <text class="vr-text">{{ parsed.sub }}</text>
        <text class="vr-arrow">▾</text>
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

.vr-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 168rpx;
  height: 64rpx;
  padding: 0 18rpx;
  background: #f7f9fb;
  border-radius: 12rpx;
  margin-right: 14rpx;
}

.vr-cell-sub {
  min-width: 132rpx;
}

.vr-text {
  font-size: 28rpx;
  color: #1f2937;
}

.vr-empty .vr-text {
  color: #b7bfc9;
  font-size: 26rpx;
}

.vr-arrow {
  font-size: 20rpx;
  color: #b7bfc9;
  margin-left: 10rpx;
}

.vr-clear {
  font-size: 24rpx;
  color: #b7bfc9;
  padding: 8rpx;
}
</style>
