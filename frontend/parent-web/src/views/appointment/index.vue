<template>
  <div class="appointment-page">
    <!-- 步骤指示器 -->
    <div class="steps-bar">
      <div
        v-for="(step, index) in steps"
        :key="index"
        class="step-item"
        :class="{ active: currentStep === index, done: currentStep > index }"
      >
        <div class="step-circle">
          <span v-if="currentStep > index">✓</span>
          <span v-else>{{ index + 1 }}</span>
        </div>
        <span class="step-label">{{ step }}</span>
      </div>
    </div>

    <!-- Step 1: 选择门店 -->
    <div v-show="currentStep === 0" class="step-content">
      <div v-if="storeLoading" class="loading-placeholder">
        <el-skeleton :rows="5" animated />
      </div>
      <div v-else class="store-list">
        <div
          class="store-card"
          v-for="store in stores"
          :key="store.id"
          :class="{ selected: selectedStore?.id === store.id }"
          @click="selectStore(store)"
        >
          <div class="store-name">{{ store.storeName }}</div>
          <div class="store-address">
            <el-icon><Location /></el-icon>
            {{ store.address }}
          </div>
          <div class="store-meta">
            <span>营业时间: {{ store.businessHours }}</span>
            <span>联系电话: {{ store.contactPhone }}</span>
          </div>
        </div>
        <el-empty v-if="stores.length === 0" description="暂无可用门店" />
      </div>
    </div>

    <!-- Step 2: 选择孩子 -->
    <div v-show="currentStep === 1" class="step-content">
      <div v-if="childLoading" class="loading-placeholder">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else class="child-list">
        <div
          class="child-card"
          v-for="child in children"
          :key="child.id"
          :class="{ selected: selectedChild?.id === child.id }"
          @click="selectChild(child)"
        >
          <el-avatar :size="48" :icon="UserFilled" />
          <div class="child-info">
            <div class="child-name">{{ child.nameMask || child.name }}</div>
            <div class="child-detail">{{ calcAge(child.birthDate) }}岁 | {{ child.eyeCondition || '暂无记录' }}</div>
          </div>
          <el-icon v-if="selectedChild?.id === child.id" class="check-icon"><CircleCheckFilled /></el-icon>
        </div>
        <el-empty v-if="children.length === 0" description="暂无绑定的孩子">
          <el-button type="primary" @click="$router.push('/profile')">去添加</el-button>
        </el-empty>
      </div>
    </div>

    <!-- Step 3: 选择日期和时间 -->
    <div v-show="currentStep === 2" class="step-content">
      <div v-if="scheduleLoading" class="loading-placeholder">
        <el-skeleton :rows="5" animated />
      </div>
      <template v-else>
        <!-- 日期选择 -->
        <div class="date-section">
          <h4>选择日期</h4>
          <div class="date-grid">
            <div
              v-for="date in availableDates"
              :key="date"
              class="date-item"
              :class="{ selected: selectedDate === date }"
              @click="selectDate(date)"
            >
              <span class="date-week">{{ getWeekDay(date) }}</span>
              <span class="date-day">{{ date.substring(8, 10) }}</span>
              <span class="date-month">{{ date.substring(5, 7) }}月</span>
            </div>
          </div>
        </div>

        <!-- 时间段选择 -->
        <div class="time-section" v-if="selectedDate && timeSlots.length > 0">
          <h4>选择时间段</h4>
          <div class="time-grid">
            <div
              v-for="slot in timeSlots"
              :key="slot.id"
              class="time-item"
              :class="{ selected: selectedSchedule?.id === slot.id, disabled: (slot.availableCount ?? 0) <= 0 }"
              @click="selectTimeSlot(slot)"
            >
              <span>{{ fmtTime(slot.timeSlotStart) }}-{{ fmtTime(slot.timeSlotEnd) }}</span>
              <span class="slot-avail">{{ (slot.availableCount ?? 0) > 0 ? `余${slot.availableCount}位` : '已满' }}</span>
            </div>
          </div>
        </div>

        <el-empty v-if="selectedDate && timeSlots.length === 0" description="该日期暂无可用时段" />
      </template>
    </div>

    <!-- Step 4: 确认预约 -->
    <div v-show="currentStep === 3" class="step-content">
      <div class="confirm-card">
        <h4>预约确认</h4>
        <div class="confirm-row">
          <span class="label">门店</span>
          <span class="value">{{ selectedStore?.storeName }}</span>
        </div>
        <div class="confirm-row">
          <span class="label">孩子</span>
          <span class="value">{{ selectedChild?.nameMask || selectedChild?.name }}</span>
        </div>
        <div class="confirm-row">
          <span class="label">日期</span>
          <span class="value">{{ selectedDate }}</span>
        </div>
        <div class="confirm-row">
          <span class="label">时间段</span>
          <span class="value">{{ fmtTime(selectedSchedule?.timeSlotStart) }} - {{ fmtTime(selectedSchedule?.timeSlotEnd) }}</span>
        </div>
        <div class="confirm-row">
          <span class="label">技师</span>
          <span class="value">{{ selectedSchedule?.technicianName }}</span>
        </div>

        <el-form-item label="备注" style="margin-top: 16px;">
          <el-input v-model="remark" type="textarea" :rows="2" placeholder="选填，如有特殊情况请备注" />
        </el-form-item>
      </div>
    </div>

    <!-- 成功页面 -->
    <div v-if="currentStep === 4" class="step-content success-page">
      <el-result icon="success" title="预约成功" sub-title="请按时到店进行视力养护">
        <template #extra>
          <el-button type="primary" @click="$router.push('/appointment/list')">查看我的预约</el-button>
          <el-button @click="resetForm">继续预约</el-button>
        </template>
      </el-result>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar" v-if="currentStep < 4">
      <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
      <el-button
        v-if="currentStep < 3"
        type="primary"
        :disabled="!canNext"
        @click="currentStep++"
      >
        下一步
      </el-button>
      <el-button
        v-if="currentStep === 3"
        type="primary"
        :loading="submitLoading"
        @click="handleSubmit"
      >
        确认预约
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ParentAppointment' })
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Location, UserFilled, CircleCheckFilled } from '@element-plus/icons-vue'
import type { Store, Child, Schedule } from '@/types'
import { storeApi, childApi, scheduleApi, reserveApi } from '@/api'

