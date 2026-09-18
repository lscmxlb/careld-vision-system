<template>
  <view class="create">
    <view class="body">
      <!-- 调整模式：展示当前预约 -->
      <view v-if="isAdjust" class="card">
        <view class="section-title">当前预约</view>
        <view v-if="adjusting" class="adjust-cur">
          <text class="ac-name">{{ adjusting.childName }}</text>
          <text class="ac-slot">{{ adjusting.scheduleDate }} {{ formatHm(adjusting.timeSlotStart) }}-{{ formatHm(adjusting.timeSlotEnd) }}</text>
        </view>
        <view v-else class="loading-tip">加载中…</view>
      </view>

      <!-- 新建模式：选择儿童 -->
      <view v-else class="card">
        <view class="section-title">1. 选择儿童</view>
        <view class="search">
          <text class="search-icon">🔍</text>
          <input
            v-model="childKeyword"
            class="search-input"
            placeholder="输入姓名或手机号筛选"
            placeholder-class="ph"
          />
          <text v-if="childKeyword" class="search-clear" @click="childKeyword = ''">✕</text>
        </view>

        <view v-if="childrenLoading" class="loading-tip">加载中…</view>
        <scroll-view v-else class="child-scroll" scroll-y>
          <view
            v-for="item in filteredChildren"
            :key="item.id"
            class="child-row"
            :class="{ 'child-on': selectedChild?.id === item.id }"
            @click="selectChild(item)"
          >
            <view class="cr-main">
              <view class="cr-name-row">
                <text class="cr-name">{{ item.name }}</text>
                <text class="cr-meta">{{ genderText(item.gender) }} · {{ ageText(item.birthDate, item.age) }}</text>
              </view>
              <text class="cr-phone">{{ item.phone || '-' }}</text>
            </view>
            <view class="cr-right">
              <text class="tag" :class="(item.remainingCount || 0) > 0 ? 'tag-primary' : 'tag-grey'">
                剩余 {{ item.remainingCount || 0 }} 次
              </text>
              <text v-if="selectedChild?.id === item.id" class="cr-check">✓</text>
            </view>
          </view>
          <view v-if="!filteredChildren.length" class="mini-empty">
            没有匹配的档案（仅显示已审核且启用的儿童）
          </view>
        </scroll-view>
      </view>

      <!-- 选择日期 -->
      <view class="card">
        <view class="section-title">
          <text>{{ isAdjust ? '1' : '2' }}. 选择日期</text>
          <text v-if="daysLoading" class="section-more">加载中…</text>
        </view>
        <scroll-view v-if="availableDates.length" class="date-strip" scroll-x :show-scrollbar="false">
          <view class="strip-inner">
            <view
              v-for="d in availableDates"
              :key="d.value"
              class="date-cell"
              :class="{ 'date-on': form.date === d.value }"
              @click="pickDate(d.value)"
            >
              <text class="dc-main">{{ d.label }}</text>
              <text class="dc-sub">{{ d.sub }}</text>
            </view>
          </view>
        </scroll-view>
        <view v-else-if="!daysLoading" class="mini-empty">
          近30天暂无可约日期，请先在 PC 端「排班设置」中配置排班规则
        </view>
      </view>

      <!-- 选择时段 -->
      <view class="card">
        <view class="section-title">{{ isAdjust ? '2' : '3' }}. 选择时段</view>
        <view v-if="!form.date" class="mini-empty">请先选择日期</view>
        <view v-else-if="slotLoading" class="loading-tip">加载中…</view>
        <view v-else-if="!slots.length" class="mini-empty">该日期无可约时段，请重新选择日期</view>
        <view v-else class="slot-list">
          <view
            v-for="slot in slots"
            :key="slot.id"
            class="slot"
            :class="{ 'slot-on': form.slotId === slot.id, 'slot-off': slotRemaining(slot) <= 0 || slot.status !== 1 }"
            @click="pickSlot(slot)"
          >
            <text class="slot-time">{{ formatHm(slot.slotStartTime) }} - {{ formatHm(slot.slotEndTime) }}</text>
            <text class="slot-cap">
              {{ slot.status !== 1 ? '已关闭' : slotRemaining(slot) <= 0 ? '预约已满' : `剩余 ${slotRemaining(slot)} 人` }}
            </text>
          </view>
        </view>
      </view>

      <!-- 备注 -->
      <view class="card">
        <view class="section-title">备注</view>
        <textarea
          v-model="form.remark"
          class="remark"
          maxlength="100"
          placeholder="选填，如特殊说明"
          placeholder-class="ph"
        />
      </view>

      <view class="safe-bottom" />
      <view class="submit-holder" />
    </view>

    <view class="sticky-bar">
      <view v-if="!isAdjust && selectedChild" class="bar-summary">
        <text class="bs-name">{{ selectedChild.name }}</text>
        <text class="bs-sub">剩余 {{ selectedChild.remainingCount || 0 }} 次 · 成功预约将扣减 1 次</text>
      </view>
      <view class="bar-row">
        <view class="btn btn-block btn-primary" :class="{ 'is-disabled': !canSubmit || submitting }" @click="submit">
          {{ submitting ? '提交中…' : isAdjust ? '确认调整' : '确认预约' }}
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { onBackPress, onLoad } from '@dcloudio/uni-app'
import { childApi, reserveApi, scheduleRuleApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { addDays, ageText, formatDate, formatHm, todayStr } from '@/utils/format'
import { toast } from '@/utils/request'
import type { Child, Reserve, ScheduleSlot } from '@/types'

const userStore = useUserStore()

const isAdjust = ref(false)
const adjustId = ref<number | null>(null)
const adjusting = ref<Reserve | null>(null)

const childKeyword = ref('')
const children = ref<Child[]>([])
const childrenLoading = ref(false)
const selectedChild = ref<Child | null>(null)

const availableDates = ref<Array<{ value: string; label: string; sub: string }>>([])
const daysLoading = ref(false)

const slots = ref<ScheduleSlot[]>([])
const slotLoading = ref(false)

const form = reactive({ date: '', slotId: '', remark: '' })
const submitting = ref(false)
const leaving = ref(false)

const filteredChildren = computed(() => {
  const kw = childKeyword.value.trim()
  if (!kw) return children.value
  return children.value.filter((c) => (c.name || '').includes(kw) || (c.phone || '').includes(kw))
})

const canSubmit = computed(() => {
  if (!form.slotId) return false
  if (isAdjust.value) return true
  return !!selectedChild.value
})

function genderText(gender?: number) {
  return gender === 1 ? '男' : gender === 0 ? '女' : '未填'
}

function slotRemaining(slot: ScheduleSlot) {
  return (slot.maxCapacity || 0) - (slot.bookedCount || 0)
}

onLoad(async (query) => {
  isAdjust.value = query?.mode === 'adjust'
  if (isAdjust.value) {
    adjustId.value = Number(query?.id)
    uni.setNavigationBarTitle({ title: '预约调整' })
    await loadAdjusting()
  } else {
    uni.setNavigationBarTitle({ title: '新建预约' })
    loadChildren()
  }
  loadAvailableDates()
})

async function loadAdjusting() {
  if (!adjustId.value) return
  try {
    adjusting.value = await reserveApi.getReserveDetail(adjustId.value)
  } catch {
    // 错误提示已在请求层处理
  }
}

async function loadChildren() {
  childrenLoading.value = true
  try {
    children.value = await childApi.pickOptions({ storeId: userStore.storeId })
  } catch {
    // 错误提示已在请求层处理
  } finally {
    childrenLoading.value = false
  }
}

async function loadAvailableDates() {
  daysLoading.value = true
  try {
    const start = todayStr()
    const end = formatDate(addDays(new Date(`${start}T00:00:00`), 29))
    const dates = await scheduleRuleApi.getAvailableDates(start, end, userStore.storeId)
    availableDates.value = (dates || []).map((value) => {
      const d = new Date(`${value}T00:00:00`)
      const isToday = value === start
      return {
        value,
        label: isToday ? '今天' : value.slice(5).replace('-', '/'),
        sub: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()],
      }
    })
  } catch {
    // 错误提示已在请求层处理
  } finally {
    daysLoading.value = false
  }
}

