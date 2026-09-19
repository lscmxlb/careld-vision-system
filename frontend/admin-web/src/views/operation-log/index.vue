<template>
  <div class="operation-log-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>操作日志</span>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="医院">
          <el-select
            v-model="queryForm.storeId"
            placeholder="全部医院"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="item in storeOptions"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日志类型">
          <el-select v-model="queryForm.logType" placeholder="全部类型" clearable>
            <el-option label="操作日志" :value="1" />
            <el-option label="登录日志" :value="2" />
            <el-option label="异常日志" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="queryForm.module" placeholder="全部模块" clearable filterable>
            <el-option
              v-for="item in moduleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-select v-model="queryForm.action" placeholder="全部操作" clearable filterable>
            <el-option
              v-for="item in actionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="人员/操作内容/URL"
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
        <el-table-column prop="logType" label="日志类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getLogTypeTag(row.logType)" size="small">
              {{ getLogTypeLabel(row.logType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userName" label="操作用户" width="100" />
        <el-table-column prop="storeName" label="所属医院" width="130" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">
            {{ moduleLabel(row.module) }}
          </template>
        </el-table-column>
        <el-table-column label="操作内容" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || actionLabel(row.action) }}
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="请求方式" width="90">
          <template #default="{ row }">
            <el-tag
              :type="getMethodTag(row.requestMethod)"
              size="small"
              effect="plain"
            >
              {{ row.requestMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求URL" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column prop="executeTime" label="执行耗时" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.executeTime > 1000 ? '#f56c6c' : '#67c23a' }">
              {{ row.executeTime }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">详情</el-button>
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="日志类型">
          <el-tag :type="getLogTypeTag(currentLog.logType)" size="small">
            {{ getLogTypeLabel(currentLog.logType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.status === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ currentLog.userName }}</el-descriptions-item>
        <el-descriptions-item label="所属医院">{{ currentLog.storeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ moduleLabel(currentLog.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作内容">
          {{ currentLog.description || actionLabel(currentLog.action) }}
        </el-descriptions-item>
        <el-descriptions-item label="请求方式">
          <el-tag :type="getMethodTag(currentLog.requestMethod)" size="small" effect="plain">
            {{ currentLog.requestMethod }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">
          <span class="url-text">{{ currentLog.requestUrl }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时">
          <span :style="{ color: currentLog.executeTime > 1000 ? '#f56c6c' : '#67c23a' }">
            {{ currentLog.executeTime }}ms
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">
          {{ formatDateTime(currentLog.createdAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 请求参数 -->
      <div class="json-section" v-if="currentLog?.requestParams">
        <h4>请求参数</h4>
        <pre class="json-content">{{ formatJson(currentLog.requestParams) }}</pre>
      </div>

      <!-- 响应数据 -->
      <div class="json-section" v-if="currentLog?.responseData">
        <h4>响应数据</h4>
        <pre class="json-content">{{ formatJson(currentLog.responseData) }}</pre>
      </div>

      <!-- 错误信息 -->
      <div class="error-section" v-if="currentLog?.errorMsg">
        <h4>错误信息</h4>
        <pre class="error-content">{{ currentLog.errorMsg }}</pre>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminOperationLog' })
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { operationLogApi, storeApi } from '@/api'
import type { OperationLog, OperationLogQuery, Store } from '@/types'

// 表格数据
const loading = ref(false)
const tableData = ref<OperationLog[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 查询表单
const queryForm = reactive<OperationLogQuery>({
  storeId: undefined,
  logType: undefined,
  module: undefined,
  action: undefined,
  keyword: '',
  startDate: undefined,
  endDate: undefined
})

const dateRange = ref<[string, string] | null>(null)

// 医院下拉（数据权限由后端 /stores/all 控制；含禁用医院，历史日志仍需可查）
const storeOptions = ref<Store[]>([])

// 模块选项：value 必须与后端 module 字段一致（英文 key）
const moduleOptions = [
  { label: '儿童档案', value: 'children' },
  { label: '预约管理', value: 'reserve' },
  { label: '排班管理', value: 'schedules' },
  { label: '排班设置', value: 'schedule-rules' },
  { label: '预约规则', value: 'appointment-config' },
  { label: '养护记录', value: 'care-records' },
  { label: '视力记录', value: 'vision' },
  { label: '医院管理', value: 'stores' },
  { label: '设备管理', value: 'devices' },
  { label: '设备类型', value: 'device-types' },
  { label: '科室管理', value: 'departments' },
  { label: '用户管理', value: 'users' },
  { label: '角色权限', value: 'roles' },
  { label: '菜单管理', value: 'menus' },
  { label: '组织架构', value: 'org' },
  { label: '医务人员', value: 'medical-staff' },
  { label: '登录认证', value: 'auth' },
  { label: '统计报表', value: 'statistics' }
]

const actionOptions = [
  { label: '新增', value: 'create' },
  { label: '修改', value: 'update' },
  { label: '删除', value: 'delete' },
  { label: '审核', value: 'audit' },
  { label: '取消', value: 'cancel' },
  { label: '状态变更', value: 'status' },
  { label: '恢复', value: 'restore' },
  { label: '开始', value: 'start' },
  { label: '完成', value: 'complete' },
  { label: '改期', value: 'adjust' },
  { label: '爽约', value: 'no-show' },
  { label: '授权次数', value: 'grant' },
  { label: '绑定', value: 'bind' },
  { label: '解绑', value: 'unbind' },
  { label: '释放', value: 'release' },
  { label: '分配角色', value: 'roles' },
  { label: '权限分配', value: 'permissions' },
  { label: '重置密码', value: 'password' },
  { label: '修改手机号', value: 'phone' },
  { label: '登录', value: 'login' },
  { label: '登出', value: 'logout' }
]

const moduleLabel = (value: string) =>
  moduleOptions.find((item) => item.value === value)?.label || value

const actionLabel = (value: string) =>
  actionOptions.find((item) => item.value === value)?.label || value

// 详情弹窗
const detailVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)

// 日志类型映射
const getLogTypeLabel = (type: number) => {
  const map: Record<number, string> = {
    1: '操作日志',
    2: '登录日志',
    3: '异常日志'
  }
  return map[type] || '未知'
}

const getLogTypeTag = (type: number) => {
  const map: Record<number, string> = {
    1: '',
    2: 'success',
    3: 'danger'
  }
  return map[type] || 'info'
}

// 请求方式标签
const getMethodTag = (method: string) => {
  const map: Record<string, string> = {
    GET: 'success',
    POST: 'primary',
    PUT: 'warning',
    PATCH: 'warning',
    DELETE: 'danger'
  }
  return map[method?.toUpperCase()] || 'info'
}

const formatDateTime = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatJson = (str: string) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

// 日期范围变化
const handleDateChange = (value: [string, string] | null) => {
  if (value) {
    queryForm.startDate = value[0]
    queryForm.endDate = value[1]
  } else {
    queryForm.startDate = undefined
    queryForm.endDate = undefined
  }
}

// 获取日志列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await operationLogApi.getOperationLogList({
      ...queryForm,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取操作日志失败', error)
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
  queryForm.logType = undefined
  queryForm.module = undefined
  queryForm.action = undefined
  queryForm.keyword = ''
  queryForm.startDate = undefined
  queryForm.endDate = undefined
  dateRange.value = null
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

// 查看详情
const handleView = (row: OperationLog) => {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(async () => {
  try {
    storeOptions.value = await storeApi.getAllStores({ includeDisabled: true })
  } catch (error) {
    console.error('获取医院列表失败', error)
  }
  fetchData()
})
</script>

<style scoped lang="scss">
.operation-log-page {
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

  .json-section {
    margin-top: 16px;

    h4 {
      margin: 0 0 8px 0;
      font-size: 14px;
      color: #303133;
    }

    .json-content {
      background: #f5f7fa;
      border: 1px solid #e4e7ed;
      border-radius: 4px;
      padding: 12px;
      font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
      font-size: 12px;
      line-height: 1.6;
      color: #606266;
      overflow-x: auto;
      max-height: 300px;
      overflow-y: auto;
      white-space: pre-wrap;
      word-break: break-all;
      margin: 0;
    }
  }

  .error-section {
    margin-top: 16px;

    h4 {
      margin: 0 0 8px 0;
      font-size: 14px;
      color: #f56c6c;
    }

    .error-content {
      background: #fef0f0;
      border: 1px solid #fde2e2;
      border-radius: 4px;
      padding: 12px;
      font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
      font-size: 12px;
      line-height: 1.6;
      color: #f56c6c;
      overflow-x: auto;
      max-height: 200px;
      overflow-y: auto;
      white-space: pre-wrap;
      word-break: break-all;
      margin: 0;
    }
  }

  .url-text {
    font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
    font-size: 12px;
    color: #409eff;
  }
}
</style>
