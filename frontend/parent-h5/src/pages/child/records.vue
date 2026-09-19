<template>
  <view class="page">
    <view v-if="loading" class="loading-tip">加载中…</view>

    <template v-else-if="records.length">
      <view v-for="row in records" :key="row.id" class="card rec-card">
        <view class="rec-head">
          <text class="tag" :class="row.changeCount > 0 ? 'tag-success' : 'tag-danger'">
            {{ typeText(row.changeType) }}
          </text>
          <text class="rec-count" :class="row.changeCount > 0 ? 'rec-plus' : 'rec-minus'">
            {{ row.changeCount > 0 ? '+' : '' }}{{ row.changeCount }} 次
          </text>
        </view>
        <view class="rec-meta">
          <text>{{ formatDateTime(row.createdAt) || '—' }}</text>
          <text class="rec-dot">·</text>
          <text>可用次数 {{ row.remainingAfter ?? '—' }}</text>
        </view>
        <view v-if="row.reserveDate" class="rec-meta">
          <text>预约日期 {{ row.reserveDate }}</text>
          <text class="rec-dot">·</text>
          <text>时段 {{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</text>
        </view>
        <view v-else class="rec-meta">
          <text>{{ row.paymentMethod || '—' }}</text>
          <text class="rec-dot">·</text>
          <text>{{ row.paymentAmount != null ? `¥${row.paymentAmount}` : '—' }}</text>
          <text class="rec-dot">·</text>
          <text>开单医生 {{ row.doctorName || '—' }}</text>
        </view>
        <view v-if="row.remark" class="rec-remark">备注：{{ row.remark }}</view>
      </view>
    </template>

    <view v-else class="empty">
      <text class="empty-icon">🧾</text>
      <text>暂无预约记录</text>
    </view>

    <view class="safe-bottom" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { formatDateTime, formatHm } from '@/utils/format'
import type { ChildServiceRecord } from '@/types'

const childId = ref(0)
const records = ref<ChildServiceRecord[]>([])
const loading = ref(false)

/** 变更类型：1 预约授权 / 2 预约扣减 / 3 取消退还 / 4 爽约退还 / 5 爽约不退还 */
const CHANGE_TYPE_MAP: Record<number, string> = {
  1: '预约授权',
  2: '预约扣减',
  3: '取消退还',
  4: '爽约退还',
  5: '爽约不退还',
}

function typeText(type: number) {
  return CHANGE_TYPE_MAP[type] || '未知'
}

async function load() {
  if (!childId.value) return
  loading.value = true
  try {
    records.value = await childApi.getServiceRecords(childId.value)
  } catch {
    records.value = []
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  childId.value = Number(options?.id || 0)
  load()
})
</script>

<style lang="scss" scoped>
.rec-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rec-count {
  font-size: 30rpx;
  font-weight: 600;
}

.rec-plus {
  color: #10b981;
}

.rec-minus {
  color: #ef4444;
}

.rec-meta {
  display: flex;
  align-items: center;
  margin-top: 14rpx;
  font-size: 25rpx;
  color: #64748b;
}

.rec-dot {
  margin: 0 10rpx;
  color: #cbd5e1;
}

.rec-remark {
  margin-top: 14rpx;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  color: #475569;
  background: #f8fafc;
  border-radius: 10rpx;
}
</style>