const steps = ['选择门店', '选择孩子', '选择时间', '确认预约']
const currentStep = ref(0)
const submitLoading = ref(false)

// Step 1
const storeLoading = ref(false)
const stores = ref<Store[]>([])
const selectedStore = ref<Store | null>(null)

// Step 2
const childLoading = ref(false)
const children = ref<Child[]>([])
const selectedChild = ref<Child | null>(null)

// Step 3
const scheduleLoading = ref(false)
const availableDates = ref<string[]>([])
const timeSlots = ref<Schedule[]>([])
const selectedDate = ref('')
const selectedSchedule = ref<Schedule | null>(null)

// Step 4
const remark = ref('')

// 是否可以进入下一步
const canNext = computed(() => {
  switch (currentStep.value) {
    case 0: return !!selectedStore.value
    case 1: return !!selectedChild.value
    case 2: return !!selectedSchedule.value
    default: return false
  }
})

// 选择门店
const selectStore = (store: Store) => {
  selectedStore.value = store
}

// 选择孩子
const selectChild = (child: Child) => {
  selectedChild.value = child
}

// 选择日期
const selectDate = async (date: string) => {
  selectedDate.value = date
  selectedSchedule.value = null
  await fetchTimeSlots(date)
}

// 选择时间段
const selectTimeSlot = (slot: Schedule) => {
  if (slot.availableCount <= 0) {
    ElMessage.warning('该时段已约满')
    return
  }
  selectedSchedule.value = slot
}

