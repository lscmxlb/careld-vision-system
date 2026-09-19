<template>
  <view class="care">
    <view class="body">
      <!-- 预约信息 -->
      <view class="card">
        <view class="head">
          <text class="head-name">{{ reserve?.childName || '-' }}</text>
          <text class="tag tag-primary">{{ mode === 'start' ? '待开始养护' : '养护中' }}</text>
        </view>
        <view class="head-time">
          预约养护时间：{{ reserve?.scheduleDate }} {{ formatHm(reserve?.timeSlotStart) }}-{{ formatHm(reserve?.timeSlotEnd) }}
        </view>
        <view v-if="reserve?.remark" class="head-remark">备注：{{ reserve.remark }}</view>
      </view>

      <!-- 养护服务信息 -->
      <view class="card">
        <view class="section-title">养护服务</view>
        <view class="field">
          <text class="field-label required">养护人</text>
          <view class="field-control">
            <picker
              mode="selector"
              :range="staffNames"
              :value="staffIndex"
              :disabled="mode !== 'start'"
              @change="onStaffChange"
            >
              <view class="select-box" :class="{ 'is-locked': mode !== 'start' }">
                <text class="select-text" :class="{ 'field-placeholder': !form.executorId }">
                  {{ executorName || '请选择养护人' }}
                </text>
                <view class="select-arrow" />
              </view>
            </picker>
          </view>
        </view>
        <view class="field">
          <text class="field-label required">开始时间</text>
          <view class="field-control">
            <picker mode="time" :value="form.startTime || '09:00'" :disabled="mode !== 'start'" @change="onTimeChange">
              <view class="select-box" :class="{ 'is-locked': mode !== 'start' }">
                <text class="select-text" :class="{ 'field-placeholder': !form.startTime }">
                  {{ form.startTime || '选择开始时间' }}
                </text>
                <view class="select-arrow" />
              </view>
            </picker>
          </view>
        </view>
      </view>

      <!-- 养护前视力 -->
      <view class="card">
        <view class="section-title">
          <text>养护前裸眼视力</text>
          <text v-if="mode !== 'start'" class="section-more">已登记，不可修改</text>
        </view>
        <VisionPicker v-model="form.visionBeforeBoth" label="双眼" :disabled="mode !== 'start'" />
        <VisionPicker v-model="form.visionBeforeLeft" label="左眼" :disabled="mode !== 'start'" />
        <VisionPicker v-model="form.visionBeforeRight" label="右眼" :disabled="mode !== 'start'" />
      </view>

      <!-- 养护后视力 -->
      <view class="card">
        <view class="section-title">
          <text>养护后裸眼视力</text>
          <text class="section-more">结束养护时填写</text>
        </view>
        <VisionPicker v-model="form.visionAfterBoth" label="双眼" />
        <VisionPicker v-model="form.visionAfterLeft" label="左眼" />
        <VisionPicker v-model="form.visionAfterRight" label="右眼" />
      </view>

      <view class="safe-bottom" />
      <view class="submit-holder" />
    </view>

    <view class="sticky-bar">
      <view class="btn btn-danger" :class="{ 'is-disabled': submitting }" @click="onCancel">取消</view>
      <view
        v-if="mode === 'start'"
        class="btn btn-primary"
        :class="{ 'is-disabled': !form.executorId || !form.startTime || submitting }"
        @click="submitStart"
      >
        开始养护
      </view>
      <view
        class="btn btn-success"
        :class="{ 'is-disabled': !form.executorId || !form.startTime || submitting }"
        @click="submitComplete"
      >
        {{ submitting ? '提交中…' : '完成养护' }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onBackPress, onLoad } from '@dcloudio/uni-app'
import VisionPicker from '@/components/VisionPicker.vue'
import { medicalStaffApi, reserveApi } from '@/api'
import { formatHm } from '@/utils/format'
import { toast } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import type { MedicalStaff, Reserve, CareRecord } from '@/types'

const userStore = useUserStore()

const reserveId = ref<number | null>(null)
const reserve = ref<Reserve | null>(null)
const mode = ref<'start' | 'complete'>('start')

const staffList = ref<MedicalStaff[]>([])
const submitting = ref(false)
const leaving = ref(false)

const emptyVision = () => ({
  executorId: undefined as number | undefined,
  startTime: '',
  visionBeforeBoth: undefined as string | undefined,
  visionBeforeLeft: undefined as string | undefined,
  visionBeforeRight: undefined as string | undefined,
  visionAfterBoth: undefined as string | undefined,
  visionAfterLeft: undefined as string | undefined,
  visionAfterRight: undefined as string | undefined,
})

const form = reactive(emptyVision())

const staffNames = computed(() => staffList.value.map((s) => `${s.name}${s.staffRole === 2 ? '（助理）' : ''}`))
const staffIndex = computed(() => {
  const index = staffList.value.findIndex((s) => s.id === form.executorId)
  return index < 0 ? 0 : index
})
const executorName = computed(() => {
  const staff = staffList.value.find((s) => s.id === form.executorId)
  return staff ? staff.name : ''
})

onLoad(async (query) => {
  reserveId.value = Number(query?.id)
  if (!reserveId.value) {
    toast('缺少预约参数')
    return
  }
  await Promise.all([loadReserve(), loadStaff()])
  if (mode.value === 'complete') await backfillFromRecord()
  capture()
})

async function loadReserve() {
  if (!reserveId.value) return
  try {
    reserve.value = await reserveApi.getReserveDetail(reserveId.value)
    mode.value = reserve.value.status === 2 ? 'complete' : 'start'
    if (mode.value === 'start' && !form.startTime) {
      const now = new Date()
      form.startTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    }
  } catch {
    // 错误提示已在请求层处理
  }
}

