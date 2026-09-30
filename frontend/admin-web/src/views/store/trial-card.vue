<template>
  <div class="trial-card-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title-block">
            <span class="card-title">项目体验卡</span>
            <span class="header-hint">
              发行后家长在医院端「儿童档案 → 兑换次数」输入编号与验证码，可为该档案增加 1 次可用预约次数
            </span>
          </div>
          <div class="header-actions">
            <el-button v-permission="'trialcard:create'" @click="openManage">
              <el-icon><Setting /></el-icon>体验卡管理
            </el-button>
            <el-button type="primary" v-permission="'trialcard:create'" @click="openIssue">
              <el-icon><Plus /></el-icon>发行体验卡
            </el-button>
            <el-button v-permission="'trialcard:export'" :loading="exporting" @click="handleExport">
              <el-icon><Download /></el-icon>导出 Excel
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="query-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="未兑换" :value="0" />
            <el-option label="已绑定" :value="2" />
            <el-option label="已使用" :value="1" />
            <el-option label="已禁用" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="运营中心">
          <el-select
            v-model="query.centerId"
            placeholder="全部"
            clearable
            filterable
            style="width: 170px"
            @change="handleCenterChange"
          >
            <el-option v-for="item in centers" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理商">
          <el-select
            v-model="query.agentId"
            placeholder="全部"
            clearable
            filterable
            :disabled="!query.centerId"
            style="width: 170px"
            @change="handleAgentChange"
          >
            <el-option v-for="item in agentOptions" :key="item.id" :label="item.agentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用医院">
          <el-select
            v-model="query.storeId"
            placeholder="全部"
            clearable
            filterable
            :disabled="!query.centerId"
            style="width: 180px"
          >
            <el-option v-for="item in storeOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="体验卡编号">
          <el-input
            v-model="query.keyword"
            placeholder="输入编号搜索"
            clearable
            style="width: 150px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="发行日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
            unlink-panels
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="cards" v-loading="loading" stripe scrollbar-always-on>
        <el-table-column prop="cardNo" label="体验卡编号" width="130" />
        <el-table-column prop="verifyCode" label="验证码" width="110" />
        <el-table-column label="标题" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || '—' }}</template>
        </el-table-column>
        <el-table-column label="适用范围" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">{{ scopeText(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="cardStatusTagType(row.status)">{{ cardStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定时间" width="150">
          <template #default="{ row }">{{ dateTimeText(row.boundAt) }}</template>
        </el-table-column>
        <el-table-column label="使用时间" width="150">
          <template #default="{ row }">{{ dateTimeText(row.usedAt) }}</template>
        </el-table-column>
        <el-table-column label="使用医院" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.usedStoreName || '—' }}</template>
        </el-table-column>
        <el-table-column label="使用儿童" width="100">
          <template #default="{ row }">{{ row.childName || '—' }}</template>
        </el-table-column>
        <el-table-column label="家长姓名" width="100">
          <template #default="{ row }">{{ row.parentName || '—' }}</template>
        </el-table-column>
        <el-table-column label="联系方式" width="130">
          <template #default="{ row }">{{ row.parentPhone || '—' }}</template>
        </el-table-column>
        <el-table-column label="发行人" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.creatorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="发行时间" width="150">
          <template #default="{ row }">{{ dateTimeText(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 3"
              type="success"
              size="small"
              v-permission="'trialcard:create'"
              :loading="rowActionId === row.id"
              @click="handleEnable(row)"
            >启用</el-button>
            <el-button
              v-else
              type="danger"
              size="small"
              v-permission="'trialcard:create'"
              :disabled="row.status !== 0"
              :loading="rowActionId === row.id"
              @click="handleDisable(row)"
            >禁用</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无体验卡" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination-wrapper" v-if="pagination.total > pagination.size">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchCards"
        />
      </div>
    </el-card>

    <!-- 体验卡管理：按编号区间批量禁用 -->
    <el-dialog v-model="manageVisible" title="体验卡管理" width="560px" destroy-on-close>
      <div class="manage-tip">
        按编号区间批量禁用体验卡：仅「未兑换」状态的卡会被禁用，已绑定 / 已使用 / 已禁用的卡将自动跳过。
      </div>
      <el-form ref="manageFormRef" :model="manageForm" :rules="manageRules" label-width="110px">
        <el-form-item label="起始编号" prop="startCardNo">
          <el-input v-model="manageForm.startCardNo" placeholder="输入 11 位体验卡编号" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="结束编号" prop="endCardNo">
          <el-input v-model="manageForm.endCardNo" placeholder="输入 11 位体验卡编号（含该编号）" clearable maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manageVisible = false">取消</el-button>
        <el-button type="danger" :loading="disablingRange" @click="handleDisableRange">批量禁用</el-button>
      </template>
    </el-dialog>

    <!-- 发行体验卡 -->
    <el-dialog v-model="issueVisible" title="发行项目体验卡" width="560px" destroy-on-close>
      <el-form ref="issueFormRef" :model="issueForm" :rules="issueRules" label-width="110px">
        <el-form-item label="运营中心" prop="centerId">
          <el-select
            v-model="issueForm.centerId"
            placeholder="选择适用运营中心（必填）"
            filterable
            style="width: 100%"
            @change="handleIssueCenterChange"
          >
            <el-option v-for="item in centers" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理商">
          <el-select
            v-model="issueForm.agentId"
            placeholder="不选则适用该运营中心下全部代理商"
            clearable
            filterable
            :disabled="!issueForm.centerId"
            style="width: 100%"
            @change="handleIssueAgentChange"
          >
            <el-option v-for="item in issueAgentOptions" :key="item.id" :label="item.agentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="具体医院">
          <el-select
            v-model="issueForm.storeId"
            placeholder="不选则适用该运营中心/代理商下全部医院"
            clearable
            filterable
            :disabled="!issueForm.centerId"
            style="width: 100%"
          >
            <el-option v-for="item in issueStoreOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市区号" prop="areaCode">
          <el-input v-model="issueForm.areaCode" placeholder="如 0755；3 位区号（755）自动补前导 0" clearable style="width: 100%" />
          <div class="field-tip">
            编号规则：2 位年份（{{ yearPrefix }}）+ 4 位区号（{{ areaCodePreview || '----' }}）+ 5 位顺序编号
          </div>
        </el-form-item>
        <el-form-item label="发行标题" prop="title">
          <el-input
            v-model="issueForm.title"
            placeholder="如：2026 国庆推广批次"
            clearable
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="发行数量" prop="count">
          <el-input-number
            v-model="issueForm.count"
            :min="1"
            :max="10000"
            :step="1"
            step-strictly
            controls-position="right"
            style="width: 160px"
          />
          <span class="field-inline-tip">张（1~10000，同批编号连续）</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="issueForm.remark" placeholder="选填，如发行批次、用途" clearable maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="issuing" @click="handleIssueSubmit">确认发行</el-button>
      </template>
    </el-dialog>

    <!-- 批量发行结果 -->
    <el-dialog v-model="resultVisible" title="发行成功" width="560px">
      <div class="result-summary">
        标题：<b>{{ resultTitle }}</b>，共发行 <b class="result-count">{{ issuedCards.length }}</b> 张体验卡（编号连续）
      </div>
      <div class="result-tip">请将编号与验证码提供给家长，用于手机端「儿童档案 → 兑换次数」兑换次数</div>
      <div class="result-tip" v-if="issuedCards.length > RESULT_PREVIEW_LIMIT">
        本批共 {{ issuedCards.length }} 张，此处仅展示前 {{ RESULT_PREVIEW_LIMIT }} 张；完整编号请在列表中查看或用「导出 Excel」导出
      </div>
      <el-table :data="resultCards" max-height="360" stripe>
        <el-table-column type="index" label="#" width="52" />
        <el-table-column prop="cardNo" label="体验卡编号" width="150" />
        <el-table-column prop="verifyCode" label="验证码" width="130" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminTrialCard' })
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Setting } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { orgApi, storeApi, trialCardApi } from '@/api'
import type { TrialCard, TrialCardQuery } from '@/api'
import { exportXlsx } from '@/utils/xlsx'
import type { Agent, OpsCenter, Store } from '@/types'

