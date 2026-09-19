<template>
  <div class="appointment-record-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button type="primary" @click="handleCreateReserve">
            <el-icon><Plus /></el-icon>新建预约
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="儿童姓名/手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker
            v-model="queryForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="全部"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="queryForm.statuses"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部"
            clearable
            style="width: 180px;"
            popper-class="status-checkbox-popper"
          >
            <el-option v-for="opt in STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value">
              <span class="status-option">
                <el-checkbox class="status-option-box" :model-value="queryForm.statuses.includes(opt.value)" />
                <span>{{ opt.label }}</span>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="reserveList" v-loading="reserveLoading" stripe scrollbar-always-on>
        <el-table-column prop="scheduleDate" label="日期" width="120" />
        <el-table-column label="时段" width="120">
          <template #default="{ row }">{{ formatHm(row.timeSlotStart) }}-{{ formatHm(row.timeSlotEnd) }}</template>
        </el-table-column>
        <el-table-column prop="childName" label="儿童姓名" width="100" />
        <el-table-column prop="parentPhone" label="家长电话" width="130" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.noShowFlag === 1" type="danger">已爽约</el-tag>
            <el-tag v-else-if="row.status === 1 && row.adjustFlag === 1" type="info">已调整</el-tag>
            <el-tag v-else :type="getReserveStatusType(row.status)">
              {{ getReserveStatusText(row.status) }}
            </el-tag>
            <el-tooltip
              v-if="row.status === 4 && row.cancelReason"
              :content="row.noShowFlag === 1 ? row.cancelReason : `取消原因：${row.cancelReason}`"
              placement="top"
            >
              <el-icon class="cancel-reason-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="row.source === 1 ? 'success' : 'info'" size="small">
              {{ row.source === 1 ? '家长预约' : '医生预约' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="预约人" width="100">
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="executorName" label="养护人" width="100">
          <template #default="{ row }">{{ row.executorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="cancelOperatorName" label="取消人" width="100">
          <template #default="{ row }">{{ row.cancelOperatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="adjustOperatorName" label="调整人" width="100">
          <template #default="{ row }">{{ row.adjustOperatorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="282" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 2 ? 'success' : 'primary'"
              plain
              size="small"
              :disabled="!canOperateCare(row)"
              @click="row.status === 2 ? handleCompleteCare(row) : handleStartCare(row)"
            >{{ row.status === 2 ? '完成养护' : '开始养护' }}</el-button>
            <el-tooltip
              v-if="row.status === 1 && isOverdue(row)"
              content="已超过预约时段，不可调整"
              placement="top"
            >
              <span>
                <el-button plain size="small" disabled>预约调整</el-button>
              </span>
            </el-tooltip>
            <el-button
              v-else
              plain
              size="small"
              :disabled="row.status !== 1"
              @click="handleAdjust(row)"
            >预约调整</el-button>
            <el-button
              type="warning"
              plain
              size="small"
              :disabled="row.status !== 1 || !(isReserveToday(row) || isOverdue(row))"
              @click="handleNoShow(row)"
            >标记爽约</el-button>
            <el-button
              type="danger"
              plain
              size="small"
              :disabled="row.status !== 1 || isOverdue(row)"
              @click="handleCancelReserve(row)"
            >取消预约</el-button>
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

    <!-- 新建预约弹窗（新链路：选儿童 + 可约时段） -->
    <el-dialog
      v-model="reserveDialogVisible"
      title="新建预约"
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

    <!-- 养护记录登记弹窗（开始/结束养护合并） -->
    <el-dialog
      v-model="careVisible"
      title="养护记录登记"
      width="560px"
      top="3vh"
      class="care-dialog"
      :close-on-click-modal="false"
    >
      <el-form label-width="110px">
        <el-form-item label="养护服务">
          <el-select v-model="careForm.executorId" placeholder="请选择进行养护服务的医师或医生助理" style="width: 100%;" :disabled="careMode !== 'start'">
            <el-option v-for="doc in doctorOptions" :key="doc.id" :label="doc.name" :value="doc.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker
            v-model="careForm.startTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择开始时间"
            style="width: 100%;"
            :disabled="careMode !== 'start'"
          />
        </el-form-item>
      </el-form>
      <el-divider />
      <div class="vision-section-title">养护前视力检查记录</div>
      <el-form label-width="110px">
        <el-form-item label="双眼">
          <div class="vision-pair">
            <el-select v-model="careForm.beforeBothMain" placeholder="5.3-4.0" :disabled="careMode !== 'start'">
              <el-option v-for="v in VISION_MAIN_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <el-select v-model="careForm.beforeBothSub" placeholder="+0~+5 / -1~-5" :disabled="careMode !== 'start'">
              <el-option v-for="v in VISION_SUB_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="左眼">
          <div class="vision-pair">
            <el-select v-model="careForm.beforeLeftMain" placeholder="5.3-4.0" :disabled="careMode !== 'start'">
              <el-option v-for="v in VISION_MAIN_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <el-select v-model="careForm.beforeLeftSub" placeholder="+0~+5 / -1~-5" :disabled="careMode !== 'start'">
              <el-option v-for="v in VISION_SUB_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="右眼">
          <div class="vision-pair">
            <el-select v-model="careForm.beforeRightMain" placeholder="5.3-4.0" :disabled="careMode !== 'start'">
              <el-option v-for="v in VISION_MAIN_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <el-select v-model="careForm.beforeRightSub" placeholder="+0~+5 / -1~-5" :disabled="careMode !== 'start'">
              <el-option v-for="v in VISION_SUB_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
          </div>
        </el-form-item>
      </el-form>
      <el-divider />
      <div class="vision-section-title">养护后视力检查记录</div>
      <el-form label-width="110px">
        <el-form-item label="双眼">
          <div class="vision-pair">
            <el-select v-model="careForm.afterBothMain" placeholder="5.3-4.0">
              <el-option v-for="v in VISION_MAIN_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <el-select v-model="careForm.afterBothSub" placeholder="+0~+5 / -1~-5">
              <el-option v-for="v in VISION_SUB_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="左眼">
          <div class="vision-pair">
            <el-select v-model="careForm.afterLeftMain" placeholder="5.3-4.0">
              <el-option v-for="v in VISION_MAIN_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <el-select v-model="careForm.afterLeftSub" placeholder="+0~+5 / -1~-5">
              <el-option v-for="v in VISION_SUB_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="右眼">
          <div class="vision-pair">
            <el-select v-model="careForm.afterRightMain" placeholder="5.3-4.0">
              <el-option v-for="v in VISION_MAIN_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <el-select v-model="careForm.afterRightSub" placeholder="+0~+5 / -1~-5">
              <el-option v-for="v in VISION_SUB_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
          </div>
        </el-form-item>
      </el-form>
      <el-divider />
      <template #footer>
        <el-button @click="handleCareCancel">取消</el-button>
        <el-button type="primary" :loading="careStartLoading" :disabled="careMode !== 'start' || !careForm.executorId || !careForm.startTime" @click="handleCareStart">开始养护</el-button>
        <el-button type="success" :loading="careCompleteLoading" :disabled="!careForm.executorId || !careForm.startTime" @click="handleCareComplete">结束养护</el-button>
      </template>
    </el-dialog>

    <!-- 标记爽约弹窗 -->
    <el-dialog v-model="noShowVisible" title="标记爽约" width="420px">
      <div class="no-show-tip">标记爽约不退还预约次数（与取消不同），确认标记该预约为爽约吗？</div>
      <template #footer>
        <el-button @click="noShowVisible = false">取消</el-button>
        <el-button type="warning" :loading="noShowLoading" @click="handleNoShowSubmit">确定标记</el-button>
      </template>
    </el-dialog>

    <!-- 预约调整弹窗（更换日期/时段） -->
    <el-dialog v-model="adjustVisible" title="预约调整" width="520px">
      <el-form :model="adjustForm" :rules="adjustRules" ref="adjustFormRef" label-width="100px">
        <el-form-item label="当前预约">
          <div v-if="adjustingReserve">
            {{ adjustingReserve.childName || '-' }}｜{{ adjustingReserve.scheduleDate }}
            {{ adjustingReserve.timeSlotStart }}-{{ adjustingReserve.timeSlotEnd }}
          </div>
        </el-form-item>
        <el-form-item label="新日期" prop="date">
          <el-date-picker
            v-model="adjustForm.date"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择新日期"
            :disabled-date="addDisabledDate"
            @change="loadAdjustSlots"
          />
        </el-form-item>
        <el-form-item label="可约时段" prop="slotId">
          <el-select v-model="adjustForm.slotId" placeholder="请先选择日期" style="width: 100%;" :loading="adjustSlotLoading">
            <el-option
              v-for="slot in adjustSlotOptions"
              :key="slot.id"
              :label="`${slot.slotStartTime}-${slot.slotEndTime}（剩余${slot.maxCapacity - slot.bookedCount}人）`"
              :value="slot.id"
              :disabled="slot.maxCapacity - slot.bookedCount <= 0"
            />
          </el-select>
          <div v-if="adjustForm.date && adjustSlotOptions.length === 0 && !adjustSlotLoading" class="slot-empty-tip">
            该日期无可约时段，请先在「排班设置」中配置排班规则
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="handleAdjustSubmit">确定调整</el-button>
      </template>
    </el-dialog>

    <!-- 取消预约弹窗 -->
    <el-dialog v-model="cancelVisible" title="取消预约" width="400px">
      <el-form :model="cancelForm" :rules="cancelRules" ref="cancelFormRef" label-width="80px">
        <el-form-item label="取消原因" prop="cancelReason">
          <el-input v-model="cancelForm.cancelReason" type="textarea" :rows="3" placeholder="请输入取消原因（必填）" />
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
defineOptions({ name: 'StoreAppointmentRecord' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, QuestionFilled } from '@element-plus/icons-vue'
import type { Reserve, ScheduleSlot, Child, MedicalStaff } from '@/types'
import { scheduleRuleApi, reserveApi, childApi, medicalStaffApi, appointmentConfigApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ==================== 预约列表 ====================
const reserveLoading = ref(false)
const reserveList = ref<Reserve[]>([])
const reservePagination = reactive({ page: 1, size: 10, total: 0 })
const queryForm = reactive({
  keyword: '',
  date: '',
  statuses: [] as number[]
})

/** 状态筛选项（多选，下拉选项前置方框勾选） */
const STATUS_OPTIONS = [
  { value: 1, label: '已预约' },
  { value: 2, label: '养护中' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已爽约' }
]

/** 默认显示状态（系统设置-预约规则），进入页与重置时作为默认勾选 */
const DEFAULT_SHOW_STATUSES = [1, 2, 3, 4, 5]
const defaultShowStatuses = ref<number[]>([...DEFAULT_SHOW_STATUSES])

const loadDefaultShowStatuses = async () => {
  try {
    const config = await appointmentConfigApi.getConfig()
    const parsed = (config.defaultShowStatuses || '1,2,3,4,5')
      .split(',')
      .map((v) => Number(v))
      .filter((v) => v >= 1 && v <= 5)
    defaultShowStatuses.value = parsed.length ? parsed : [...DEFAULT_SHOW_STATUSES]
  } catch {
    // 拉取失败按全部状态处理
  }
  queryForm.statuses = [...defaultShowStatuses.value]
}

const getReserveStatusType = (status: number) => {
  const map: Record<number, string> = { 1: 'warning', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}

const getReserveStatusText = (status: number) => {
  const map: Record<number, string> = { 1: '已预约', 2: '养护中', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

/** 时段显示统一为 HH:mm（后端返回 HH:mm:ss 时去掉秒） */
const formatHm = (t?: string) => (t ? t.slice(0, 5) : '')

const formatDate = (d: Date): string => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 今天（YYYY-MM-DD）：开始养护仅预约当天可操作 */
const todayStr = () => formatDate(new Date())
const isReserveToday = (row: Reserve) => row.scheduleDate === todayStr()

/** 养护按钮（同一按钮二态）：已预约当天可开始养护；养护中可完成养护 */
const canOperateCare = (row: Reserve) => row.status === 2 || (row.status === 1 && isReserveToday(row))

/** 已逾期：当前时间超过预约时段结束时间（逾期后不可调整/取消，只能标记爽约） */
const isOverdue = (row: Reserve) => {
  if (!row.scheduleDate || !row.timeSlotEnd) return false
  return new Date(`${row.scheduleDate}T${row.timeSlotEnd}`) < new Date()
}

const fetchReserves = async () => {
  reserveLoading.value = true
  try {
    const statuses = queryForm.statuses
    // 仅勾选已取消/已爽约时需覆盖历史（不加默认起始日），见下方 startDate 规则
    const onlyCancelOrNoShow = statuses.length > 0 && statuses.every((s) => s === 4 || s === 5)
    const res = await reserveApi.getReserveList({
      storeId: userStore.storeId,
      keyword: queryForm.keyword.trim() || undefined,
      statuses: statuses.length ? statuses.join(',') : undefined,
      date: queryForm.date || undefined,
      // 未指定日期时默认只看今天及以后的记录；只看已取消/已爽约需覆盖历史，不加默认起始日
      startDate: queryForm.date || onlyCancelOrNoShow ? undefined : todayStr(),
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

const handleSearch = () => {
  reservePagination.page = 1
  fetchReserves()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.date = ''
  queryForm.statuses = [...defaultShowStatuses.value]
  reservePagination.page = 1
  fetchReserves()
}

const handleReservePageChange = (val: number) => {
  reservePagination.page = val
  fetchReserves()
}

// ==================== 新建预约（新链路：选儿童 + 可约时段） ====================
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

const reserveRules = {
  childId: [{ required: true, message: '请选择儿童', trigger: 'change' }],
  date: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  slotId: [{ required: true, message: '请选择可约时段', trigger: 'change' }]
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

const handleCreateReserve = () => {
  Object.assign(reserveForm, {
    childId: undefined,
    date: '',
    slotId: undefined,
    remark: ''
  })
  reserveFormSnapshot.value = JSON.stringify(reserveForm)
  childOptions.value = []
  slotOptions.value = []
  loadAvailability()
  reserveDialogVisible.value = true
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
    fetchReserves()
  } catch {
    // 错误已在拦截器处理
  } finally {
    reserveSubmitLoading.value = false
  }
}

// ==================== 养护记录登记（开始/结束养护合并弹窗） ====================
const careVisible = ref(false)
const careMode = ref<'start' | 'complete'>('start')
const careStartLoading = ref(false)
const careCompleteLoading = ref(false)
const caringReserve = ref<Reserve | null>(null)
const doctorOptions = ref<MedicalStaff[]>([])
const VISION_MAIN_OPTIONS = ['5.3', '5.2', '5.1', '5.0', '4.9', '4.8', '4.7', '4.6', '4.5', '4.4', '4.3', '4.2', '4.1', '4.0']
const VISION_SUB_OPTIONS = ['+0', '+1', '+2', '+3', '+4', '+5', '-1', '-2', '-3', '-4', '-5']

const emptyCareForm = () => ({
  executorId: undefined as number | undefined,
  startTime: '',
  beforeBothMain: '', beforeBothSub: '+0',
  beforeLeftMain: '', beforeLeftSub: '+0',
  beforeRightMain: '', beforeRightSub: '+0',
  afterBothMain: '', afterBothSub: '+0',
  afterLeftMain: '', afterLeftSub: '+0',
  afterRightMain: '', afterRightSub: '+0'
})
const careForm = reactive(emptyCareForm())

const resetCareForm = () => Object.assign(careForm, emptyCareForm())

const loadDoctors = async () => {
  if (doctorOptions.value.length) return
  try {
    const res = await medicalStaffApi.getStaffList({ status: 1, page: 1, size: 100 })
    doctorOptions.value = res.list
  } catch {
    // 错误已在拦截器处理
  }
}

const combineVision = (main: string, sub: string) => (main ? main + (sub || '') : undefined)

const parseVision = (v?: string) => {
  const m = /^(5\.[0-3]|4\.[0-9])([+-]\d+)?$/.exec(v || '')
  return m ? { main: m[1], sub: m[2] || '+0' } : { main: '', sub: '+0' }
}

/** 是否有任何录入（取消时弹确认框）；次值有默认 +0，不计入 */
const careFormDirty = () =>
  !!careForm.executorId || !!careForm.startTime ||
  !!careForm.beforeBothMain || !!careForm.beforeLeftMain || !!careForm.beforeRightMain ||
  !!careForm.afterBothMain || !!careForm.afterLeftMain || !!careForm.afterRightMain

/** 数据是否填写完整（结束养护时校验） */
const careFormComplete = () =>
  !!careForm.executorId && !!careForm.startTime &&
  !!careForm.beforeBothMain && !!careForm.beforeBothSub &&
  !!careForm.beforeLeftMain && !!careForm.beforeLeftSub && !!careForm.beforeRightMain && !!careForm.beforeRightSub &&
  !!careForm.afterBothMain && !!careForm.afterBothSub &&
  !!careForm.afterLeftMain && !!careForm.afterLeftSub && !!careForm.afterRightMain && !!careForm.afterRightSub

const handleStartCare = (row: Reserve) => {
  caringReserve.value = row
  careMode.value = 'start'
  resetCareForm()
  careVisible.value = true
  loadDoctors()
}

const handleCompleteCare = async (row: Reserve) => {
  caringReserve.value = row
  resetCareForm()
  careMode.value = 'complete'
  careVisible.value = true
  loadDoctors()
  try {
    const rec = await reserveApi.getCareRecord(row.id)
    if (rec) {
      const bb = parseVision(rec.visionBeforeBoth)
      const bl = parseVision(rec.visionBeforeLeft)
      const br = parseVision(rec.visionBeforeRight)
      const ab = parseVision(rec.visionAfterBoth)
      const al = parseVision(rec.visionAfterLeft)
      const ar = parseVision(rec.visionAfterRight)
      Object.assign(careForm, {
        executorId: rec.executorId,
        startTime: row.startTime ? row.startTime.slice(11, 16) : '',
        beforeBothMain: bb.main, beforeBothSub: bb.sub,
        beforeLeftMain: bl.main, beforeLeftSub: bl.sub,
        beforeRightMain: br.main, beforeRightSub: br.sub,
        afterBothMain: ab.main, afterBothSub: ab.sub,
        afterLeftMain: al.main, afterLeftSub: al.sub,
        afterRightMain: ar.main, afterRightSub: ar.sub
      })
    }
  } catch {
    // 错误已在拦截器处理
  }
}

/** 取消：无数据直接退出，有数据弹确认框 */
const handleCareCancel = async () => {
  if (careFormDirty()) {
    try {
      await ElMessageBox.confirm('确认取消吗？已填写的数据将不会保存', '取消确认', {
        confirmButtonText: '确认取消',
        cancelButtonText: '继续填写',
        type: 'warning'
      })
    } catch {
      return
    }
  }
  careVisible.value = false
}

/** 提交开始养护（未录入养护前视力时需确认） */
const doStartCare = async (): Promise<boolean> => {
  if (!caringReserve.value) return false
  if (!careForm.beforeLeftMain || !careForm.beforeRightMain) {
    try {
      await ElMessageBox.confirm('数据未填写完整，是否确认开始养护？', '开始确认', {
        confirmButtonText: '确认开始',
        cancelButtonText: '继续填写',
        type: 'warning'
      })
    } catch {
      return false
    }
  }
  const doctor = doctorOptions.value.find(d => d.id === careForm.executorId)
  careStartLoading.value = true
  try {
    await reserveApi.startCare(caringReserve.value.id, {
      executorId: careForm.executorId,
      executorName: doctor?.name,
      startTime: careForm.startTime || undefined,
      visionBeforeBoth: combineVision(careForm.beforeBothMain, careForm.beforeBothSub),
      visionBeforeLeft: combineVision(careForm.beforeLeftMain, careForm.beforeLeftSub),
      visionBeforeRight: combineVision(careForm.beforeRightMain, careForm.beforeRightSub)
    })
    return true
  } catch {
    return false
  } finally {
    careStartLoading.value = false
  }
}

const handleCareStart = async () => {
  const ok = await doStartCare()
  if (!ok) return
  ElMessage.success('已开始养护')
  // 开始养护成功即关闭弹窗，后续完成养护从列表行进入（记录自动回填）
  careVisible.value = false
  fetchReserves()
}

const handleCareComplete = async () => {
  if (!caringReserve.value) return
  // 尚未开始养护时先开始（未录入养护前视力会先弹确认框）
  if (careMode.value === 'start') {
    const ok = await doStartCare()
    if (!ok) return
  }
  if (!careFormComplete()) {
    try {
      await ElMessageBox.confirm('数据没有填写完整，是否确认结束养护？', '结束确认', {
        confirmButtonText: '确认结束',
        cancelButtonText: '继续填写',
        type: 'warning'
      })
    } catch {
      return
    }
  }
  careCompleteLoading.value = true
  try {
    await reserveApi.completeCare(caringReserve.value.id, {
      visionAfterBoth: combineVision(careForm.afterBothMain, careForm.afterBothSub),
      visionAfterLeft: combineVision(careForm.afterLeftMain, careForm.afterLeftSub),
      visionAfterRight: combineVision(careForm.afterRightMain, careForm.afterRightSub)
    })
    ElMessage.success('养护已完成')
    careVisible.value = false
    fetchReserves()
  } catch {
    // 错误已在拦截器处理
  } finally {
    careCompleteLoading.value = false
  }
}

const noShowVisible = ref(false)
const noShowLoading = ref(false)
const noShowingReserve = ref<Reserve | null>(null)

const handleNoShow = (row: Reserve) => {
  noShowingReserve.value = row
  noShowVisible.value = true
}

const handleNoShowSubmit = async () => {
  if (!noShowingReserve.value) return
  noShowLoading.value = true
  try {
    await reserveApi.markNoShow(noShowingReserve.value.id)
    ElMessage.success('已标记爽约（不退还次数）')
    noShowVisible.value = false
    fetchReserves()
  } catch {
    // 错误已在拦截器处理
  } finally {
    noShowLoading.value = false
  }
}

// ==================== 预约调整 ====================
const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustingReserve = ref<Reserve | null>(null)
const adjustFormRef = ref<FormInstance>()
const adjustForm = reactive({
  date: '',
  slotId: undefined as string | undefined
})
const adjustSlotOptions = ref<ScheduleSlot[]>([])
const adjustSlotLoading = ref(false)

const adjustRules = {
  date: [{ required: true, message: '请选择新日期', trigger: 'change' }],
  slotId: [{ required: true, message: '请选择可约时段', trigger: 'change' }]
}

/** 近30天有可约时段的日期集合（打开新建/调整弹窗时拉取，禁选其余日期） */
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

const loadAdjustSlots = async () => {
  adjustForm.slotId = undefined
  adjustSlotOptions.value = []
  if (!adjustForm.date) return
  adjustSlotLoading.value = true
  try {
    adjustSlotOptions.value = await scheduleRuleApi.getSlots(adjustForm.date, userStore.storeId || undefined)
    if (adjustSlotOptions.value.length === 0) {
      ElMessage.warning('该日期无可约时段，请重新选择')
      adjustForm.date = ''
    }
  } catch {
    // 错误已在拦截器处理
  } finally {
    adjustSlotLoading.value = false
  }
}

const handleAdjust = (row: Reserve) => {
  adjustingReserve.value = row
  adjustForm.date = ''
  adjustForm.slotId = undefined
  adjustSlotOptions.value = []
  loadAvailability()
  adjustVisible.value = true
}

const handleAdjustSubmit = async () => {
  if (!adjustFormRef.value || !adjustingReserve.value) return
  await adjustFormRef.value.validate()
  adjustLoading.value = true
  try {
    await reserveApi.adjustReserve(adjustingReserve.value.id, adjustForm.slotId!)
    ElMessage.success('预约已调整')
    adjustVisible.value = false
    fetchReserves()
  } catch {
    // 错误已在拦截器处理
  } finally {
    adjustLoading.value = false
  }
}

// ==================== 取消预约 ====================
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelingReserveId = ref<number | null>(null)
const cancelFormRef = ref<FormInstance>()
const cancelForm = reactive({ cancelReason: '' })

const cancelRules = {
  cancelReason: [{ required: true, message: '请填写取消原因', trigger: 'blur' }]
}

const handleCancelReserve = (row: Reserve) => {
  cancelingReserveId.value = row.id
  cancelForm.cancelReason = ''
  cancelVisible.value = true
}

const handleCancelSubmit = async () => {
  if (!cancelingReserveId.value) return
  if (!cancelFormRef.value) return
  await cancelFormRef.value.validate()
  cancelLoading.value = true
  try {
    await reserveApi.cancelReserve(cancelingReserveId.value, cancelForm.cancelReason.trim())
    ElMessage.success('预约已取消')
    cancelVisible.value = false
    fetchReserves()
  } catch {
    // 错误已在拦截器处理
  } finally {
    cancelLoading.value = false
  }
}

// ==================== 初始化 ====================
onMounted(async () => {
  await loadDefaultShowStatuses()
  fetchReserves()
})
</script>

<style scoped lang="scss">
.appointment-record-page {
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
  }

  .slot-empty-tip {
    color: #e6a23c;
    font-size: 12px;
    margin-top: 4px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  :deep(.el-table .cell .el-button--small) {
    padding: 0 4px;
  }

  :deep(.el-table .cell .el-button + .el-button) {
    margin-left: 8px;
  }

  .cancel-reason-icon {
    margin-left: 4px;
    color: #909399;
    cursor: pointer;
    vertical-align: middle;
  }

  .no-show-tip {
    color: #e6a23c;
    font-size: 13px;
    line-height: 1.6;
  }
}

.vision-section-title {
  font-weight: 600;
  margin: 0 0 12px;
}

.vision-pair {
  display: flex;
  gap: 10px;
  width: 100%;

  .el-select {
    flex: 1;
  }
}
</style>

<style lang="scss">
/* 状态多选下拉（popper 挂载在 body，需非 scoped 样式）：选项前置方框勾选 */
.status-checkbox-popper {
  .status-option {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .status-option-box {
    pointer-events: none;
    margin-right: 0;
    height: auto;
  }

  /* 选中标记由前置勾选框表达，隐藏右侧默认对勾 */
  .el-select-dropdown__item.is-selected::after {
    display: none;
  }
}

/* 养护记录登记弹窗：内容超高时弹窗内滚动，保证整框在视口内完整显示 */
.care-dialog {
  .el-dialog__body {
    max-height: calc(97vh - 130px);
    overflow-y: auto;
  }
}
</style>
