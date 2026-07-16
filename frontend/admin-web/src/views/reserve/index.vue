<template>
  <div class="reserve-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预约管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增预约
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="所属门店">
          <el-select v-model="queryForm.storeId" placeholder="选择门店" clearable filterable>
            <el-option
              v-for="item in storeOptions"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="选择状态" clearable>
            <el-option label="待到店" :value="1" />
            <el-option label="已到店" :value="2" />
            <el-option label="服务中" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约日期">
          <el-date-picker
            v-model="queryForm.date"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="儿童姓名/订单号/手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="childName" label="儿童姓名" width="100" />
        <el-table-column prop="parentName" label="家长姓名" width="100" />
        <el-table-column prop="parentPhone" label="联系电话" width="130" />
        <el-table-column prop="storeName" label="所属门店" min-width="130" show-overflow-tooltip />
        <el-table-column prop="scheduleDate" label="预约日期" width="110" />
        <el-table-column label="预约时段" width="130">
          <template #default="{ row }">
            {{ row.timeSlotStart }}-{{ row.timeSlotEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="reserveType" label="预约类型" width="90">
          <template #default="{ row }">
            {{ getReserveTypeLabel(row.reserveType) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="80">
          <template #default="{ row }">
            {{ getSourceLabel(row.source) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button
              v-if="row.status === 1"
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增预约弹窗 -->
    <el-dialog
      v-model="addDialogVisible"
      title="新增预约"
      width="550px"
      destroy-on-close
    >
      <el-form
        ref="addFormRef"
        :model="addFormData"
        :rules="addFormRules"
        label-width="100px"
      >
        <el-form-item label="所属门店" prop="storeId">
          <el-select v-model="addFormData.storeId" placeholder="选择门店" filterable style="width: 100%">
            <el-option
              v-for="item in storeOptions"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="儿童姓名" prop="childName">
          <el-input v-model="addFormData.childName" placeholder="请输入儿童姓名" />
        </el-form-item>
        <el-form-item label="家长姓名" prop="parentName">
          <el-input v-model="addFormData.parentName" placeholder="请输入家长姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="parentPhone">
          <el-input v-model="addFormData.parentPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="预约日期" prop="scheduleDate">
          <el-date-picker
            v-model="addFormData.scheduleDate"
            type="date"
            placeholder="选择预约日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预约时段" prop="timeSlotStart">
          <el-col :span="11">
            <el-time-select
              v-model="addFormData.timeSlotStart"
              start="08:00"
              step="00:30"
              end="20:00"
              placeholder="开始时间"
              style="width: 100%"
            />
          </el-col>
          <el-col :span="2" class="time-sep">至</el-col>
          <el-col :span="11">
            <el-time-select
              v-model="addFormData.timeSlotEnd"
              start="08:00"
              step="00:30"
              end="20:30"
              placeholder="结束时间"
              style="width: 100%"
            />
          </el-col>
        </el-form-item>
        <el-form-item label="预约类型" prop="reserveType">
          <el-select v-model="addFormData.reserveType" placeholder="选择类型" style="width: 100%">
            <el-option label="视力检测" :value="1" />
            <el-option label="养护" :value="2" />
            <el-option label="复查" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="addFormData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="550px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentReserve">
        <el-descriptions-item label="订单号" :span="2">{{ currentReserve.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="儿童姓名">{{ currentReserve.childName }}</el-descriptions-item>
        <el-descriptions-item label="家长姓名">{{ currentReserve.parentName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ currentReserve.parentPhone }}</el-descriptions-item>
        <el-descriptions-item label="所属门店">{{ currentReserve.storeName }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ currentReserve.scheduleDate }}</el-descriptions-item>
        <el-descriptions-item label="预约时段">{{ currentReserve.timeSlotStart }}-{{ currentReserve.timeSlotEnd }}</el-descriptions-item>
        <el-descriptions-item label="预约类型">{{ getReserveTypeLabel(currentReserve.reserveType) }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ getSourceLabel(currentReserve.source) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTag(currentReserve.status)" size="small">
            {{ getStatusLabel(currentReserve.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="检测师">{{ currentReserve.technicianName }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentReserve.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item v-if="currentReserve.cancelReason" label="取消原因" :span="2">
          <span style="color: #f56c6c">{{ currentReserve.cancelReason }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(currentReserve.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminReserve' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { reserveApi, storeApi } from '@/api'
import type { Reserve, ReserveQuery, Store } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

// 表格数据
const loading = ref(false)
const tableData = ref<Reserve[]>([])
const storeOptions = ref<Store[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 查询表单
const queryForm = reactive<ReserveQuery>({
  storeId: undefined,
  status: undefined,
  date: undefined,
  keyword: ''
})

// 新增弹窗
const addDialogVisible = ref(false)
const addFormRef = ref<FormInstance>()
const submitLoading = ref(false)

const addFormData = reactive<Partial<Reserve>>({
  storeId: undefined,
  childName: '',
  parentName: '',
  parentPhone: '',
  scheduleDate: '',
  timeSlotStart: '',
  timeSlotEnd: '',
  reserveType: 1,
  remark: ''
})

const addFormRules: FormRules = {
  storeId: [{ required: true, message: '请选择所属门店', trigger: 'change' }],
  childName: [{ required: true, message: '请输入儿童姓名', trigger: 'blur' }],
  parentName: [{ required: true, message: '请输入家长姓名', trigger: 'blur' }],
  parentPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  scheduleDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  timeSlotStart: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  timeSlotEnd: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  reserveType: [{ required: true, message: '请选择预约类型', trigger: 'change' }]
}

// 详情弹窗
const detailVisible = ref(false)
const currentReserve = ref<Reserve | null>(null)

// 状态映射
const getStatusLabel = (status: number) => {
  const map: Record<number, string> = {
    1: '待到店',
    2: '已到店',
    3: '服务中',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知'
}

const getStatusTag = (status: number) => {
  const map: Record<number, string> = {
    1: 'info',
    2: 'warning',
    3: '',
    4: 'success',
    5: 'danger'
  }
  return map[status] || 'info'
}

// 预约类型映射
const getReserveTypeLabel = (type: number) => {
  const map: Record<number, string> = {
    1: '视力检测',
    2: '养护',
    3: '复查'
  }
  return map[type] || '其他'
}

// 来源映射
const getSourceLabel = (source?: number) => {
  const map: Record<number, string> = {
    1: '小程序',
    2: '门店',
    3: '电话'
  }
  return source ? (map[source] || '其他') : '-'
}

const formatDateTime = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取门店列表
const fetchStores = async () => {
  try {
    const res = await storeApi.getAllStores()
    storeOptions.value = res
  } catch (error) {
    console.error('获取门店列表失败', error)
  }
}

// 获取预约列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await reserveApi.getReserveList({
      ...queryForm,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取预约列表失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryForm.storeId = undefined
  queryForm.status = undefined
  queryForm.date = undefined
  queryForm.keyword = ''
  pagination.page = 1
  fetchData()
}

// 分页变化
const handleSizeChange = (val: number) => {
  pagination.size = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

// 新增
const handleAdd = () => {
  Object.assign(addFormData, {
    storeId: undefined,
    childName: '',
    parentName: '',
    parentPhone: '',
    scheduleDate: '',
    timeSlotStart: '',
    timeSlotEnd: '',
    reserveType: 1,
    remark: ''
  })
  addDialogVisible.value = true
}

// 提交新增
const handleSubmitAdd = async () => {
  if (!addFormRef.value) return
  await addFormRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        await reserveApi.createReserve(addFormData)
        ElMessage.success('预约创建成功')
        addDialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error('创建预约失败', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

// 查看详情
const handleView = (row: Reserve) => {
  currentReserve.value = row
  detailVisible.value = true
}

// 取消预约
const handleCancel = async (row: Reserve) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约', {
      confirmButtonText: '确定取消',
      cancelButtonText: '返回',
      inputType: 'textarea',
      inputPlaceholder: '请输入取消原因...',
      inputValidator: (val) => {
        if (!val || val.trim().length === 0) {
          return '请输入取消原因'
        }
        return true
      }
    })
    await reserveApi.cancelReserve(row.id, value)
    ElMessage.success('预约已取消')
    fetchData()
  } catch {
    // 取消操作
  }
}

onMounted(() => {
  fetchStores()
  fetchData()
})
</script>

<style scoped lang="scss">
.reserve-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .time-sep {
    text-align: center;
    line-height: 32px;
    color: #909399;
  }
}
</style>
