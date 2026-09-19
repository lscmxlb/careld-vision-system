<template>
  <div class="log-record-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="title">日志记录</span>
        </div>
      </template>

      <el-form :model="query" inline class="filter-form">
        <el-form-item label="操作日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :clearable="true"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="操作人员">
          <el-input v-model="query.userName" placeholder="姓名/账号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="日志类型">
          <el-select v-model="query.logType" clearable placeholder="全部" style="width: 140px">
            <el-option label="操作日志" :value="1" />
            <el-option label="登录日志" :value="2" />
            <el-option label="异常日志" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属模块">
          <el-select v-model="query.module" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="query.action" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" placeholder="描述/接口地址" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="createdAt" label="操作时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="操作人员" width="120">
          <template #default="{ row }">{{ row.userName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="logType" label="日志类型" width="100">
          <template #default="{ row }">
            <el-tag :type="logTypeTagType(row.logType)" effect="plain">
              {{ logTypeText(row.logType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作内容" min-width="180">
          <template #default="{ row }">
            {{ row.description || `${moduleText(row.module)} - ${actionText(row.action)}` }}
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="接口地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="executeTime" label="耗时" width="90">
          <template #default="{ row }">{{ row.executeTime != null ? row.executeTime + 'ms' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchData"
          @size-change="handleSearch"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="620px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="操作时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作人员">{{ detail.userName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属门店">{{ detail.storeName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="日志类型">{{ logTypeText(detail.logType) }}</el-descriptions-item>
        <el-descriptions-item label="所属模块">{{ moduleText(detail.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ actionText(detail.action) }}</el-descriptions-item>
        <el-descriptions-item label="操作内容">{{ detail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="接口地址">{{ detail.requestUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ detail.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ detail.executeTime != null ? detail.executeTime + 'ms' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="结果">
          <el-tag :type="detail.status === 1 ? 'success' : 'danger'">
            {{ detail.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息">
          <span class="error-text">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="json-block">{{ prettyJson(detail.requestParams) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="浏览器标识">
          <span class="break-text">{{ detail.userAgent || '-' }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'LogRecord' })
import { ref, reactive, onMounted } from 'vue'
import type { OperationLog, OperationLogQuery } from '@/types'
import { logRecordApi } from '@/api'

const loading = ref(false)
const tableData = ref<OperationLog[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const dateRange = ref<[string, string] | null>(null)

const detailVisible = ref(false)
const detail = ref<OperationLog | null>(null)

const query = reactive<OperationLogQuery>({
  logType: undefined,
  module: undefined,
  action: undefined,
  userName: undefined,
  keyword: undefined
})

const moduleOptions = [
  { value: 'children', label: '儿童档案' },
  { value: 'reserve', label: '预约管理' },
  { value: 'schedules', label: '排班管理' },
  { value: 'schedule-rules', label: '排班设置' },
  { value: 'appointment-config', label: '预约规则' },
  { value: 'vision', label: '视力记录' },
  { value: 'care-records', label: '养护记录' },
  { value: 'stores', label: '门店管理' },
  { value: 'devices', label: '设备管理' },
  { value: 'device-types', label: '设备类型' },
  { value: 'departments', label: '科室管理' },
  { value: 'users', label: '用户管理' },
  { value: 'roles', label: '角色权限' },
  { value: 'menus', label: '菜单管理' },
  { value: 'org', label: '组织架构' },
  { value: 'medical-staff', label: '医务人员' },
  { value: 'auth', label: '登录认证' },
  { value: 'statistics', label: '统计报表' }
]

const actionOptions = [
  { value: 'create', label: '新增' },
  { value: 'update', label: '修改' },
  { value: 'delete', label: '删除' },
  { value: 'audit', label: '审核' },
  { value: 'cancel', label: '取消' },
  { value: 'status', label: '状态变更' },
  { value: 'restore', label: '恢复' },
  { value: 'start', label: '开始' },
  { value: 'complete', label: '完成' },
  { value: 'adjust', label: '改期' },
  { value: 'no-show', label: '爽约' },
  { value: 'bind', label: '绑定' },
  { value: 'unbind', label: '解绑' },
  { value: 'release', label: '释放' },
  { value: 'roles', label: '分配角色' },
  { value: 'permissions', label: '权限分配' },
  { value: 'password', label: '重置密码' },
  { value: 'phone', label: '修改手机号' },
  { value: 'export', label: '导出' },
  { value: 'login', label: '登录' },
  { value: 'logout', label: '登出' }
]

const moduleMap = new Map(moduleOptions.map(item => [item.value, item.label]))
const actionMap = new Map(actionOptions.map(item => [item.value, item.label]))

const moduleText = (module?: string) => (module ? moduleMap.get(module) || module : '-')
const actionText = (action?: string) => (action ? actionMap.get(action) || action : '-')

const logTypeText = (logType?: number) => {
  switch (logType) {
    case 1:
      return '操作日志'
    case 2:
      return '登录日志'
    case 3:
      return '异常日志'
    default:
      return '-'
  }
}

const logTypeTagType = (logType?: number) => {
  switch (logType) {
    case 2:
      return 'info'
    case 3:
      return 'danger'
    default:
      return 'primary'
  }
}

const formatTime = (value?: string) => (value ? value.replace('T', ' ') : '-')

const prettyJson = (value?: string) => {
  if (!value) return '-'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await logRecordApi.getLogList({
      ...query,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
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
  query.logType = undefined
  query.module = undefined
  query.action = undefined
  query.userName = undefined
  query.keyword = undefined
  dateRange.value = null
  handleSearch()
}

const handleDetail = async (row: OperationLog) => {
  try {
    detail.value = await logRecordApi.getLogDetail(row.id)
    detailVisible.value = true
  } catch {
    // 错误已在拦截器处理
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.log-record-page {
  .card-header {
    .title {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .filter-form {
    margin-bottom: 8px;

    :deep(.el-form-item) {
      margin-bottom: 12px;
    }
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .json-block {
    margin: 0;
    max-height: 240px;
    overflow: auto;
    font-family: Consolas, Monaco, monospace;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .error-text {
    color: #f56c6c;
  }

  .break-text {
    word-break: break-all;
  }
}
</style>
