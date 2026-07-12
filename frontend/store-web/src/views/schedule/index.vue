<template>
  <div class="schedule-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>排班管理</span>
          <el-button type="primary" @click="handleBatchCreate">
            <el-icon><Plus /></el-icon>批量排班
          </el-button>
        </div>
      </template>

      <!-- 日历 -->
      <el-calendar v-model="currentDate" @change="handleDateChange">
        <template #date-cell="{ data }">
          <div class="calendar-cell">
            <div class="date-number">{{ data.day.split('-')[2] }}</div>
            <div class="schedule-info" v-if="getScheduleCount(data.day) > 0">
              <el-tag size="small" type="success">
                {{ getScheduleCount(data.day) }}个时段
              </el-tag>
            </div>
          </div>
        </template>
      </el-calendar>
    </el-card>

    <!-- 预约管理 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>预约管理</span>
          <div class="reserve-actions">
            <el-radio-group v-model="reserveFilter.status" size="small" @change="fetchReserves">
              <el-radio-button :label="undefined">全部</el-radio-button>
              <el-radio-button :label="0">待服务</el-radio-button>
              <el-radio-button :label="1">已完成</el-radio-button>
              <el-radio-button :label="2">已取消</el-radio-button>
            </el-radio-group>
            <el-button type="primary" size="small" @click="handleCreateReserve" style="margin-left: 10px;">
              <el-icon><Plus /></el-icon>新建预约
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="reserveList" v-loading="reserveLoading" stripe>
        <el-table-column prop="scheduleDate" label="日期" width="120" />
        <el-table-column prop="timeSlot" label="时段" width="120">
          <template #default="{ row }">{{ row.timeSlotStart }}-{{ row.timeSlotEnd }}</template>
        </el-table-column>
        <el-table-column prop="technicianName" label="技师" width="100" />
        <el-table-column prop="childName" label="儿童姓名" width="100" />
        <el-table-column prop="parentPhone" label="家长电话" width="130" />
        <el-table-column prop="reserveType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ getReserveTypeText(row.reserveType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getReserveStatusType(row.status)">
              {{ getReserveStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="primary"
              size="small"
              @click="handleRecord(row)"
            >录入视力</el-button>
            <el-button
              v-if="row.status === 0"
              type="danger"
              size="small"
              @click="handleCancelReserve(row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="reservePagination.page"
          v-model:page-size="reservePagination.size"
          :total="reservePagination.total"
          layout="total, prev, pager, next"
          @current-change="handleReservePageChange"
        />
      </div>
    </el-card>

    <!-- 批量排班弹窗 -->
    <el-dialog v-model="batchVisible" title="批量排班" width="600px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="batchForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="选择技师">
          <el-select v-model="batchForm.technicianId" placeholder="请选择技师">
            <el-option label="技师A" :value="1" />
            <el-option label="技师B" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="时段设置">
          <div v-for="(slot, index) in batchForm.timeSlots" :key="index" class="time-slot-row">
            <el-time-picker
              v-model="slot.start"
              placeholder="开始时间"
              format="HH:mm"
            />
            <span style="margin: 0 10px;">至</span>
            <el-time-picker
              v-model="slot.end"
              placeholder="结束时间"
              format="HH:mm"
            />
            <el-input-number v-model="slot.capacity" :min="1" :max="10" style="width: 100px; margin-left: 10px;" />
            <el-button type="danger" size="small" @click="removeSlot(index)" style="margin-left: 10px;">删除</el-button>
          </div>
          <el-button type="primary" size="small" @click="addSlot" style="margin-top: 10px;">添加时段</el-button>
        </el-form-item>
        <el-form-item label="重复星期">
          <el-checkbox-group v-model="batchForm.weekDays">
            <el-checkbox :label="1">周一</el-checkbox>
            <el-checkbox :label="2">周二</el-checkbox>
            <el-checkbox :label="3">周三</el-checkbox>
            <el-checkbox :label="4">周四</el-checkbox>
            <el-checkbox :label="5">周五</el-checkbox>
            <el-checkbox :label="6">周六</el-checkbox>
            <el-checkbox :label="7">周日</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="handleBatchSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建预约弹窗 -->
    <el-dialog v-model="reserveDialogVisible" title="新建预约" width="500px">
      <el-form :model="reserveForm" :rules="reserveRules" ref="reserveFormRef" label-width="100px">
        <el-form-item label="儿童姓名" prop="childName">
          <el-input v-model="reserveForm.childName" placeholder="请输入儿童姓名" />
        </el-form-item>
        <el-form-item label="家长电话" prop="parentPhone">
          <el-input v-model="reserveForm.parentPhone" placeholder="请输入家长手机号" />
        </el-form-item>
        <el-form-item label="预约日期" prop="scheduleDate">
          <el-date-picker v-model="reserveForm.scheduleDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="预约时段" prop="timeSlotStart">
          <el-time-picker v-model="reserveForm.timeSlotStart" format="HH:mm" placeholder="开始时间" />
          <span style="margin: 0 8px;">至</span>
          <el-time-picker v-model="reserveForm.timeSlotEnd" format="HH:mm" placeholder="结束时间" />
        </el-form-item>
        <el-form-item label="预约类型" prop="reserveType">
          <el-select v-model="reserveForm.reserveType" placeholder="选择类型">
            <el-option label="初次检测" :value="1" />
            <el-option label="复查" :value="2" />
            <el-option label="养护" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reserveForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reserveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reserveSubmitLoading" @click="handleReserveSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 取消预约弹窗 -->
    <el-dialog v-model="cancelVisible" title="取消预约" width="400px">
      <el-form :model="cancelForm" label-width="80px">
        <el-form-item label="取消原因">
          <el-input v-model="cancelForm.cancelReason" type="textarea" :rows="3" placeholder="请输入取消原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">返回</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="handleCancelSubmit">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreSchedule' })
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Reserve } from '@/types'
import { scheduleApi, reserveApi } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// ==================== 日历 ====================
const currentDate = ref(new Date())
const scheduleData = reactive<Record<string, number>>({})

const getScheduleCount = (day: string) => {
  return scheduleData[day] || 0
}

const handleDateChange = (date: Date) => {
  currentDate.value = date
  fetchScheduleCalendar()
  // 切换日期后重新加载预约
  reserveFilter.status = undefined
  reservePagination.page = 1
  fetchReserves()
}

const fetchScheduleCalendar = async () => {
  if (!userStore.storeId) return
  try {
    const date = currentDate.value
    const startDate = new Date(date.getFullYear(), date.getMonth(), 1)
    const endDate = new Date(date.getFullYear(), date.getMonth() + 1, 0)
    const formatDate = (d: Date): string => {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }

    const res = await scheduleApi.getScheduleCalendar({
      storeId: userStore.storeId,
      startDate: formatDate(startDate),
      endDate: formatDate(endDate)
    })
    // 清空并更新日历数据
    Object.keys(scheduleData).forEach(key => delete scheduleData[key])
    if (res) {
      Object.entries(res).forEach(([date, schedules]) => {
        scheduleData[date] = schedules.length
      })
    }
  } catch {
    // 错误已在拦截器处理
  }
}

// ==================== 预约列表 ====================
const reserveLoading = ref(false)
const reserveList = ref<Reserve[]>([])
const reservePagination = reactive({ page: 1, size: 10, total: 0 })
const reserveFilter = reactive({
  status: undefined as number | undefined
})

const getReserveTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '初次检测', 2: '复查', 3: '养护' }
  return map[type] || '未知'
}

const getReserveStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] || 'info'
}

const getReserveStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待服务', 1: '已完成', 2: '已取消' }
  return map[status] || '未知'
}

const fetchReserves = async () => {
  reserveLoading.value = true
  try {
    const dateStr = formatDate(currentDate.value)
    const res = await reserveApi.getReserveList({
      storeId: userStore.storeId,
      status: reserveFilter.status,
      date: dateStr,
      page: reservePagination.page,
      size: reservePagination.size
    })
    reserveList.value = res.list
    reservePagination.total = res.pagination.total
  } catch {
    // 错误已在拦截器处理
  } finally {
    reserveLoading.value = false
  }
}

const handleReservePageChange = (val: number) => {
  reservePagination.page = val
  fetchReserves()
}

const formatDate = (date: Date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// ==================== 新建预约 ====================
const reserveDialogVisible = ref(false)
const reserveSubmitLoading = ref(false)
const reserveFormRef = ref<FormInstance>()
const reserveForm = reactive({
  childName: '',
  parentPhone: '',
  scheduleDate: '',
  timeSlotStart: null as Date | null,
  timeSlotEnd: null as Date | null,
  reserveType: 1,
  remark: ''
})

const reserveRules = {
  childName: [{ required: true, message: '请输入儿童姓名', trigger: 'blur' }],
  parentPhone: [{ required: true, message: '请输入家长电话', trigger: 'blur' }],
  scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  reserveType: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const handleCreateReserve = () => {
  Object.assign(reserveForm, {
    childName: '',
    parentPhone: '',
    scheduleDate: '',
    timeSlotStart: null,
    timeSlotEnd: null,
    reserveType: 1,
    remark: ''
  })
  reserveDialogVisible.value = true
}

const formatTime = (d: Date | null) => {
  if (!d) return ''
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

const handleReserveSubmit = async () => {
  if (!reserveFormRef.value) return
  await reserveFormRef.value.validate()
  reserveSubmitLoading.value = true
  try {
    await reserveApi.createReserve({
      childName: reserveForm.childName,
      parentPhone: reserveForm.parentPhone,
      scheduleDate: reserveForm.scheduleDate,
      timeSlotStart: formatTime(reserveForm.timeSlotStart),
      timeSlotEnd: formatTime(reserveForm.timeSlotEnd),
      reserveType: reserveForm.reserveType,
      remark: reserveForm.remark || undefined
    })
    ElMessage.success('预约创建成功')
    reserveDialogVisible.value = false
    fetchReserves()
  } catch {
    // 错误已在拦截器处理
  } finally {
    reserveSubmitLoading.value = false
  }
}

// ==================== 取消预约 ====================
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelingReserveId = ref<number | null>(null)
const cancelForm = reactive({ cancelReason: '' })

const handleCancelReserve = (row: Reserve) => {
  cancelingReserveId.value = row.id
  cancelForm.cancelReason = ''
  cancelVisible.value = true
}

const handleCancelSubmit = async () => {
  if (!cancelingReserveId.value) return
  cancelLoading.value = true
  try {
    await reserveApi.cancelReserve(cancelingReserveId.value, cancelForm.cancelReason)
    ElMessage.success('预约已取消')
    cancelVisible.value = false
    fetchReserves()
  } catch {
    // 错误已在拦截器处理
  } finally {
    cancelLoading.value = false
  }
}

// ==================== 录入视力 ====================
const handleRecord = (row: Reserve) => {
  router.push({ path: '/vision', query: { childId: String(row.childId), reserveId: String(row.id) } })
}

// ==================== 批量排班 ====================
const batchVisible = ref(false)
const batchLoading = ref(false)

const batchForm = reactive({
  dateRange: [] as string[],
  technicianId: undefined as number | undefined,
  timeSlots: [{ start: null as Date | null, end: null as Date | null, capacity: 3 }],
  weekDays: [1, 2, 3, 4, 5]
})

const handleBatchCreate = () => {
  batchForm.dateRange = []
  batchForm.technicianId = undefined
  batchForm.timeSlots = [{ start: null, end: null, capacity: 3 }]
  batchForm.weekDays = [1, 2, 3, 4, 5]
  batchVisible.value = true
}

const addSlot = () => {
  batchForm.timeSlots.push({ start: null, end: null, capacity: 3 })
}

const removeSlot = (index: number) => {
  batchForm.timeSlots.splice(index, 1)
}

const handleBatchSubmit = async () => {
  if (!batchForm.dateRange || batchForm.dateRange.length < 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  if (!batchForm.technicianId) {
    ElMessage.warning('请选择技师')
    return
  }
  batchLoading.value = true
  try {
    await scheduleApi.batchCreateSchedule({
      startDate: batchForm.dateRange[0] as string,
      endDate: batchForm.dateRange[1] as string,
      technicianId: batchForm.technicianId,
      timeSlots: batchForm.timeSlots
        .filter(s => s.start && s.end)
        .map(s => ({
          start: formatTime(s.start),
          end: formatTime(s.end),
          capacity: s.capacity
        })),
      weekDays: batchForm.weekDays
    })
    ElMessage.success('批量排班成功')
    batchVisible.value = false
    fetchScheduleCalendar()
  } catch {
    // 错误已在拦截器处理
  } finally {
    batchLoading.value = false
  }
}

// ==================== 初始化 ====================
onMounted(() => {
  fetchScheduleCalendar()
  fetchReserves()
})
</script>

<style scoped lang="scss">
.schedule-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .calendar-cell {
    height: 100%;
    display: flex;
    flex-direction: column;

    .date-number {
      font-size: 14px;
      margin-bottom: 5px;
    }

    .schedule-info {
      margin-top: auto;
    }
  }

  .time-slot-row {
    display: flex;
    align-items: center;
    margin-bottom: 10px;
  }

  .reserve-actions {
    display: flex;
    align-items: center;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
