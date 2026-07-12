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
              :key="item"
              :label="item"
              :value="item"
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
            placeholder="用户名/操作/URL"
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
        <el-table-column prop="storeName" label="所属门店" width="130" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" min-width="140" show-overflow-tooltip />
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
        <el-descriptions-item label="所属门店">{{ currentLog.storeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.action }}</el-descriptions-item>
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
import { operationLogApi } from '@/api'
import type { OperationLog, OperationLogQuery } from '@/types'

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
  logType: undefined,
  module: undefined,
  keyword: '',
  startDate: undefined,
  endDate: undefined
})

const dateRange = ref<[string, string] | null>(null)

// 模块选项
const moduleOptions = [
  '用户管理', '门店管理', '科室管理', '儿童档案',
  '排班管理', '预约管理', '视力检测', '设备管理',
  '系统设置', '认证授权', '数据统计'
]

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
    tableData.value = res.data.list
    pagination.total = res.data.pagination.total
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
  queryForm.logType = undefined
  queryForm.module = undefined
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

onMounted(() => {
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
