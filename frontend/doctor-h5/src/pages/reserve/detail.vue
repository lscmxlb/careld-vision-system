<template>
  <view class="detail">
    <view v-if="loading" class="loading-tip">加载中…</view>
    <template v-else-if="row">
      <!-- 状态头 -->
      <view class="status-head" :class="statusClass">
        <view class="sh-left">
          <text class="sh-name">{{ row.childName || '未知儿童' }}</text>
          <text class="sh-date">{{ row.scheduleDate }} {{ formatHm(row.timeSlotStart) }} - {{ formatHm(row.timeSlotEnd) }}</text>
        </view>
        <text class="sh-tag">{{ statusLabel }}</text>
      </view>

      <view class="body">
        <view class="card">
          <view class="section-title">预约信息</view>
          <view class="info-row">
            <text class="ir-label">儿童姓名</text>
            <text class="ir-value">{{ row.childName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">家长姓名</text>
            <text class="ir-value">{{ row.parentName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">联系电话</text>
            <text class="ir-value">{{ row.parentPhone || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">预约时段</text>
            <text class="ir-value">{{ row.scheduleDate }} {{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">来源</text>
            <text class="ir-value">{{ row.source === 1 ? '家长预约' : '医生/医院预约' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">操作人</text>
            <text class="ir-value">{{ row.operatorName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">备注</text>
            <text class="ir-value">{{ row.remark || '-' }}</text>
          </view>
        </view>

        <view class="card">
          <view class="section-title">养护执行</view>
          <view class="info-row">
            <text class="ir-label">执行人</text>
            <text class="ir-value">{{ row.executorName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">开始时间</text>
            <text class="ir-value">{{ formatDateTime(row.startTime) || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">结束时间</text>
            <text class="ir-value">{{ formatDateTime(row.endTime) || '-' }}</text>
          </view>
          <view v-if="row.status === 2 || row.status === 3" class="link-row" @click="goCareRecord">
            <text>查看养护记录</text>
            <text class="link-arrow">›</text>
          </view>
        </view>

        <view v-if="row.status === 4" class="card">
          <view class="section-title">取消信息</view>
          <view class="info-row">
            <text class="ir-label">取消原因</text>
            <text class="ir-value">{{ row.cancelReason || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">操作人</text>
            <text class="ir-value">{{ row.cancelOperatorName || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="ir-label">爽约标记</text>
            <text class="ir-value">{{ row.noShowFlag === 1 ? '已标记爽约（次数不退还）' : '否' }}</text>
          </view>
        </view>

        <view v-if="row.adjustFlag === 1" class="card">
          <view class="section-title">调整信息</view>
          <view class="info-row">
            <text class="ir-label">调整人</text>
            <text class="ir-value">{{ row.adjustOperatorName || '-' }}</text>
          </view>
        </view>

        <view class="safe-bottom" />
        <view v-if="hasActions" class="submit-holder" />
      </view>
    </template>

    <view v-else class="empty">
      <view class="empty-icon">?</view>
      <text>预约不存在或无权查看</text>
    </view>

    <!-- 底部操作 -->
    <view v-if="row && hasActions" class="sticky-bar">
      <view class="bar-row">
        <view
          v-if="row.status === 1 || row.status === 2"
          class="btn btn-sm"
          :class="canCare ? 'btn-primary' : 'is-disabled'"
          @click="goCare"
        >
          {{ row.status === 2 ? '完成养护' : '开始养护' }}
        </view>
        <view v-if="row.status === 1" class="btn btn-sm btn-plain" :class="{ 'is-disabled': !adjustable }" @click="onAdjust">
          调整
        </view>
        <view v-if="row.status === 1" class="btn btn-sm btn-plain" :class="{ 'is-disabled': !noShowable }" @click="onNoShow">
          爽约
        </view>
        <view v-if="row.status === 1" class="btn btn-sm btn-danger-plain" :class="{ 'is-disabled': !cancelable }" @click="openCancel">
          取消
        </view>
      </view>
    </view>

    <CancelSheet v-model:visible="cancelVisible" :row="row" :loading="cancelLoading" @confirm="submitCancel" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import CancelSheet from '@/components/CancelSheet.vue'
import { reserveApi } from '@/api'
import { reserveStatusLabel, reserveStatusValue } from '@/utils/dict'
import { formatDateTime, formatHm } from '@/utils/format'
import { canAdjust, canCancel, canMarkNoShow, canOperateCare, overdueTip } from '@/utils/reserve-rules'
import { toast } from '@/utils/request'
import type { Reserve } from '@/types'

const reserveId = ref<number | null>(null)
const row = ref<Reserve | null>(null)
const loading = ref(false)
const cancelVisible = ref(false)
const cancelLoading = ref(false)

const statusValue = computed(() => (row.value ? reserveStatusValue(row.value) : 0))
const statusLabel = computed(() => (row.value ? reserveStatusLabel(row.value) : ''))
const statusClass = computed(() => `sh-${statusValue.value}`)

const canCare = computed(() => !!row.value && canOperateCare(row.value))
const adjustable = computed(() => !!row.value && canAdjust(row.value))
const noShowable = computed(() => !!row.value && canMarkNoShow(row.value))
const cancelable = computed(() => !!row.value && canCancel(row.value))
const hasActions = computed(() => !!row.value && row.value.status <= 2)

onLoad(async (query) => {
  reserveId.value = Number(query?.id)
  if (!reserveId.value) {
    toast('缺少预约参数')
    return
  }
  await fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    row.value = await reserveApi.getReserveDetail(reserveId.value!)
  } catch {
    // 错误提示已在请求层处理
  } finally {
    loading.value = false
  }
}

function goCare() {
  if (!row.value) return
  if (!canCare.value) {
    toast('仅预约当天可开始养护')
    return
  }
  uni.navigateTo({ url: `/pages/reserve/care?id=${row.value.id}` })
}

function goCareRecord() {
  if (!row.value) return
  uni.navigateTo({ url: `/pages/care-record/detail?reserveId=${row.value.id}` })
}

function onAdjust() {
  if (!row.value) return
  if (!adjustable.value) {
    toast(overdueTip(row.value) || '当前状态不可调整')
    return
  }
  uni.navigateTo({ url: `/pages/reserve/create?mode=adjust&id=${row.value.id}` })
}

function onNoShow() {
  if (!row.value) return
  if (!noShowable.value) {
    toast('仅预约当天或逾期后可标记爽约')
    return
  }
  uni.showModal({
    title: '标记爽约',
    content: '标记爽约不退还预约次数（与取消不同），确认标记该预约为爽约吗？',
    confirmText: '确定标记',
    confirmColor: '#f59e0b',
    success: async (res) => {
      if (!res.confirm || !row.value) return
      try {
        await reserveApi.markNoShow(row.value.id)
        uni.showToast({ title: '已标记爽约（不退还次数）', icon: 'none' })
        fetchDetail()
      } catch {
        // 错误提示已在请求层处理
      }
    },
  })
}

function openCancel() {
  if (!row.value) return
  if (!cancelable.value) {
    toast(overdueTip(row.value) || '当前状态不可取消')
    return
  }
  cancelVisible.value = true
}

async function submitCancel(reason: string) {
  if (!row.value) return
  cancelLoading.value = true
  try {
    await reserveApi.cancelReserve(row.value.id, reason)
    uni.showToast({ title: '预约已取消，次数已退还', icon: 'none' })
    cancelVisible.value = false
    fetchDetail()
  } catch {
    // 错误提示已在请求层处理
  } finally {
    cancelLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.detail {
  min-height: 100vh;
  background: #f4f6f9;
}

.status-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40rpx 32rpx;
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
}

.sh-2 {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

.sh-3 {
  background: linear-gradient(135deg, #4ade80, #22c55e);
}

.sh-4 {
  background: linear-gradient(135deg, #cbd5e1, #94a3b8);
}

.sh-5 {
  background: linear-gradient(135deg, #f87171, #ef4444);
}

.sh-name {
  display: block;
  font-size: 38rpx;
  font-weight: 600;
  color: #fff;
}

.sh-date {
  display: block;
  margin-top: 10rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.sh-tag {
  padding: 8rpx 22rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.24);
  color: #fff;
  font-size: 25rpx;
}

.body {
  padding: 24rpx 28rpx 0;
}

.info-row {
  display: flex;
  align-items: flex-start;
  padding: 20rpx 0;
  border-bottom: 2rpx solid #f4f6f9;
}

.info-row:last-child {
  border-bottom: none;
}

.ir-label {
  width: 160rpx;
  flex-shrink: 0;
  font-size: 27rpx;
  color: #9ca3af;
}

.ir-value {
  flex: 1;
  font-size: 28rpx;
  color: #1f2937;
  word-break: break-all;
}

.link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22rpx 0 4rpx;
  font-size: 27rpx;
  color: #2563eb;
  border-top: 2rpx solid #f4f6f9;
}

.link-arrow {
  font-size: 34rpx;
  color: #cbd5e1;
}

.submit-holder {
  height: 130rpx;
}

.bar-row {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
}
</style>
