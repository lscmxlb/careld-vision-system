<template>
  <div class="schedule-rule-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>排班设置</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增排班规则
          </el-button>
        </div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="按自然日区间设置接待时段（工作日/周末均可配置多个不重复时段，也可只配置其中一类），系统按整小时自动生成可约时段；同一日期只能存在于一条规则中。"
        style="margin-bottom: 16px;"
      />

      <el-table :data="ruleList" v-loading="loading" stripe scrollbar-always-on>
        <el-table-column label="生效日期区间" width="200">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column label="周一~五" min-width="220">
          <template #default="{ row }">
            <div v-for="p in periodsOf(row, 1)" :key="p.startTime" class="period-line">
              {{ p.startTime?.slice(0, 5) }}-{{ p.endTime?.slice(0, 5) }}
              <el-tag size="small" type="success" style="margin-left: 6px;">每小时{{ p.capacity }}人</el-tag>
            </div>
            <span v-if="!periodsOf(row, 1).length" style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="周六日" min-width="220">
          <template #default="{ row }">
            <div v-for="p in periodsOf(row, 2)" :key="p.startTime" class="period-line">
              {{ p.startTime?.slice(0, 5) }}-{{ p.endTime?.slice(0, 5) }}
              <el-tag size="small" type="success" style="margin-left: 6px;">每小时{{ p.capacity }}人</el-tag>
            </div>
            <span v-if="!periodsOf(row, 2).length" style="color: #999;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="例外日" min-width="200">
          <template #default="{ row }">
            <template v-if="exceptionGroupsOf(row).length">
              <div v-for="(g, gi) in exceptionGroupsOf(row)" :key="gi" class="period-line">
                <el-tag size="small" type="danger">{{ g.dates.join('、') }}</el-tag>
                <span v-if="g.remark" class="exception-remark">{{ g.remark }}</span>
              </div>
            </template>
            <span v-else style="color: #999;">无</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="720px" :before-close="handleDialogBeforeClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="生效日期区间" prop="dateRange">
          <el-date-picker
            v-model="formData.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-divider content-position="left">周一~五接待时段（可多个，留空表示不排班）</el-divider>
        <div v-for="(p, i) in formData.weekdayPeriods" :key="'wd' + i" class="period-row">
          <el-time-picker v-model="p.start" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width: 110px;" />
          <span class="period-sep">至</span>
          <el-time-picker v-model="p.end" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width: 110px;" />
          <span class="period-sep">每小时上限</span>
          <el-input-number v-model="p.capacity" :min="1" :max="20" style="width: 110px;" />
          <el-button type="danger" circle plain size="small" @click="formData.weekdayPeriods.splice(i, 1)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button type="primary" link @click="formData.weekdayPeriods.push({ start: '', end: '', capacity: 3 })">
          <el-icon><Plus /></el-icon>添加工作日时段
        </el-button>

        <el-divider content-position="left">周六日接待时段（可多个，留空表示不排班）</el-divider>
        <div v-for="(p, i) in formData.weekendPeriods" :key="'we' + i" class="period-row">
          <el-time-picker v-model="p.start" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width: 110px;" />
          <span class="period-sep">至</span>
          <el-time-picker v-model="p.end" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width: 110px;" />
          <span class="period-sep">每小时上限</span>
          <el-input-number v-model="p.capacity" :min="1" :max="20" style="width: 110px;" />
          <el-button type="danger" circle plain size="small" @click="formData.weekendPeriods.splice(i, 1)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button type="primary" link @click="formData.weekendPeriods.push({ start: '', end: '', capacity: 3 })">
          <el-icon><Plus /></el-icon>添加周末时段
        </el-button>

        <el-divider content-position="left">例外日（节假日等不接待，可分批并备注原因）</el-divider>
        <div v-for="(g, i) in formData.exceptionGroups" :key="'ex' + i" class="exception-row">
          <el-date-picker
            v-model="g.dates"
            type="dates"
            placeholder="选择该批的一个或多个日期"
            value-format="YYYY-MM-DD"
            style="width: 320px;"
          />
          <el-input v-model="g.remark" placeholder="例外原因，如：国庆假期" style="width: 200px;" />
          <el-button type="danger" circle plain size="small" @click="formData.exceptionGroups.splice(i, 1)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button type="primary" link @click="formData.exceptionGroups.push({ dates: [], remark: '' })">
          <el-icon><Plus /></el-icon>添加一批例外日期
        </el-button>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogCancel">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreScheduleRule' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { ScheduleRule, ScheduleRulePeriod, ScheduleRuleException } from '@/types'