async function loadStaff() {
  try {
    const res = await medicalStaffApi.getStaffList({ status: 1, page: 1, size: 100 })
    staffList.value = res.list || []
    // 默认选中本人（若在列表中）
    const me = staffList.value.find((s) => s.name === userStore.displayName)
    if (me?.id) form.executorId = me.id
  } catch {
    // 错误提示已在请求层处理
  }
}

async function backfillFromRecord() {
  if (!reserveId.value) return
  try {
    const rec: CareRecord = await reserveApi.getCareRecord(reserveId.value)
    if (!rec) return
    form.visionBeforeBoth = rec.visionBeforeBoth
    form.visionBeforeLeft = rec.visionBeforeLeft
    form.visionBeforeRight = rec.visionBeforeRight
    form.visionAfterBoth = rec.visionAfterBoth
    form.visionAfterLeft = rec.visionAfterLeft
    form.visionAfterRight = rec.visionAfterRight
    if (rec.executorId) form.executorId = rec.executorId
    if (reserve.value?.startTime) {
      form.startTime = reserve.value.startTime.slice(11, 16)
    } else if (rec.createdAt) {
      form.startTime = rec.createdAt.slice(11, 16)
    }
  } catch {
    // 错误提示已在请求层处理
  }
}

function onStaffChange(e: { detail: { value: string | number } }) {
  const staff = staffList.value[Number(e.detail.value)]
  if (staff?.id) form.executorId = staff.id
}

function onTimeChange(e: { detail: { value: string | number } }) {
  form.startTime = String(e.detail.value)
}

const snapshot = ref('')

function capture() {
  snapshot.value = JSON.stringify(form)
}

/** 与加载完成时的快照对比：任何录入/改动都算未保存 */
function isDirty() {
  return !leaving.value && JSON.stringify(form) !== snapshot.value
}

function confirmLeave() {
  uni.showModal({
    title: '放弃填写',
    content: '已录入的数据将不会保存，确认离开吗？',
    confirmText: '确认离开',
    cancelText: '继续填写',
    success: (res) => {
      if (!res.confirm) return
      leaving.value = true
      uni.navigateBack()
    },
  })
}

function onCancel() {
  if (submitting.value) return
  if (!isDirty()) {
    leaving.value = true
    uni.navigateBack()
    return
  }
  confirmLeave()
}

onBackPress(() => {
  if (leaving.value || !isDirty()) return false
  confirmLeave()
  return true
})

async function doStart(): Promise<boolean> {
  if (!reserveId.value) return false
  if (!form.executorId || !form.startTime) {
    toast('请选择养护人与开始时间')
    return false
  }
  if (!form.visionBeforeLeft || !form.visionBeforeRight) {
    const ok = await confirmModal('数据未填写完整', '养护前裸眼视力未录入完整，是否确认开始养护？', '确认开始')
    if (!ok) return false
  }
  await reserveApi.startCare(reserveId.value, {
    executorId: form.executorId,
    executorName: executorName.value,
    startTime: form.startTime || undefined,
    visionBeforeBoth: form.visionBeforeBoth,
    visionBeforeLeft: form.visionBeforeLeft,
    visionBeforeRight: form.visionBeforeRight,
  })
  return true
}

/** 仅开始养护：成功后返回预约列表页 */
async function submitStart() {
  if (submitting.value) return
  submitting.value = true
  try {
    const ok = await doStart()
    if (!ok) return
    leaving.value = true
    uni.showToast({ title: '已开始养护', icon: 'none' })
    setTimeout(() => uni.switchTab({ url: '/pages/reserve/index' }), 600)
  } catch {
    // 错误提示已在请求层处理
  } finally {
    submitting.value = false
  }
}

/** 完成养护（尚未开始时先开始） */
async function submitComplete() {
  if (submitting.value) return
  if (!form.executorId || !form.startTime) {
    toast('请选择养护人与开始时间')
    return
  }
  submitting.value = true
  try {
    if (mode.value === 'start') {
      const ok = await doStart()
      if (!ok) return
    }
    if (!form.visionAfterBoth || !form.visionAfterLeft || !form.visionAfterRight) {
      const ok = await confirmModal('数据未填写完整', '养护后裸眼视力未录入完整，是否确认结束养护？', '确认结束')
      if (!ok) return
    }
    await reserveApi.completeCare(reserveId.value!, {
      visionAfterBoth: form.visionAfterBoth,
      visionAfterLeft: form.visionAfterLeft,
      visionAfterRight: form.visionAfterRight,
    })
    leaving.value = true
    uni.showToast({ title: '养护已完成', icon: 'none' })
    setTimeout(() => uni.switchTab({ url: '/pages/reserve/index' }), 600)
  } catch {
    // 错误提示已在请求层处理
  } finally {
    submitting.value = false
  }
}

function confirmModal(title: string, content: string, confirmText: string): Promise<boolean> {
  return new Promise((resolve) => {
    uni.showModal({
      title,
      content,
      confirmText,
      cancelText: '继续填写',
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    })
  })
}
</script>

<style lang="scss" scoped>
.care {
  min-height: 100vh;
  background: #f4f6f9;
}

.body {
  padding: 24rpx 28rpx 0;
}

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.head-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #1f2937;
}

.head-time {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #1d4ed8;
  font-weight: 500;
}

.head-remark {
  margin-top: 10rpx;
  font-size: 24rpx;
  color: #9ca3af;
}

.select-box.is-locked {
  opacity: 0.6;
}

.submit-holder {
  height: 150rpx;
}
</style>