// 获取门店列表
const fetchStores = async () => {
  storeLoading.value = true
  try {
    stores.value = await storeApi.getStoreList()
  } catch {
    ElMessage.error('获取门店列表失败')
  } finally {
    storeLoading.value = false
  }
}

// 获取孩子列表
const fetchChildren = async () => {
  childLoading.value = true
  try {
    children.value = await childApi.getMyChildren()
  } catch {
    ElMessage.error('获取孩子列表失败')
  } finally {
    childLoading.value = false
  }
}

// 获取可用日期
const fetchAvailableDates = async () => {
  if (!selectedStore.value) return

  const today = new Date()
  const endDate = new Date()
  endDate.setDate(today.getDate() + 14)

  const startDateStr = formatDate(today)
  const endDateStr = formatDate(endDate)

  scheduleLoading.value = true
  try {
    const scheduleMap = await scheduleApi.getAvailableSchedules(
      selectedStore.value.id,
      startDateStr,
      endDateStr
    )
    availableDates.value = Object.keys(scheduleMap).sort()

    // 默认选择第一个有排班的日期
    if (availableDates.value.length > 0) {
      await selectDate(availableDates.value[0] ?? '')
    }
  } catch {
    ElMessage.error('获取排班信息失败')
  } finally {
    scheduleLoading.value = false
  }
}

// 获取时间段
const fetchTimeSlots = async (date: string) => {
  if (!selectedStore.value) return

  try {
    const scheduleMap = await scheduleApi.getAvailableSchedules(
      selectedStore.value.id,
      date,
      date
    )
    timeSlots.value = scheduleMap[date] || []
  } catch {
    ElMessage.error('获取时间段失败')
  }
}

// 提交预约
const handleSubmit = async () => {
  if (!selectedStore.value || !selectedChild.value || !selectedSchedule.value) {
    ElMessage.warning('请填写完整信息')
    return
  }

  submitLoading.value = true
  try {
    await reserveApi.createReservation({
      childId: selectedChild.value.id,
      storeId: selectedStore.value.id,
      scheduleId: selectedSchedule.value.id,
      reserveType: 2, // 2=养护
      remark: remark.value
    })
    currentStep.value = 4
    ElMessage.success('预约成功')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '预约失败')
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  currentStep.value = 0
  selectedStore.value = null
  selectedChild.value = null
  selectedDate.value = ''
  selectedSchedule.value = null
  remark.value = ''
  timeSlots.value = []
}

// 获取星期
const getWeekDay = (dateStr: string) => {
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return days[new Date(dateStr).getDay()]
}

// 从 birthDate 计算年龄
const calcAge = (birthDateStr: string | undefined): string => {
  if (!birthDateStr) return '?'
  const birth = new Date(birthDateStr)
  const today = new Date()
  let age = today.getFullYear() - birth.getFullYear()
  const monthDiff = today.getMonth() - birth.getMonth()
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
    age--
  }
  return age >= 0 ? String(age) : '?'
}

// 格式化时间：去掉秒 "09:00:00" → "09:00"
const fmtTime = (t: string | undefined): string => {
  if (!t) return ''
  return t.substring(0, 5)
}