function selectChild(child: Child) {
  selectedChild.value = child
}

async function pickDate(value: string) {
  if (form.date === value) return
  // 换日期后时段失效，需重选
  form.date = value
  form.slotId = ''
  await loadSlots()
}

async function loadSlots() {
  if (!form.date) return
  slotLoading.value = true
  try {
    slots.value = await scheduleRuleApi.getSlots(form.date, userStore.storeId)
  } catch {
    // 错误提示已在请求层处理
  } finally {
    slotLoading.value = false
  }
}

function pickSlot(slot: ScheduleSlot) {
  if (slot.status !== 1) {
    toast('该时段已关闭')
    return
  }
  if (slotRemaining(slot) <= 0) {
    toast('该时段预约已满')
    return
  }
  form.slotId = slot.id
}

/** 有未保存内容时返回需确认（脏保护） */
function isDirty() {
  if (leaving.value) return false
  return !!form.slotId || !!form.date || !!form.remark || (!isAdjust.value && !!selectedChild.value)
}

onBackPress(() => {
  if (!isDirty()) return false
  uni.showModal({
    title: '放弃填写',
    content: '确认离开吗？已填写的数据将不会保存',
    confirmText: '确认离开',
    cancelText: '继续填写',
    success: (res) => {
      if (!res.confirm) return
      leaving.value = true
      uni.navigateBack()
    },
  })
  return true
})