const loading = ref(false)
const cards = ref<TrialCard[]>([])
const centers = ref<OpsCenter[]>([])
const allAgents = ref<Agent[]>([])
const allStores = ref<Store[]>([])
const dateRange = ref<[string, string] | null>(null)
const pagination = reactive({ page: 1, size: 20, total: 0 })

const query = reactive<TrialCardQuery>({
  status: undefined,
  centerId: undefined,
  agentId: undefined,
  storeId: undefined,
  keyword: ''
})

/** 代理商 / 医院下拉：按已选中心（及代理商）联动过滤 */
const agentOptions = computed(() =>
  query.centerId ? allAgents.value.filter((item) => item.centerId === query.centerId) : allAgents.value,
)
const storeOptions = computed(() =>
  allStores.value.filter((store) => {
    const centerId = Number(store.centerId)
    if (query.centerId && centerId !== query.centerId) return false
    if (query.agentId && store.agentId !== query.agentId) return false
    return true
  }),
)

const fetchCards = async () => {
  loading.value = true
  try {
    const data = await trialCardApi.getList({
      status: query.status,
      centerId: query.centerId,
      agentId: query.agentId,
      storeId: query.storeId,
      keyword: query.keyword || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      page: pagination.page,
      size: pagination.size
    })
    cards.value = data.list ?? []
    pagination.total = Number(data.pagination?.total ?? 0)
  } finally {
    loading.value = false
  }
}