// 日期格式化
const formatDate = (date: Date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

onMounted(async () => {
  await Promise.all([fetchStores(), fetchChildren()])
})

// 监听步骤变化
import { watch } from 'vue'
watch(currentStep, (step) => {
  if (step === 2 && selectedStore.value) {
    fetchAvailableDates()
  }
})
</script>

<style scoped lang="scss">
.appointment-page {
  padding: 16px;
  padding-bottom: 80px;

  .steps-bar {
    display: flex;
    justify-content: space-between;
    margin-bottom: 24px;
    padding: 0 8px;

    .step-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      position: relative;

      .step-circle {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: #e8e8e8;
        color: #999;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        margin-bottom: 6px;
        transition: all 0.3s;
      }

      .step-label {
        font-size: 11px;
        color: #999;
        white-space: nowrap;
      }

      &.active {
        .step-circle {
          background: #1890ff;
          color: #fff;
        }
        .step-label {
          color: #1890ff;
          font-weight: bold;
        }
      }

      &.done {
        .step-circle {
          background: #52c41a;
          color: #fff;
        }
      }
    }
  }

  .step-content {
    min-height: 300px;
  }

  .loading-placeholder {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
  }

  // 门店列表
  .store-list {
    .store-card {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      border: 2px solid transparent;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      cursor: pointer;
      transition: all 0.2s;

      &.selected {
        border-color: #1890ff;
        background: #f0f7ff;
      }

      &:active {
        transform: scale(0.98);
      }

      .store-name {
        font-size: 16px;
        font-weight: bold;
        margin-bottom: 8px;
      }

      .store-address {
        font-size: 13px;
        color: #666;
        display: flex;
        align-items: center;
        gap: 4px;
        margin-bottom: 8px;
      }

      .store-meta {
        display: flex;
        flex-direction: column;
        gap: 4px;
        font-size: 12px;
        color: #999;
      }
    }
  }

  // 孩子列表
  .child-list {
    .child-card {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      border: 2px solid transparent;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 12px;
      transition: all 0.2s;

      &.selected {
        border-color: #1890ff;
        background: #f0f7ff;
      }

      .child-info {
        flex: 1;

        .child-name {
          font-size: 16px;
          font-weight: bold;
          margin-bottom: 4px;
        }

        .child-detail {
          font-size: 13px;
          color: #666;
        }
      }

      .check-icon {
        color: #1890ff;
        font-size: 24px;
      }
    }
  }

  // 日期选择
  .date-section,
  .time-section {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 16px;

    h4 {
      font-size: 15px;
      margin-bottom: 12px;
      color: #333;
    }
  }

  .date-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 8px;

    .date-item {
      text-align: center;
      padding: 8px 4px;
      border-radius: 8px;
      background: #f7f8fa;
      cursor: pointer;
      transition: all 0.2s;

      &.selected {
        background: #1890ff;
        color: #fff;

        .date-week, .date-month { color: rgba(255, 255, 255, 0.8); }
        .date-day { color: #fff; }
      }

      .date-week {
        display: block;
        font-size: 11px;
        color: #999;
        margin-bottom: 2px;
      }

      .date-day {
        display: block;
        font-size: 18px;
        font-weight: bold;
      }

      .date-month {
        display: block;
        font-size: 11px;
        color: #999;
      }
    }
  }

  .time-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;

    .time-item {
      text-align: center;
      padding: 12px 8px;
      border-radius: 8px;
      background: #f7f8fa;
      cursor: pointer;
      transition: all 0.2s;

      &.selected {
        background: #1890ff;
        color: #fff;

        .slot-avail { color: rgba(255, 255, 255, 0.8); }
      }

      &.disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }

      span {
        display: block;
        font-size: 14px;
      }

      .slot-avail {
        font-size: 11px;
        color: #999;
        margin-top: 4px;
      }
    }
  }

  // 确认卡片
  .confirm-card {
    background: #fff;
    border-radius: 12px;
    padding: 20px;

    h4 {
      font-size: 16px;
      margin-bottom: 16px;
      color: #333;
    }

    .confirm-row {
      display: flex;
      padding: 10px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-of-type {
        border-bottom: none;
      }

      .label {
        width: 70px;
        color: #999;
        font-size: 14px;
      }

      .value {
        flex: 1;
        color: #333;
        font-size: 14px;
      }
    }
  }

  // 成功页
  .success-page {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 400px;
  }

  // 底部操作栏
  .bottom-bar {
    position: fixed;
    bottom: 60px;
    left: 0;
    right: 0;
    padding: 12px 16px;
    background: #fff;
    border-top: 1px solid #e8e8e8;
    display: flex;
    justify-content: space-between;

    .el-button {
      flex: 1;
    }
  }
}
</style>