import { scheduleRuleApi } from '@/api'

interface PeriodInput { start: string; end: string; capacity: number }
interface ExceptionGroupInput { dates: string[]; remark: string }

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitLoading = ref(false)
const ruleList = ref<ScheduleRule[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增排班规则')
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const formData = reactive({
  dateRange: [] as string[],
  weekdayPeriods: [{ start: '08:00', end: '12:00', capacity: 3 }] as PeriodInput[],
  weekendPeriods: [{ start: '08:00', end: '12:00', capacity: 3 }] as PeriodInput[],
  exceptionGroups: [] as ExceptionGroupInput[]
})
/** 打开弹窗时的表单快照：与当前表单不一致视为有未保存数据 */
const formSnapshot = ref('')

const formRules = {
  dateRange: [
    { required: true, message: '请选择日期区间', trigger: 'change' },
    {
      validator: (_rule: unknown, value: string[], callback: (error?: Error) => void) => {
        if (!value || value.length < 2) {
          callback(new Error('请选择日期区间'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

/** 校验时段输入：完整、起止顺序、整小时、同日类型内不重叠；整栏留空视为该类型不排班 */
const validatePeriodInputs = (list: PeriodInput[], label: string): string | null => {
  if (list.some(p => (p.start && !p.end) || (!p.start && p.end))) {
    return `${label}时段的开始/结束时间需同时填写`
  }
  const valid = list.filter(p => p.start && p.end)
  const sorted = [...valid].sort((a, b) => a.start.localeCompare(b.start))
  let prevEnd = ''
  for (const p of sorted) {
    if (p.start >= p.end) return `${label}时段 ${p.start}~${p.end} 结束时间必须晚于开始时间`
    if (!/^\d{2}:\d{2}$/.test(p.start) || !/^\d{2}:\d{2}$/.test(p.end)) return `${label}时段时间格式不正确`
    const [sh = 0, sm = 0] = p.start.split(':').map(Number)
    const [eh = 0, em = 0] = p.end.split(':').map(Number)
    const minutes = (eh * 60 + em) - (sh * 60 + sm)
    if (minutes <= 0 || minutes % 60 !== 0) return `${label}时段 ${p.start}~${p.end} 必须为整小时（如 8:30~11:30）`
    if (!p.capacity || p.capacity < 1) return `${label}时段 ${p.start} 每小时接待人数上限必须大于 0`
    if (prevEnd && p.start < prevEnd) return `${label}时段 ${p.start}~${p.end} 与前面的时段重叠`
    prevEnd = p.end
  }
  return null
}

const periodsOf = (row: ScheduleRule, dayType: number): ScheduleRulePeriod[] =>
  (row.periods || []).filter(p => p.dayType === dayType)

/** 按 remark 将例外日聚合为批次展示 */
const exceptionGroupsOf = (row: ScheduleRule): { dates: string[]; remark: string }[] => {
  const groups: { dates: string[]; remark: string }[] = []
  for (const e of row.exceptions || []) {
    const g = groups.find(x => (x.remark || '') === (e.remark || ''))
    if (g) g.dates.push(e.exceptionDate)
    else groups.push({ dates: [e.exceptionDate], remark: e.remark || '' })
  }
  return groups
}

const fetchData = async () => {
  loading.value = true
  try {
    ruleList.value = await scheduleRuleApi.getRuleList()
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

const toPeriodInputs = (row: ScheduleRule, dayType: number): PeriodInput[] =>
  periodsOf(row, dayType).map(p => ({
    start: (p.startTime || '').slice(0, 5),
    end: (p.endTime || '').slice(0, 5),
    capacity: p.capacity
  }))

/** 将后端例外日按 remark 聚合还原为批次 */
const toExceptionGroups = (row: ScheduleRule): ExceptionGroupInput[] =>
  exceptionGroupsOf(row).map(g => ({ dates: [...g.dates], remark: g.remark }))

const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增排班规则'
  Object.assign(formData, {
    dateRange: [],
    weekdayPeriods: [{ start: '08:00', end: '12:00', capacity: 3 }],
    weekendPeriods: [{ start: '08:00', end: '12:00', capacity: 3 }],
    exceptionGroups: []
  })
  formSnapshot.value = JSON.stringify(formData)
  dialogVisible.value = true
}

const handleEdit = (row: ScheduleRule) => {
  isEdit.value = true
  editingId.value = row.id ?? null
  dialogTitle.value = '编辑排班规则'
  Object.assign(formData, {
    dateRange: [row.startDate, row.endDate],
    weekdayPeriods: toPeriodInputs(row, 1),
    weekendPeriods: toPeriodInputs(row, 2),
    exceptionGroups: toExceptionGroups(row)
  })
  formSnapshot.value = JSON.stringify(formData)
  dialogVisible.value = true
}

/** 有未保存数据时弹确认框；返回 true 表示可以关闭 */
const confirmDialogClose = async (): Promise<boolean> => {
  if (JSON.stringify(formData) === formSnapshot.value) return true
  try {
    await ElMessageBox.confirm('确认关闭吗？已录入的数据将不会保存', '关闭确认', {
      confirmButtonText: '确认关闭',
      cancelButtonText: '继续填写',
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

const handleDialogCancel = async () => {
  if (await confirmDialogClose()) dialogVisible.value = false
}

const handleDialogBeforeClose = async (done: () => void) => {
  if (await confirmDialogClose()) done()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  // 时段校验：工作日/周六日可只配其中一类，但整体至少一条
  const wdError = validatePeriodInputs(formData.weekdayPeriods, '周一~五')
  if (wdError) { ElMessage.warning(wdError); return }
  const weError = validatePeriodInputs(formData.weekendPeriods, '周六日')
  if (weError) { ElMessage.warning(weError); return }
  const wdPeriods = formData.weekdayPeriods.filter(p => p.start && p.end)
  const wePeriods = formData.weekendPeriods.filter(p => p.start && p.end)
  if (!wdPeriods.length && !wePeriods.length) {
    ElMessage.warning('请至少配置一个接待时段（工作日或周六日均可）')
    return
  }
  // 例外日校验：所选日期必须在区间内
  const [rangeStart, rangeEnd] = formData.dateRange
  if (!rangeStart || !rangeEnd) { ElMessage.warning('请选择日期区间'); return }
  for (const g of formData.exceptionGroups) {
    for (const d of g.dates) {
      if (d < rangeStart || d > rangeEnd) {
        ElMessage.warning(`例外日 ${d} 不在排班区间内`)
        return
      }
    }
  }
  submitLoading.value = true
  try {
    // 只提交已填完整的时段，留空整栏不提交
    const periods: ScheduleRulePeriod[] = [
      ...wdPeriods.map(p => ({ dayType: 1, startTime: p.start, endTime: p.end, capacity: p.capacity })),
      ...wePeriods.map(p => ({ dayType: 2, startTime: p.start, endTime: p.end, capacity: p.capacity }))
    ]
    const exceptions: ScheduleRuleException[] = []
    for (const g of formData.exceptionGroups) {
      for (const d of g.dates) {
        exceptions.push({ exceptionDate: d, remark: g.remark || undefined })
      }
    }
    const data: Partial<ScheduleRule> = {
      startDate: rangeStart,
      endDate: rangeEnd,
      periods,
      exceptions
    }
    if (isEdit.value && editingId.value) {
      await scheduleRuleApi.updateRule(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await scheduleRuleApi.createRule(data)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row: ScheduleRule) => {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${row.startDate} ~ ${row.endDate} 的排班规则吗？未预约的时段将一并删除。`,
      '删除确认',
      { type: 'warning' }
    )
    await scheduleRuleApi.deleteRule(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消或错误已在拦截器处理
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.schedule-rule-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .period-line {
    line-height: 24px;
  }
  .period-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    padding-left: 24px;
  }
  .period-sep {
    margin: 0 4px;
    color: #666;
  }
  .exception-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
    padding-left: 24px;
  }
  .exception-remark {
    margin-left: 6px;
    color: #909399;
    font-size: 12px;
  }
}
</style>