const handleCenterChange = () => {
  query.agentId = undefined
  query.storeId = undefined
  handleSearch()
}

const handleAgentChange = () => {
  query.storeId = undefined
  handleSearch()
}

const handleSearch = () => {
  pagination.page = 1
  fetchCards()
}

const handleReset = () => {
  query.status = undefined
  query.centerId = undefined
  query.agentId = undefined
  query.storeId = undefined
  query.keyword = ''
  dateRange.value = null
  handleSearch()
}

const scopeText = (row: TrialCard) => {
  const parts: string[] = []
  if (row.centerName) parts.push(row.centerName)
  if (row.agentName) parts.push(row.agentName)
  if (row.storeName) parts.push(row.storeName)
  if (!parts.length) return '—'
  if (!row.agentId && !row.storeId) return `${parts[0]}（全部代理商与医院）`
  if (row.agentId && !row.storeId) return `${parts.join(' / ')}（全部医院）`
  return parts.join(' / ')
}

const dateTimeText = (value?: string) => (value ? `${value.slice(0, 10)} ${value.slice(11, 16)}` : '—')

/** 状态：0=未兑换 2=已绑定（已兑换未核销） 1=已使用 3=已禁用 */
const cardStatusText = (status?: number) =>
  status === 3 ? '已禁用' : status === 1 ? '已使用' : status === 2 ? '已绑定' : '未兑换'
const cardStatusTagType = (status?: number) =>
  status === 3 ? 'danger' : status === 1 ? 'info' : status === 2 ? 'warning' : 'success'

/* ---------------- 列表操作：禁用 / 启用 ---------------- */
const rowActionId = ref<number | null>(null)

