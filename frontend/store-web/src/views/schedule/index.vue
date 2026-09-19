<template>
  <div class="schedule-page">
    <!-- 顶部：预约查询汇总 -->
    <el-card class="summary-card">
      <template #header>
        <div class="card-header">
          <el-form inline class="header-form">
            <el-form-item label="日期范围">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                :clearable="false"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchSummary">查询</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>
      <el-row :gutter="20" class="summary-row">
        <el-col :span="8">
          <div class="summary-item">
            <div class="summary-value">{{ summary.total }}</div>
            <div class="summary-label">预约总数</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-item success">
            <div class="summary-value">{{ summary.completed }}</div>
            <div class="summary-label">完成数</div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="summary-item danger">
            <div class="summary-value">{{ summary.cancelled }}</div>
            <div class="summary-label">取消数</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 预约日历 -->
    <el-card class="calendar-card">
      <template #header>
        <div class="card-header">
          <span>预约日历 - {{ currentMonthLabel }}</span>
          <div class="header-actions">
            <el-button size="small" @click="handlePrevMonth">上个月</el-button>
            <el-button size="small" @click="handleToday">今天</el-button>
            <el-button size="small" @click="handleNextMonth">下个月</el-button>
          </div>
        </div>
      </template>

      <el-calendar v-model="currentDate" @change="fetchCalendar">
        <template #date-cell="{ data }">
          <div class="calendar-cell">
            <div class="date-number">{{ data.day.split('-')[2] }}</div>
            <div v-if="data.type === 'current-month'" class="reserve-info">
              <span class="ab-text" :class="{ 'ab-text--muted': isDayEmpty(data.day) }">{{ getDayAbText(data.day) }}</span>
            </div>
            <div class="cell-actions">
              <el-button
                link
                size="small"
                :class="{ 'detail-btn--muted': isDayEmpty(data.day) }"
                @click.stop="handleDetail(data.day)"
              >预约详情</el-button>
              <el-button
                v-if="!isPast(data.day) && isDayFull(data.day)"
                link
                type="info"
                size="small"
                disabled
              >预约已满</el-button>
              <el-button
                v-else-if="!isPast(data.day)"
                link
                type="warning"
                size="small"
                @click.stop="handleAdd(data.day)"
              >添加预约</el-button>
            </div>
          </div>
        </template>
      </el-calendar>
    </el-card>

    <!-- 预约详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`${detailDate} 预约详情`" width="920px">
      <el-table :data="detailList" v-loading="detailLoading" stripe scrollbar-always-on>
        <el-table-column prop="scheduleDate" label="日期" width="110" />
        <el-table-column label="时段" width="130">
          <template #default="{ row }">{{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</template>
        </el-table-column>
        <el-table-column prop="childName" label="儿童姓名" width="100" />
        <el-table-column prop="parentPhone" label="家长电话" width="130" />
        <el-table-column prop="operatorName" label="预约人" width="100">
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="executorName" label="养护人" width="100">
          <template #default="{ row }">{{ row.executorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getReserveStatusType(row.status)">
              {{ getReserveStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!detailLoading && detailList.length === 0" class="detail-empty">该日期暂无预约记录</div>
    </el-dialog>

    <!-- 新建预约弹窗（选儿童 + 可约时段） -->
    <el-dialog
      v-model="reserveDialogVisible"
      title="添加预约"
      width="520px"
      :before-close="handleReserveDialogBeforeClose"
    >
      <el-form :model="reserveForm" :rules="reserveRules" ref="reserveFormRef" label-width="100px">
        <el-form-item label="儿童" prop="childId">
          <el-select
            v-model="reserveForm.childId"
            filterable
            remote
            :remote-method="searchChildren"
            :loading="childSearchLoading"
            placeholder="输入姓名/手机号搜索已审核儿童"
            style="width: 100%;"
          >
            <el-option
              v-for="child in childOptions"
              :key="child.id"
              :label="`${child.name}[${child.phone}]（剩余${child.remainingCount || 0}次）`"
              :value="child.id"
              :disabled="(child.remainingCount || 0) <= 0"
            >
              <span>{{ child.name }}[{{ child.phone }}]（剩余{{ child.remainingCount || 0 }}次）</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期" prop="date">
          <el-date-picker
            v-model="reserveForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            :disabled-date="addDisabledDate"
            @change="loadSlots"
          />
        </el-form-item>
        <el-form-item label="可约时段" prop="slotId">
          <el-select v-model="reserveForm.slotId" placeholder="请先选择日期" style="width: 100%;" :loading="slotLoading">
            <el-option
              v-for="slot in slotOptions"
              :key="slot.id"
              :label="`${slot.slotStartTime}-${slot.slotEndTime}（剩余${slot.maxCapacity - slot.bookedCount}人）`"
              :value="slot.id"
              :disabled="slot.maxCapacity - slot.bookedCount <= 0"
            />
          </el-select>
          <div v-if="reserveForm.date && slotOptions.length === 0 && !slotLoading" class="slot-empty-tip">
            该日期无可约时段，请先在「排班设置」中配置排班规则
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reserveForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleReserveDialogCancel">取消</el-button>
        <el-button type="primary" :loading="reserveSubmitLoading" @click="handleReserveSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreSchedule' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import type { Reserve, ReserveDailyStatistics, ScheduleSlot, Child } from '@/types'
import { scheduleApi, reserveApi, scheduleRuleApi, childApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ==================== 顶部：日期范围汇总统计 ====================
const dateRange = ref<string[]>([])
const summaryLoading = ref(false)
const summary = reactive({ total: 0, completed: 0, cancelled: 0 })

const fetchSummary = async () => {
  if (!dateRange.value || dateRange.value.length < 2) return
  summaryLoading.value = true
  try {
    const list = await reserveApi.getStatistics(dateRange.value[0] as string, dateRange.value[1] as string)
    summary.total = list.reduce((sum: number, item: ReserveDailyStatistics) => sum + (item.total || 0), 0)
    summary.completed = list.reduce((sum: number, item: ReserveDailyStatistics) => sum + (item.completed || 0), 0)
    summary.cancelled = list.reduce((sum: number, item: ReserveDailyStatistics) => sum + (item.cancelled || 0), 0)
  } catch {
    // 错误已在拦截器处理
  } finally {
    summaryLoading.value = false
  }
}

// ==================== 预约日历 ====================
const currentDate = ref(new Date())
/** 当月每日已预约数：date -> Σ已约名额（与容量同源，不含已取消/爽约） */
const dayBookedMap = reactive<Record<string, number>>({})
/** 当月每日总可约人数：date -> Σ开放时段容量（固定值，不受已约影响） */
const dayCapacityMap = reactive<Record<string, number>>({})
/** 当月每日名额状态：date -> 是否已满（无可约时段或全部约满） */
const dayFullMap = reactive<Record<string, boolean>>({})

const formatDate = (d: Date): string => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 日历单元格显示：A/B（已约人数 / 当天可接受预约总数，如 1/4），无排班显示 -/- */
const getDayAbText = (day: string) => {
  const capacity = dayCapacityMap[day] || 0
  return capacity === 0 ? '-/-' : `${dayBookedMap[day] || 0}/${capacity}`
}

const isPast = (day: string) => day < formatDate(new Date())

const isDayFull = (day: string) => dayFullMap[day] === true

/** 无排班（-/-）的日单元格：数值与"预约详情"置灰 */
const isDayEmpty = (day: string) => getDayAbText(day) === '-/-'

/** 加载当月每日名额：A=Σ已约、B=Σ开放时段容量；B 为固定值，不随 A 变化 */
const fetchCalendar = async () => {
  if (!userStore.storeId) return
  const date = currentDate.value
  const start = new Date(date.getFullYear(), date.getMonth(), 1)
  const end = new Date(date.getFullYear(), date.getMonth() + 1, 0)
  const startDate = formatDate(start)
  const endDate = formatDate(end)
  try {
    const list = await scheduleRuleApi.getSlotDailySummary(startDate, endDate, userStore.storeId)
    // 先清空当月旧数据（含排班已删除的日期），再按最新结果回填
    Object.keys(dayCapacityMap).forEach(key => {
      if (key >= startDate && key <= endDate) {
        delete dayCapacityMap[key]
        delete dayBookedMap[key]
        delete dayFullMap[key]
      }
    })
    list.forEach(item => {
      dayBookedMap[item.date] = item.booked
      dayCapacityMap[item.date] = item.total
      dayFullMap[item.date] = item.total - item.booked <= 0
    })
  } catch {
    // 错误已在拦截器处理
  }
}

// ==================== 预约详情弹窗 ====================
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailDate = ref('')
const detailList = ref<Reserve[]>([])

/** 时段显示统一为 HH:mm（后端返回 HH:mm:ss 时去掉秒） */
const formatHm = (t?: string) => (t ? t.slice(0, 5) : '')

const currentMonthLabel = computed(() => `${currentDate.value.getFullYear()}年${currentDate.value.getMonth() + 1}月`)

const goToMonth = (offset: number) => {
  const d = currentDate.value
  currentDate.value = new Date(d.getFullYear(), d.getMonth() + offset, 1)
  fetchCalendar()
}

const handlePrevMonth = () => goToMonth(-1)
const handleNextMonth = () => goToMonth(1)
const handleToday = () => {
  currentDate.value = new Date()
  fetchCalendar()
}

const getReserveStatusType = (status: number) => {
  const map: Record<number, string> = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}

const getReserveStatusText = (status: number) => {
  const map: Record<number, string> = { 1: '已预约', 2: '养护中', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

const handleDetail = async (day: string) => {
  detailDate.value = day
  detailVisible.value = true
  detailLoading.value = true
  detailList.value = []
  try {
    const res = await reserveApi.getReserveList({
      storeId: userStore.storeId,
      date: day,
      page: 1,
      size: 100
    })
    detailList.value = res.list
  } catch {
    // 错误已在拦截器处理
  } finally {
    detailLoading.value = false
  }
}

// ==================== 新建预约（选儿童 + 可约时段） ====================
const reserveDialogVisible = ref(false)
const reserveSubmitLoading = ref(false)
const reserveFormRef = ref<FormInstance>()
const reserveForm = reactive({
  childId: undefined as number | undefined,
  date: '',
  slotId: undefined as string | undefined,
  remark: ''
})
/** 打开弹窗时的表单快照：与当前表单不一致视为有未保存数据 */
const reserveFormSnapshot = ref('')
const childOptions = ref<Child[]>([])
const childSearchLoading = ref(false)
const slotOptions = ref<ScheduleSlot[]>([])
const slotLoading = ref(false)

const reserveRules = {
  childId: [{ required: true, message: '请选择儿童', trigger: 'change' }],
  date: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  slotId: [{ required: true, message: '请选择可约时段', trigger: 'change' }]
}

const searchChildren = async (keyword: string) => {
  if (!keyword || !keyword.trim()) return
  childSearchLoading.value = true
  try {
    const res = await childApi.pickOptions({
      storeId: userStore.storeId,
      keyword: keyword.trim()
    })
    childOptions.value = res
  } catch {
    // 错误已在拦截器处理
  } finally {
    childSearchLoading.value = false
  }
}

const loadSlots = async () => {
  reserveForm.slotId = undefined
  slotOptions.value = []
  if (!reserveForm.date) return
  slotLoading.value = true
  try {
    slotOptions.value = await scheduleRuleApi.getSlots(reserveForm.date, userStore.storeId || undefined)
    if (slotOptions.value.length === 0) {
      ElMessage.warning('该日期无可约时段，请重新选择')
      reserveForm.date = ''
    }
  } catch {
    // 错误已在拦截器处理
  } finally {
    slotLoading.value = false
  }
}

/** 近30天有可约时段的日期集合（打开添加弹窗时拉取，禁选其余日期） */
const availableDays = ref<Set<string>>(new Set())
const daysLoading = ref(false)

const loadAvailability = async () => {
  daysLoading.value = true
  availableDays.value = new Set()
  try {
    const now = new Date()
    const start = formatDate(now)
    const end = formatDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 29))
    const dates = await scheduleRuleApi.getAvailableDates(start, end, userStore.storeId || undefined)
    availableDays.value = new Set(dates)
  } catch {
    // 拉取失败不禁选，选中后由时段查询兜底校验
  } finally {
    daysLoading.value = false
  }
}

const addDisabledDate = (d: Date) => {
  if (d.getTime() < Date.now() - 86400000) return true
  // 可用日期加载完成前不禁用（选中后仍由时段查询兜底校验）
  if (daysLoading.value) return false
  return !availableDays.value.has(formatDate(d))
}

const handleAdd = async (day: string) => {
  Object.assign(reserveForm, {
    childId: undefined,
    date: day,
    slotId: undefined,
    remark: ''
  })
  reserveFormSnapshot.value = JSON.stringify(reserveForm)
  childOptions.value = []
  loadAvailability()
  reserveDialogVisible.value = true
  await loadSlots()
  // 时段加载可能清空日期（无可约时段），以最终结果作为未改动基线
  reserveFormSnapshot.value = JSON.stringify(reserveForm)
}

/** 有未保存数据时弹确认框；返回 true 表示可以关闭 */
const confirmReserveClose = async (): Promise<boolean> => {
  if (JSON.stringify(reserveForm) === reserveFormSnapshot.value) return true
  try {
    await ElMessageBox.confirm('确认关闭吗？已填写的数据将不会保存', '关闭确认', {
      confirmButtonText: '确认关闭',
      cancelButtonText: '继续填写',
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

const handleReserveDialogCancel = async () => {
  if (await confirmReserveClose()) reserveDialogVisible.value = false
}

const handleReserveDialogBeforeClose = async (done: () => void) => {
  if (await confirmReserveClose()) done()
}

const handleReserveSubmit = async () => {
  if (!reserveFormRef.value) return
  await reserveFormRef.value.validate()
  reserveSubmitLoading.value = true
  try {
    await reserveApi.createReserveV2({
      childId: reserveForm.childId!,
      slotId: reserveForm.slotId!,
      remark: reserveForm.remark || undefined
    })
    ElMessage.success('预约创建成功，已扣减1次可约次数')
    reserveDialogVisible.value = false
    // 刷新日历名额与顶部统计
    fetchCalendar()
    fetchSummary()
  } catch {
    // 错误已在拦截器处理
  } finally {
    reserveSubmitLoading.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  // 默认统计当月
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  dateRange.value = [formatDate(start), formatDate(end)]
  fetchSummary()
  fetchCalendar()
})
</script>

<style scoped lang="scss">
.schedule-page {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;

    .header-form {
      :deep(.el-form-item) {
        margin-bottom: 0;
      }
    }
  }

  .calendar-card {
    :deep(.el-calendar__header) {
      display: none;
    }
  }

  .summary-row {
    .summary-item {
      background: #f0f5ff;
      border-radius: 8px;
      padding: 16px;
      text-align: center;

      &.success {
        background: #f0f9eb;
      }

      &.danger {
        background: #fef0f0;
      }

      .summary-value {
        font-size: 26px;
        font-weight: bold;
        color: #303133;
      }

      .summary-label {
        color: #909399;
        font-size: 13px;
        margin-top: 4px;
      }
    }
  }

  .calendar-cell {
    height: 100%;
    display: flex;
    flex-direction: column;

    .date-number {
      font-size: 14px;
    }

    .reserve-info {
      margin: 4px 0;
      text-align: center;

      .ab-text {
        font-size: 18px;
        font-weight: 700;
        color: #409eff;

        &.ab-text--muted {
          color: #c0c4cc;
        }
      }
    }

    .cell-actions {
      margin-top: auto;
      display: flex;
      flex-wrap: wrap;
      gap: 0 4px;

      :deep(.el-button + .el-button) {
        margin-left: 0;
      }

      :deep(.el-button.detail-btn--muted) {
        --el-button-text-color: #c0c4cc;
        --el-button-hover-text-color: #c0c4cc;
        --el-button-active-text-color: #c0c4cc;
      }
    }
  }

  .detail-empty {
    text-align: center;
    color: #909399;
    padding: 24px 0;
  }

  .slot-empty-tip {
    color: #e6a23c;
    font-size: 12px;
    margin-top: 4px;
  }
}
</style>