async function submit() {
  if (submitting.value) return
  if (!isAdjust.value && !selectedChild.value) {
    toast('请选择儿童')
    return
  }
  if (!form.date) {
    toast('请选择预约日期')
    return
  }
  if (!form.slotId) {
    toast('请选择可约时段')
    return
  }
  submitting.value = true
  try {
    if (isAdjust.value && adjustId.value) {
      await reserveApi.adjustReserve(adjustId.value, form.slotId)
      leaving.value = true
      uni.showToast({ title: '预约已调整', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 600)
    } else {
      await reserveApi.createReserveV2({
        childId: selectedChild.value!.id,
        slotId: form.slotId,
        remark: form.remark.trim() || undefined,
      })
      leaving.value = true
      uni.showToast({ title: '预约创建成功，已扣减1次可约次数', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 800)
    }
  } catch {
    // 错误提示已在请求层处理
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.create {
  min-height: 100vh;
  background: #f4f6f9;
}

.body {
  padding: 24rpx 28rpx 0;
}

.adjust-cur {
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
}

.ac-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
}

.ac-slot {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #1d4ed8;
}

.search {
  display: flex;
  align-items: center;
  height: 72rpx;
  background: #f2f5f8;
  border-radius: 36rpx;
  padding: 0 24rpx;
  margin-bottom: 16rpx;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  color: #1f2937;
  height: 100%;
}

.ph {
  color: #b7bfc9;
  font-size: 27rpx;
}

.search-clear {
  font-size: 26rpx;
  color: #b7bfc9;
  padding-left: 12rpx;
}

.child-scroll {
  max-height: 620rpx;
}

.child-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 20rpx;
  border-radius: 14rpx;
  border: 2rpx solid #eef1f5;
  margin-bottom: 14rpx;
}

.child-on {
  border-color: #2563eb;
  background: #f0f7ff;
}

.cr-main {
  min-width: 0;
  flex: 1;
}

.cr-name-row {
  display: flex;
  align-items: baseline;
}

.cr-name {
  font-size: 29rpx;
  font-weight: 600;
  color: #1f2937;
  margin-right: 12rpx;
}

.cr-meta {
  font-size: 23rpx;
  color: #9ca3af;
}

.cr-phone {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #5b6572;
}

.cr-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.cr-check {
  margin-left: 12rpx;
  color: #2563eb;
  font-size: 32rpx;
  font-weight: 700;
}

.mini-empty {
  padding: 32rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #9ca3af;
}

.date-strip {
  white-space: nowrap;
}

.strip-inner {
  display: inline-flex;
  gap: 16rpx;
  padding: 4rpx 0;
}

.date-cell {
  min-width: 112rpx;
  padding: 14rpx 18rpx;
  border-radius: 14rpx;
  background: #f7f9fb;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
}

.date-on {
  background: #2563eb;
}

.dc-main {
  font-size: 26rpx;
  color: #1f2937;
  font-weight: 500;
}

.dc-sub {
  font-size: 21rpx;
  color: #9ca3af;
  margin-top: 4rpx;
}

.date-on .dc-main,
.date-on .dc-sub {
  color: #fff;
}

.slot-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.slot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx;
  border-radius: 14rpx;
  border: 2rpx solid #eef1f5;
}

.slot-on {
  border-color: #2563eb;
  background: #f0f7ff;
}

.slot-off {
  background: #f6f8fa;
  opacity: 0.6;
}

.slot-time {
  font-size: 29rpx;
  font-weight: 600;
  color: #1f2937;
}

.slot-cap {
  font-size: 24rpx;
  color: #5b6572;
}

.remark {
  width: 100%;
  height: 150rpx;
  background: #f7f9fb;
  border-radius: 12rpx;
  padding: 20rpx 24rpx;
  font-size: 27rpx;
  color: #1f2937;
  box-sizing: border-box;
}

.submit-holder {
  height: 160rpx;
}

.bar-summary {
  display: flex;
  align-items: baseline;
  padding: 0 8rpx 12rpx;
}

.bs-name {
  font-size: 27rpx;
  font-weight: 600;
  color: #1f2937;
  margin-right: 14rpx;
}

.bs-sub {
  font-size: 23rpx;
  color: #9ca3af;
}
</style>
