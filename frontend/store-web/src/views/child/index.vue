<template>
  <div class="child-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>儿童档案管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新建档案
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="姓名/手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="queryForm.auditStatus" placeholder="全部" clearable style="width: 120px;">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" style="width: 120px;" @change="handleSearch">
            <el-option label="正常" :value="1" />
            <el-option label="已禁用" :value="0" />
            <el-option label="全部" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item label="可用次数">
          <span class="count-filter">大于</span>
          <el-input-number
            v-model="queryForm.minRemainingCount"
            :min="0"
            :controls="false"
            placeholder="不限"
            style="width: 80px;"
          />
          <span class="count-filter">次</span>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe :row-class-name="rowClassName" scrollbar-always-on>
        <el-table-column label="建档日期" width="120" align="center">
          <template #default="{ row }">{{ (row.createdAt || '').slice(0, 10) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="name" label="儿童姓名" width="100" align="center" />
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="70" align="center" />
        <el-table-column prop="parentName" label="家长姓名" width="100" align="center">
          <template #default="{ row }">{{ row.parentName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="家长手机" width="130" align="center" />
        <el-table-column label="养护次数" width="90" align="center">
          <template #default="{ row }">{{ row.careCount ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="可用次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(row.remainingCount || 0) > 0 ? 'success' : 'info'" size="small">
              {{ row.remainingCount || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="auditStatus" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getAuditStatusType(row.auditStatus)">
              {{ getAuditStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="childCode" label="档案编号" width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="326" fixed="right" align="right">
          <template #default="{ row }">
            <el-button
              v-if="row.sourceType === 1 && row.auditStatus === 0"
              type="warning"
              size="small"
              @click="handleAudit(row)"
            >审核</el-button>
            <el-button type="success" size="small" @click="handleGrant(row)">预约授权</el-button>
            <el-button type="info" size="small" @click="handleServiceRecords(row)">预约记录</el-button>
            <el-button type="primary" size="small" @click="handleEdit(row)">档案详情</el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              size="small"
              @click="handleToggleStatus(row, 0)"
            >禁用</el-button>
            <el-button
              v-else
              type="primary"
              size="small"
              @click="handleToggleStatus(row, 1)"
            >启用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗（禁止点遮罩/ESC关闭，有未保存修改时二次确认） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="min(760px, calc(100vw - 32px))"
      top="3vh"
      class="child-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="儿童姓名" prop="name">
              <el-input v-model="formData.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="formData.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="0">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker v-model="formData.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在学校" prop="school">
              <el-input v-model="formData.school" maxlength="50" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="家长姓名" prop="parentName">
              <el-input v-model="formData.parentName" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关系" prop="relation">
              <el-select v-model="formData.relation" placeholder="请选择关系" clearable style="width: 100%;">
                <el-option label="妈妈" value="妈妈" />
                <el-option label="爸爸" value="爸爸" />
                <el-option label="爷爷" value="爷爷" />
                <el-option label="奶奶" value="奶奶" />
                <el-option label="外公" value="外公" />
                <el-option label="外婆" value="外婆" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phone">
              <el-input v-model="formData.phone" maxlength="11" placeholder="11位中国手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="家庭地址" prop="homeAddress">
              <el-input v-model="formData.homeAddress" maxlength="100" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">视力情况</el-divider>
        <el-form-item label="视力状况" prop="eyeConditions">
          <el-checkbox-group v-model="formData.eyeConditions" @change="handleEyeConditionChange" class="eye-grid">
            <el-checkbox value="正常">正常</el-checkbox>
            <el-checkbox value="轻度近视">轻度近视</el-checkbox>
            <el-checkbox value="中度近视">中度近视</el-checkbox>
            <el-checkbox value="高度近视">高度近视</el-checkbox>
            <el-checkbox value="斜视">斜视</el-checkbox>
            <el-checkbox value="弱视">弱视</el-checkbox>
            <el-checkbox value="散光">散光</el-checkbox>
            <el-checkbox value="远视">远视</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="裸眼视力">
          <div class="naked-vision-row">
            <span class="naked-vision-label">双眼</span>
            <el-select v-model="formData.nakedVisionBoth" placeholder="请选择" clearable>
              <el-option v-for="v in VISION_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <span class="naked-vision-label">左眼</span>
            <el-select v-model="formData.nakedVisionLeft" placeholder="请选择" clearable>
              <el-option v-for="v in VISION_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
            <span class="naked-vision-label">右眼</span>
            <el-select v-model="formData.nakedVisionRight" placeholder="请选择" clearable>
              <el-option v-for="v in VISION_OPTIONS" :key="v" :label="v" :value="v" />
            </el-select>
          </div>
        </el-form-item>

        <el-divider content-position="left">其它信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分娩方式" prop="deliveryType">
              <el-select v-model="formData.deliveryType" placeholder="请选择分娩方式" clearable style="width: 100%;">
                <el-option label="顺产" value="顺产" />
                <el-option label="剖宫产" value="剖宫产" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日常作息" prop="bedtime">
              <div class="schedule-row">
                <el-time-picker
                  v-model="formData.bedtime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="休息时间"
                />
                <span class="schedule-sep">至</span>
                <el-time-picker
                  v-model="formData.wakeTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="起床时间"
                />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="既往病史">
              <el-input v-model="formData.medicalHistory" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="过敏信息">
              <el-input v-model="formData.allergyInfo" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="主治医师" prop="doctorId">
          <el-select v-model="formData.doctorId" placeholder="请选择主治医师（已注册医生）" style="width: 100%;">
            <el-option v-for="doc in doctorOptions" :key="doc.id" :label="doc.name" :value="doc.id!" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存档案</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="档案审核" width="460px">
      <el-form label-width="90px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.auditStatus === 1" label="主治医生" required>
          <el-select v-model="auditForm.doctorId" placeholder="请选择主治医生（已注册医生）" style="width: 100%;">
            <el-option v-for="doc in doctorOptions" :key="doc.id" :label="doc.name" :value="doc.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="2" placeholder="驳回时建议填写原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="handleAuditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 预约授权弹窗 -->
    <el-dialog v-model="grantVisible" :title="`预约授权 - ${grantChildTitle}`" width="480px">
      <el-form ref="grantFormRef" :model="grantForm" :rules="grantRules" label-width="90px">
        <el-form-item label="可用次数">
          <el-tag type="success">{{ grantChild?.remainingCount || 0 }} 次</el-tag>
        </el-form-item>
        <el-form-item label="开单医师" prop="doctorId">
          <el-select v-model="grantForm.doctorId" placeholder="请选择开单医师" style="width: 160px;">
            <el-option v-for="doc in doctorOptions" :key="doc.id" :label="doc.name" :value="doc.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权次数" prop="changeCount">
          <el-input-number
            v-model="grantForm.changeCount"
            :min="1"
            :max="99"
            @change="calcPaymentAmount"
          />
        </el-form-item>
        <el-form-item label="缴费方式" prop="paymentMethod">
          <el-select
            v-model="grantForm.paymentMethod"
            placeholder="请选择缴费方式"
            style="width: 160px;"
            @change="calcPaymentAmount"
          >
            <el-option label="自费支付" value="自费支付" />
            <el-option label="医保-个人余额" value="医保-个人余额" />
            <el-option label="医保-统筹支付" value="医保-统筹支付" />
            <el-option label="免费体验" value="免费体验" />
            <el-option label="其它" value="其它" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴费金额" prop="paymentAmount">
          <el-input-number v-model="grantForm.paymentAmount" :min="0" :precision="2" :step="100"
            placeholder="0.00" style="width: 160px;" />
          <span class="grant-unit">元</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="grantForm.remark" type="textarea" :rows="2" placeholder="如：购买套餐A / 活动赠送" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="grantLoading" @click="handleGrantSubmit">确定授予</el-button>
      </template>
    </el-dialog>

    <!-- 预约记录弹窗 -->
    <el-dialog v-model="recordsVisible" :title="`预约记录详情 - ${recordsChildTitle}`" width="860px">
      <el-table :data="serviceRecords" v-loading="recordsLoading" size="small" stripe scrollbar-always-on>
        <el-table-column label="日期" width="95">
          <template #default="{ row }">{{ (row.createdAt || '').slice(0, 10) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="60">
          <template #default="{ row }">{{ (row.createdAt || '').slice(11, 16) }}</template>
        </el-table-column>
        <el-table-column label="变更类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.changeCount > 0 ? 'success' : 'danger'" size="small">
              {{ getChangeTypeText(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更次数" width="80" align="center">
          <template #default="{ row }">{{ row.changeCount > 0 ? '+' : '' }}{{ row.changeCount }}</template>
        </el-table-column>
        <el-table-column prop="remainingAfter" label="可用次数" width="80" align="center" />
        <el-table-column label="缴费方式" width="118">
          <template #default="{ row }">{{ row.paymentMethod || '-' }}</template>
        </el-table-column>
        <el-table-column label="缴费金额" width="90" align="right">
          <template #default="{ row }">{{ row.paymentAmount != null ? `¥${row.paymentAmount}` : '-' }}</template>
        </el-table-column>
        <el-table-column label="开单医生" width="85">
          <template #default="{ row }">{{ row.doctorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" />
      </el-table>
      <template #footer>
        <el-button @click="recordsVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreChild' })
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Child, ChildServiceRecord, MedicalStaff } from '@/types'
import { childApi, medicalStaffApi, departmentApi } from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<Child[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新建档案')
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const queryForm = reactive({
  keyword: '',
  auditStatus: undefined as number | undefined,
  /** -1=全部 1=正常(默认) 0=已禁用 */
  status: 1 as number,
  /** 可用次数大于该值（不填=不限） */
  minRemainingCount: undefined as number | undefined
})
const pagination = reactive({ page: 1, size: 10, total: 0 })

const formData = reactive({
  name: '',
  parentName: '',
  relation: '',
  doctorId: undefined as number | undefined,
  phone: '',
  birthDate: '',
  gender: 1,
  homeAddress: '',
  school: '',
  deliveryType: '',
  bedtime: '',
  wakeTime: '',
  eyeConditions: [] as string[],
  nakedVisionBoth: '',
  nakedVisionLeft: '',
  nakedVisionRight: '',
  medicalHistory: '',
  allergyInfo: '',
  familyHistory: ''
})

/** 裸眼视力可选值：5.3 递减至 4.0（0.1 步长） */
const VISION_OPTIONS = Array.from({ length: 14 }, (_, i) => (5.3 - i * 0.1).toFixed(1))

/** 视力状况互斥规则：①“正常”独占，选中后其他项自动取消；②轻度/中度/高度近视三选一 */
const handleEyeConditionChange = (vals: string[]) => {
  const last = vals[vals.length - 1]
  if (last === '正常') {
    formData.eyeConditions = ['正常']
    return
  }
  const next = vals.filter(v => v !== '正常')
  if (last === '轻度近视' || last === '中度近视' || last === '高度近视') {
    formData.eyeConditions = next.filter(v => v === last || (v !== '轻度近视' && v !== '中度近视' && v !== '高度近视'))
    return
  }
  formData.eyeConditions = next
}

const formRules = {
  name: [{ required: true, message: '请输入儿童姓名', trigger: 'blur' }],
  parentName: [{ required: true, message: '请输入家长姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位中国手机号', trigger: 'blur' }
  ],
  doctorId: [{ required: true, message: '请选择主治医师', trigger: 'change' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  eyeConditions: [{ type: 'array', required: true, message: '请选择视力状况', trigger: 'change' }]
}

const getAuditStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getAuditStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }
  return map[status] || '未知'
}

/** 档案启用/禁用状态展示：status 1=启用, 0=禁用 */
const getStatusType = (status: number) => {
  return status === 1 ? 'success' : 'info'
}
const getStatusText = (status: number) => {
  return status === 1 ? '启用' : '已禁用'
}

/** 禁用行灰显 */
const rowClassName = ({ row }: { row: Child }) => {
  return row.status === 1 ? '' : 'disabled-row'
}

const fetchData = async () => {
  loading.value = true
  try {
    const allSelected = queryForm.status === -1
    const res = await childApi.getChildList({
      storeId: userStore.storeId,
      keyword: queryForm.keyword || undefined,
      auditStatus: queryForm.auditStatus,
      status: allSelected ? undefined : queryForm.status,
      includeDisabled: allSelected || undefined,
      remainingCountMin: queryForm.minRemainingCount,
      page: pagination.page,
      size: pagination.size
    })
    // 后端 /children 返回全量数组（未分页），前端本地分页展示
    const list: Child[] = Array.isArray(res) ? res : (res as unknown as { list?: Child[] }).list || []
    pagination.total = list.length
    const start = (pagination.page - 1) * pagination.size
    tableData.value = list.slice(start, start + pagination.size)
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.auditStatus = undefined
  queryForm.status = 1
  queryForm.minRemainingCount = undefined
  pagination.page = 1
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

/** 编辑弹窗脏检查：打开时保存快照，取消/关闭前 JSON 对比二次确认 */
const formSnapshot = ref('')
const captureSnapshot = () => {
  formSnapshot.value = JSON.stringify(formData)
}
const isDirty = () => JSON.stringify(formData) !== formSnapshot.value

const confirmDiscard = async () => {
  if (!isDirty()) return true
  try {
    await ElMessageBox.confirm('有未保存的修改，确认放弃吗？', '提示', {
      confirmButtonText: '放弃修改',
      cancelButtonText: '继续编辑',
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}

const handleCancel = async () => {
  if (await confirmDiscard()) dialogVisible.value = false
}

const handleDialogClose = async (done: () => void) => {
  if (await confirmDiscard()) done()
}

const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新建档案'
  Object.assign(formData, {
    name: '', parentName: '', relation: '', doctorId: undefined, phone: '', birthDate: '', gender: 1,
    homeAddress: '', school: '', deliveryType: '', bedtime: '', wakeTime: '',
    eyeConditions: [], nakedVisionBoth: '', nakedVisionLeft: '', nakedVisionRight: '',
    medicalHistory: '', allergyInfo: '', familyHistory: ''
  })
  captureSnapshot()
  loadDoctors()
  dialogVisible.value = true
}

// 列表行的 name/phone 是掩码，编辑前拉详情接口取明文回填，避免把掩码当明文提交污染数据
const handleEdit = async (row: Child) => {
  let detail: Child
  try {
    detail = await childApi.getChildDetail(row.id)
  } catch {
    return // 错误已在拦截器处理
  }
  isEdit.value = true
  editingId.value = detail.id
  dialogTitle.value = '编辑档案'
  loadDoctors()
  Object.assign(formData, {
    name: detail.name,
    parentName: detail.parentName || '',
    relation: detail.relation || '',
    doctorId: detail.doctorId,
    phone: detail.phone,
    birthDate: detail.birthDate,
    gender: detail.gender,
    homeAddress: detail.homeAddress || '',
    school: detail.school || '',
    deliveryType: detail.deliveryType || '',
    bedtime: detail.bedtime || '',
    wakeTime: detail.wakeTime || '',
    eyeConditions: detail.eyeCondition ? detail.eyeCondition.split(/[、,，]/).filter(Boolean) : [],
    nakedVisionBoth: detail.nakedVisionBoth || '',
    nakedVisionLeft: detail.nakedVisionLeft || '',
    nakedVisionRight: detail.nakedVisionRight || '',
    medicalHistory: detail.medicalHistory || '',
    allergyInfo: detail.allergyInfo || '',
    familyHistory: detail.familyHistory || ''
  })
  captureSnapshot()
  dialogVisible.value = true
}

const handleView = (row: Child) => {
  ElMessage.info(`查看档案: ${row.name}`)
}

// ==================== 档案审核（家长自建档案） ====================
const auditVisible = ref(false)
const auditLoading = ref(false)
const auditingChild = ref<Child | null>(null)
const auditForm = reactive({ auditStatus: 1, auditRemark: '', doctorId: undefined as number | undefined })

const handleAudit = (row: Child) => {
  auditingChild.value = row
  auditForm.auditStatus = 1
  auditForm.auditRemark = ''
  auditForm.doctorId = undefined
  loadDoctors()
  auditVisible.value = true
}

const handleAuditSubmit = async () => {
  if (!auditingChild.value) return
  if (auditForm.auditStatus === 2 && !auditForm.auditRemark.trim()) {
    ElMessage.warning('驳回时请填写审核备注')
    return
  }
  if (auditForm.auditStatus === 1 && !auditForm.doctorId) {
    ElMessage.warning('审核通过前请指定主治医生')
    return
  }
  auditLoading.value = true
  try {
    const doctor = doctorOptions.value.find(d => d.id === auditForm.doctorId)
    await childApi.auditChild(auditingChild.value.id, {
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark || undefined,
      doctorId: auditForm.auditStatus === 1 ? auditForm.doctorId : undefined,
      doctorName: auditForm.auditStatus === 1 ? doctor?.name : undefined
    })
    ElMessage.success(auditForm.auditStatus === 1 ? '审核已通过' : '已驳回')
    auditVisible.value = false
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    auditLoading.value = false
  }
}

// ==================== 启用/禁用档案 ====================
const handleToggleStatus = async (row: Child, status: number) => {
  const action = status === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${action}档案「${row.name}」吗？${status === 0 ? '禁用后该档案将从列表隐藏且无法预约。' : ''}`,
      `${action}档案`,
      { confirmButtonText: action, cancelButtonText: '取消', type: status === 0 ? 'warning' : 'info' }
    )
  } catch {
    return // 用户取消
  }
  try {
    await childApi.setChildStatus(row.id, { status })
    ElMessage.success(`${action}成功`)
    fetchData()
  } catch {
    // 错误已在拦截器处理
  }
}

// ==================== 授予可约次数 ====================
const grantVisible = ref(false)
const grantLoading = ref(false)
const grantChild = ref<Child | null>(null)
/** 弹窗标题：列表行是掩码，打开后拉详情换成"完整姓名[手机号]" */
const grantChildTitle = ref('')
const grantFormRef = ref<FormInstance>()
const doctorOptions = ref<MedicalStaff[]>([])
const grantForm = reactive({
  changeCount: 1 as number | undefined,
  paymentAmount: 0 as number | undefined,
  paymentMethod: '自费支付',
  doctorId: undefined as number | undefined,
  remark: ''
})
const grantRules = {
  changeCount: [{ required: true, message: '请填写授权次数', trigger: 'change' }],
  paymentAmount: [{ required: true, message: '请填写缴费金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择缴费方式', trigger: 'change' }],
  doctorId: [{ required: true, message: '请选择开单医师', trigger: 'change' }]
}

/** 需按"次数×科室收费标准"计费的缴费方式 */
const CHARGED_METHODS = ['自费支付', '医保-个人余额', '医保-统筹支付']
/** 本店第一条启用科室的收费标准（元），免费/其它方式按 0 元计 */
const departmentStandard = ref(0)

/** 读取本店第一条启用科室的收费标准（预约授权自动计费用） */
const loadDepartmentStandard = async () => {
  try {
    const list = await departmentApi.getDepartmentList(userStore.storeId!)
    const firstEnabled = (list || []).find(d => d.status === 1)
    departmentStandard.value = Number(firstEnabled?.chargeStandard) || 0
  } catch {
    departmentStandard.value = 0
  }
}

const calcPaymentAmount = () => {
  if (!CHARGED_METHODS.includes(grantForm.paymentMethod)) {
    grantForm.paymentAmount = 0
    return
  }
  const count = grantForm.changeCount || 0
  grantForm.paymentAmount = Math.round(count * departmentStandard.value * 100) / 100
}

/** 拉取本店在职医生（staffRole=1=医生）供开单医生下拉 */
const loadDoctors = async () => {
  try {
    const res = await medicalStaffApi.getStaffList({ staffRole: 1, status: 1, page: 1, size: 200 })
    doctorOptions.value = (res.list || []) as MedicalStaff[]
  } catch {
    // 错误已在拦截器处理
  }
}

const handleGrant = (row: Child) => {
  grantChild.value = row
  grantChildTitle.value = row.name || ''
  grantForm.changeCount = 1
  grantForm.paymentAmount = 0
  grantForm.paymentMethod = '自费支付'
  grantForm.doctorId = undefined
  grantForm.remark = ''
  grantVisible.value = true
  loadDoctors()
  // 先取科室收费标准再按默认值（1 次 × 自费支付）计算金额
  loadDepartmentStandard().then(calcPaymentAmount)
  childApi.getChildDetail(row.id).then((detail) => {
    grantChildTitle.value = `${detail.name || row.name || ''}[${detail.phone || ''}]`
  }).catch(() => {
    // 详情拉取失败时保留列表掩码显示
  })
}

const handleGrantSubmit = async () => {
  if (!grantChild.value) return
  if (grantFormRef.value) {
    const valid = await grantFormRef.value.validate().catch(() => false)
    if (!valid) return
  }
  grantLoading.value = true
  try {
    const doctor = doctorOptions.value.find(d => d.id === grantForm.doctorId)
    await childApi.grantServiceRecord(grantChild.value.id, {
      changeCount: grantForm.changeCount!,
      paymentAmount: grantForm.paymentAmount!,
      paymentMethod: grantForm.paymentMethod,
      doctorId: grantForm.doctorId!,
      doctorName: doctor?.name,
      remark: grantForm.remark || undefined
    })
    ElMessage.success(`已预约授权 ${grantForm.changeCount} 次`)
    grantVisible.value = false
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    grantLoading.value = false
  }
}

// ==================== 次数变更流水 ====================
const recordsVisible = ref(false)
const recordsLoading = ref(false)
const recordsChild = ref<Child | null>(null)
/** 弹窗标题：列表行是掩码，打开后拉详情换成"完整姓名[手机号]" */
const recordsChildTitle = ref('')
const serviceRecords = ref<ChildServiceRecord[]>([])

const getChangeTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '预约授权', 2: '预约扣减', 3: '取消退还', 4: '爽约退还', 5: '爽约不退还'
  }
  return map[type] || '未知'
}

const handleServiceRecords = async (row: Child) => {
  recordsChild.value = row
  recordsChildTitle.value = row.name || ''
  recordsVisible.value = true
  recordsLoading.value = true
  childApi.getChildDetail(row.id).then((detail) => {
    recordsChildTitle.value = `${detail.name || row.name || ''}[${detail.phone || ''}]`
  }).catch(() => {
    // 详情拉取失败时保留列表掩码显示
  })
  try {
    serviceRecords.value = await childApi.getServiceRecords(row.id)
  } catch {
    // 错误已在拦截器处理
  } finally {
    recordsLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const doctor = doctorOptions.value.find(d => d.id === formData.doctorId)
    const data = {
      name: formData.name,
      parentName: formData.parentName || undefined,
      relation: formData.relation || undefined,
      doctorId: formData.doctorId,
      doctorName: doctor?.name,
      phone: formData.phone,
      storeId: userStore.storeId,
      birthDate: formData.birthDate,
      gender: formData.gender,
      homeAddress: formData.homeAddress || undefined,
      school: formData.school || undefined,
      deliveryType: formData.deliveryType || undefined,
      bedtime: formData.bedtime || undefined,
      wakeTime: formData.wakeTime || undefined,
      eyeCondition: formData.eyeConditions.join('、'),
      nakedVisionBoth: formData.nakedVisionBoth || undefined,
      nakedVisionLeft: formData.nakedVisionLeft || undefined,
      nakedVisionRight: formData.nakedVisionRight || undefined,
      medicalHistory: formData.medicalHistory || undefined,
      allergyInfo: formData.allergyInfo || undefined,
      familyHistory: formData.familyHistory || undefined
    }
    if (isEdit.value && editingId.value) {
      await childApi.updateChild(editingId.value, data)
    } else {
      await childApi.createChild(data)
    }
    ElMessage.success('保存成功')
    captureSnapshot()
    dialogVisible.value = false
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchData()
  // 来自「新建预约」建档引导：预填家长手机号并打开建档弹窗
  const phone = route.query.phone
  if (typeof phone === 'string' && /^1[3-9]\d{9}$/.test(phone)) {
    handleAdd()
    formData.phone = phone
    captureSnapshot()
    ElMessage.info('已从预约流程带入家长手机号，完善儿童信息后提交即可（门店建档自动通过审核）')
  }
})
</script>

<style scoped lang="scss">
.child-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .schedule-row {
    display: flex;
    align-items: center;
    width: 100%;

    /* 半列宽下两个时间选择器均分剩余宽度 */
    :deep(.el-date-editor) {
      flex: 1;
      min-width: 0;
    }

    .schedule-sep {
      margin: 0 6px;
      flex: none;
    }
  }

  .naked-vision-row {
    display: flex;
    align-items: center;
    width: 100%;

    :deep(.el-select) {
      flex: 1;
      min-width: 0;
    }

    .naked-vision-label {
      margin: 0 6px 0 14px;
      color: #606266;
      font-size: 14px;
      flex: none;

      &:first-child {
        margin-left: 0;
      }
    }
  }

  .count-filter {
    margin: 0 4px;
    color: #606266;
    font-size: 14px;
  }

  /* 视力状况 8 项两行各 4 列对齐 */
  .eye-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    row-gap: 4px;
    width: 100%;

    :deep(.el-checkbox) {
      margin-right: 0;
    }
  }

  .grant-unit {
    margin-left: 8px;
    color: #606266;
    font-size: 14px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

/* 禁用档案行灰显 */
:deep(.el-table .disabled-row) {
  color: #c0c4cc;
  td {
    background: #fafafa !important;
  }
}
</style>

<style lang="scss">
/* 儿童档案新建/编辑弹窗：整体上移并压缩间距，尽量一屏显示；内容超高时弹窗内滚动 */
.child-dialog {
  .el-dialog__body {
    padding-top: 6px;
    padding-bottom: 10px;
    max-height: calc(97vh - 130px);
    overflow-y: auto;
    /* el-row gutter 负边距外扩 8px 落在 body 内边距内，但会让 overflow-x 变成 auto 产生横向滚动条，显式隐藏 */
    overflow-x: hidden;
  }

  .el-divider--horizontal {
    margin: 12px 0 14px;

    &:first-child {
      margin-top: 0;
    }
  }

  .el-form-item {
    margin-bottom: 12px;
  }
}
</style>