const handleDisable = async (row: TrialCard) => {
  const confirmed = await ElMessageBox.confirm(
    `确定要禁用体验卡 ${row.cardNo} 吗？禁用后家长不能兑换，可随时启用恢复。`,
    '禁用体验卡',
    { type: 'warning', confirmButtonText: '确认禁用', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  rowActionId.value = row.id
  try {
    await trialCardApi.disable(row.id)
    ElMessage.success(`体验卡 ${row.cardNo} 已禁用`)
    fetchCards()
  } finally {
    rowActionId.value = null
  }
}

const handleEnable = async (row: TrialCard) => {
  const confirmed = await ElMessageBox.confirm(
    `确定要启用体验卡 ${row.cardNo} 吗？启用后恢复为「未兑换」，家长可正常兑换。`,
    '启用体验卡',
    { type: 'info', confirmButtonText: '确认启用', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  rowActionId.value = row.id
  try {
    await trialCardApi.enable(row.id)
    ElMessage.success(`体验卡 ${row.cardNo} 已启用`)
    fetchCards()
  } finally {
    rowActionId.value = null
  }
}

/* ---------------- 体验卡管理：按编号区间批量禁用 ---------------- */
const manageVisible = ref(false)
const disablingRange = ref(false)
const manageFormRef = ref<FormInstance>()
const manageForm = reactive({ startCardNo: '', endCardNo: '' })

const cardNoValidator = (label: string) => (_rule: any, value: any, callback: (error?: Error) => void) => {
  if (!/^\d{11}$/.test(String(value ?? '').trim())) {
    callback(new Error(`${label}需为 11 位数字`))
    return
  }
  callback()
}

const manageRules: FormRules = {
  startCardNo: [
    { required: true, message: '请输入起始编号', trigger: 'blur' },
    { validator: cardNoValidator('起始编号'), trigger: 'blur' }
  ],
  endCardNo: [
    { required: true, message: '请输入结束编号', trigger: 'blur' },
    { validator: cardNoValidator('结束编号'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const start = manageForm.startCardNo.trim()
        const end = String(value ?? '').trim()
        if (/^\d{11}$/.test(start) && /^\d{11}$/.test(end) && end < start) {
          callback(new Error('结束编号不能小于起始编号'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const openManage = () => {
  manageForm.startCardNo = ''
  manageForm.endCardNo = ''
  manageVisible.value = true
}

const handleDisableRange = async () => {
  if (!manageFormRef.value) return
  const valid = await manageFormRef.value.validate().catch(() => false)
  if (!valid) return
  const startCardNo = manageForm.startCardNo.trim()
  const endCardNo = manageForm.endCardNo.trim()
  const confirmed = await ElMessageBox.confirm(
    `确定要禁用编号 ${startCardNo} 至 ${endCardNo} 区间内的体验卡吗？仅「未兑换」的卡会被禁用。`,
    '批量禁用体验卡',
    { type: 'warning', confirmButtonText: '确认禁用', cancelButtonText: '取消' },
  )
    .then(() => true)
    .catch(() => false)
  if (!confirmed) return
  disablingRange.value = true
  try {
    const result = await trialCardApi.disableRange({ startCardNo, endCardNo })
    manageVisible.value = false
    const skippedTip =
      result.skipped > 0 ? `，跳过 <b style="color:#e6a23c">${result.skipped}</b> 张（已绑定/已使用/已禁用）` : ''
    ElMessageBox.alert(
      `编号区间共 ${result.total} 张体验卡，已禁用 <b style="color:#f56c6c">${result.disabled}</b> 张${skippedTip}`,
      '批量禁用结果',
      { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' },
    )
    handleSearch()
  } finally {
    disablingRange.value = false
  }
}

/* ---------------- 发行 ---------------- */
const issueVisible = ref(false)
const issuing = ref(false)
const issueFormRef = ref<FormInstance>()
const yearPrefix = String(new Date().getFullYear() % 100).padStart(2, '0')
const issueForm = reactive({
  centerId: undefined as number | undefined,
  agentId: undefined as number | undefined,
  storeId: undefined as number | undefined,
  areaCode: '',
  title: '',
  count: 1,
  remark: ''
})

const issueRules: FormRules = {
  centerId: [{ required: true, message: '请选择适用运营中心', trigger: 'change' }],
  title: [
    { required: true, whitespace: true, message: '请填写发行标题', trigger: 'blur' },
    { max: 100, message: '发行标题最多 100 个字符', trigger: 'blur' }
  ],
  count: [
    { required: true, message: '请填写发行数量', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (!Number.isInteger(value) || value < 1 || value > 10000) {
          callback(new Error('发行数量需为 1~10000 的整数'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  areaCode: [
    { required: true, message: '请填写城市电话区号', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        const digits = String(value ?? '').replace(/\D/g, '')
        if (digits.length < 2 || digits.length > 4) {
          callback(new Error('区号请填写 2~4 位数字（3 位区号自动补前导 0）'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const areaCodePreview = computed(() => {
  const digits = issueForm.areaCode.replace(/\D/g, '')
  if (!digits || digits.length > 4) return ''
  return digits.padStart(4, '0')
})

const issueAgentOptions = computed(() =>
  issueForm.centerId ? allAgents.value.filter((item) => item.centerId === issueForm.centerId) : [],
)
const issueStoreOptions = computed(() =>
  allStores.value.filter((store) => {
    const centerId = Number(store.centerId)
    if (issueForm.centerId && centerId !== issueForm.centerId) return false
    if (issueForm.agentId && store.agentId !== issueForm.agentId) return false
    return true
  }),
)

const openIssue = () => {
  issueForm.centerId = undefined
  issueForm.agentId = undefined
  issueForm.storeId = undefined
  issueForm.areaCode = ''
  issueForm.title = ''
  issueForm.count = 1
  issueForm.remark = ''
  issueVisible.value = true
}

const handleIssueCenterChange = () => {
  issueForm.agentId = undefined
  issueForm.storeId = undefined
}

const handleIssueAgentChange = () => {
  issueForm.storeId = undefined
}

const handleIssueSubmit = async () => {
  if (!issueFormRef.value) return
  const valid = await issueFormRef.value.validate().catch(() => false)
  if (!valid) return
  issuing.value = true
  try {
    const cards = await trialCardApi.issue({
      centerId: issueForm.centerId!,
      agentId: issueForm.agentId,
      storeId: issueForm.storeId,
      areaCode: issueForm.areaCode,
      title: issueForm.title.trim(),
      count: issueForm.count,
      remark: issueForm.remark || undefined
    })
    issueVisible.value = false
    if (cards.length === 1) {
      const card = cards[0]!
      ElMessageBox.alert(
        `体验卡编号：<b style="color:#14b8a6;font-size:16px">${card.cardNo}</b><br/>体验卡验证码：<b style="color:#14b8a6;font-size:16px">${card.verifyCode}</b><br/><span style="color:#909399;font-size:12px">请将编号与验证码提供给家长，用于手机端兑换次数</span>`,
        '发行成功',
        { dangerouslyUseHTMLString: true, confirmButtonText: '知道了' },
      )
    } else {
      issuedCards.value = cards
      resultVisible.value = true
    }
    handleSearch()
  } finally {
    issuing.value = false
  }
}

/* ---------------- 批量发行结果 ---------------- */
const resultVisible = ref(false)
const issuedCards = ref<TrialCard[]>([])
/** 结果弹窗最多展示条数：大批量只预览前 N 张，完整编号走列表/导出 */
const RESULT_PREVIEW_LIMIT = 500
const resultCards = computed(() => issuedCards.value.slice(0, RESULT_PREVIEW_LIMIT))
const resultTitle = computed(() => issuedCards.value[0]?.title || '—')

/* ---------------- 导出 ---------------- */
const exporting = ref(false)
const EXPORT_LIMIT = 10000

const handleExport = async () => {
  exporting.value = true
  try {
    const rows: TrialCard[] = []
    let page = 1
    const size = 500
    let total = 0
    do {
      const data = await trialCardApi.getList({
        status: query.status,
        centerId: query.centerId,
        agentId: query.agentId,
        storeId: query.storeId,
        keyword: query.keyword || undefined,
        startDate: dateRange.value?.[0] || undefined,
        endDate: dateRange.value?.[1] || undefined,
        page,
        size
      })
      rows.push(...(data.list ?? []))
      total = Number(data.pagination?.total ?? 0)
      page += 1
    } while (rows.length < total && rows.length < EXPORT_LIMIT)

    if (!rows.length) {
      ElMessage.warning('当前查询条件没有可导出的数据')
      return
    }
    if (total > EXPORT_LIMIT) {
      ElMessage.warning(`数据超过 ${EXPORT_LIMIT} 条，仅导出前 ${EXPORT_LIMIT} 条`)
    }
    exportXlsx({
      sheetName: '项目体验卡',
      fileName: `项目体验卡_${timestamp()}`,
      columns: [
        { title: '体验卡编号', key: 'cardNo', width: 14 },
        { title: '验证码', key: 'verifyCode', width: 12 },
        { title: '标题', key: 'title', width: 24 },
        { title: '适用范围', key: 'scope', width: 32 },
        { title: '状态', key: 'statusText', width: 10 },
        { title: '绑定时间', key: 'boundAtText', width: 20 },
        { title: '使用时间', key: 'usedAtText', width: 20 },
        { title: '使用医院', key: 'usedStoreName', width: 22 },
        { title: '使用儿童', key: 'childName', width: 12 },
        { title: '家长姓名', key: 'parentName', width: 12 },
        { title: '联系方式', key: 'parentPhone', width: 16 },
        { title: '发行人', key: 'creatorName', width: 14 },
        { title: '发行时间', key: 'createdAtText', width: 20 },
        { title: '备注', key: 'remark', width: 20 }
      ],
      rows: rows.map((row) => ({
        cardNo: row.cardNo,
        verifyCode: row.verifyCode,
        title: row.title || '',
        scope: scopeText(row),
        statusText: cardStatusText(row.status),
        boundAtText: dateTimeText(row.boundAt),
        usedAtText: dateTimeText(row.usedAt),
        usedStoreName: row.usedStoreName || '',
        childName: row.childName || '',
        parentName: row.parentName || '',
        parentPhone: row.parentPhone || '',
        creatorName: row.creatorName || '',
        createdAtText: dateTimeText(row.createdAt),
        remark: row.remark || ''
      }))
    })
    ElMessage.success(`已导出 ${rows.length} 条`)
  } finally {
    exporting.value = false
  }
}

const timestamp = () => {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

onMounted(async () => {
  const [centerList, agentList, storeList] = await Promise.all([
    orgApi.getAllCenters(),
    orgApi.getAllAgents(),
    storeApi.getAllStores({ includeDisabled: true })
  ])
  centers.value = centerList ?? []
  allAgents.value = agentList ?? []
  allStores.value = storeList ?? []
  fetchCards()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.title-block {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-width: 0;
}

.card-title {
  flex: 0 0 auto;
  font-size: 16px;
  font-weight: 600;
}

.header-hint {
  font-size: 12px;
  color: #909399;
}

.header-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
}

.query-form {
  margin-bottom: 4px;
}

.field-tip {
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.field-inline-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.result-summary {
  margin-bottom: 6px;
  font-size: 14px;
  color: #303133;
}

.result-count {
  color: #14b8a6;
  font-size: 16px;
}

.result-tip {
  margin-bottom: 12px;
  font-size: 12px;
  color: #909399;
}

.manage-tip {
  margin-bottom: 14px;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
