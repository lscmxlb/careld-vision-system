<template>
  <div class="authorization-record-page">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="儿童姓名">
          <el-input
            v-model="queryForm.childName"
            placeholder="儿童姓名"
            clearable
            style="width: 100px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号码">
          <el-input
            v-model="queryForm.phone"
            placeholder="号码或片段"
            clearable
            maxlength="11"
            style="width: 130px;"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="授权方式">
          <el-select
            v-model="queryForm.changeType"
            placeholder="全部"
            clearable
            style="width: 105px;"
          >
            <el-option label="预约授权" :value="1" />
            <el-option label="体验卡兑换" :value="6" />
          </el-select>
        </el-form-item>
        <el-form-item label="开单医生">
          <el-select
            v-model="queryForm.doctorName"
            placeholder="全部"
            clearable
            filterable
            style="width: 105px;"
          >
            <el-option v-for="name in doctorOptions" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 210px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>

      <!-- 列表 -->
      <el-table :data="recordList" v-loading="loading" stripe empty-text="暂无授权记录" scrollbar-always-on>
        <el-table-column label="授权时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="儿童姓名" width="100">
          <template #default="{ row }">{{ row.childName || `儿童#${row.childId}` }}</template>
        </el-table-column>
        <el-table-column label="家长姓名" width="100">
          <template #default="{ row }">{{ row.parentName || '-' }}</template>
        </el-table-column>
        <el-table-column label="家长手机" width="120" align="center">
          <template #default="{ row }">{{ row.phoneMask || '-' }}</template>
        </el-table-column>
        <el-table-column label="授权方式" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="row.changeType === 6 ? 'success' : 'primary'" size="small">
              {{ getChangeTypeText(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="授权次数" width="90" align="center">
          <template #default="{ row }">{{ row.changeCount > 0 ? '+' : '' }}{{ row.changeCount }}</template>
        </el-table-column>
        <el-table-column label="缴费金额" width="95" align="right">
          <template #default="{ row }">{{ row.paymentAmount != null ? `¥${row.paymentAmount}` : '-' }}</template>
        </el-table-column>
        <el-table-column label="缴费方式" width="120">
          <template #default="{ row }">{{ row.paymentMethod || '-' }}</template>
        </el-table-column>
        <el-table-column label="开单医生" width="95">
          <template #default="{ row }">{{ row.doctorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 + 导出 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="handleCurrentChange"
        />
        <el-button type="primary" :loading="exporting" @click="handleExport">
          <el-icon><Download /></el-icon>导出 Excel
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreAuthorizationRecord' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import type { ChildServiceRecord } from '@/types'
import { childApi } from '@/api'
import { exportXlsx } from '@/utils/xlsx'

const loading = ref(false)
const exporting = ref(false)
const recordList = ref<ChildServiceRecord[]>([])

/** 导出上限（超出仅导出前 10000 条） */
const EXPORT_LIMIT = 10000

const queryForm = reactive({
  childName: '',
  phone: '',
  changeType: undefined as number | undefined,
  doctorName: ''
})
const doctorOptions = ref<string[]>([])
const dateRange = ref<[string, string] | null>(null)
const pagination = reactive({ page: 1, size: 10, total: 0 })

/** 开单医生下拉选项：来自记录快照去重（覆盖历史/非在职开单人） */
const loadDoctorNames = async () => {
  try {
    doctorOptions.value = await childApi.getDoctorNames()
  } catch {
    // 错误已在拦截器处理
  }
}

const buildQueryParams = (page: number, size: number) => ({
  childName: queryForm.childName || undefined,
  phone: queryForm.phone || undefined,
  changeType: queryForm.changeType || undefined,
  doctorName: queryForm.doctorName || undefined,
  startDate: dateRange.value?.[0],
  endDate: dateRange.value?.[1],
  page,
  size
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await childApi.getAuthorizationRecordPage(buildQueryParams(pagination.page, pagination.size))
    recordList.value = res.list
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
  queryForm.childName = ''
  queryForm.phone = ''
  queryForm.changeType = undefined
  queryForm.doctorName = ''
  dateRange.value = null
  pagination.page = 1
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

const handleExport = async () => {
  exporting.value = true
  try {
    const rows: ChildServiceRecord[] = []
    let page = 1
    let total = 0
    do {
      const res = await childApi.getAuthorizationRecordPage(buildQueryParams(page, 500))
      rows.push(...res.list)
      total = res.pagination.total
      page++
    } while (rows.length < total && rows.length < EXPORT_LIMIT)

    if (rows.length === 0) {
      ElMessage.warning('当前查询条件没有可导出的数据')
      return
    }
    if (total > EXPORT_LIMIT) {
      ElMessage.warning(`数据超过 ${EXPORT_LIMIT} 条，仅导出前 ${EXPORT_LIMIT} 条`)
    }

    exportXlsx({
      sheetName: '授权记录',
      columns: [
        { title: '授权时间', key: 'createdAt', width: 20 },
        { title: '儿童姓名', key: 'childName', width: 12 },
        { title: '家长姓名', key: 'parentName', width: 12 },
        { title: '家长手机', key: 'phoneMask', width: 16 },
        { title: '授权方式', key: 'changeTypeText', width: 12 },
        { title: '授权次数', key: 'changeCount', width: 10, type: 'number' },
        { title: '缴费金额', key: 'paymentAmount', width: 12, type: 'number' },
        { title: '缴费方式', key: 'paymentMethod', width: 12 },
        { title: '开单医生', key: 'doctorName', width: 14 },
        { title: '备注', key: 'remark', width: 24 }
      ],
      rows: rows.map((row) => ({
        createdAt: formatDateTime(row.createdAt),
        childName: row.childName || `儿童#${row.childId}`,
        parentName: row.parentName || '-',
        phoneMask: row.phoneMask || '-',
        changeTypeText: getChangeTypeText(row.changeType),
        changeCount: row.changeCount,
        paymentAmount: row.paymentAmount ?? '',
        paymentMethod: row.paymentMethod || '-',
        doctorName: row.doctorName || '-',
        remark: row.remark || '-'
      })),
      fileName: `授权记录_${timestamp()}`
    })
    ElMessage.success(`已导出 ${rows.length} 条`)
  } catch {
    // 错误已在拦截器处理
  } finally {
    exporting.value = false
  }
}

/** 导出文件名时间戳：YYYYMMDD_HHmmss */
const timestamp = () => {
  const now = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
}

/** 授权时间：createdAt 'YYYY-MM-DDTHH:mm:ss' → 'YYYY-MM-DD HH:mm' */
const formatDateTime = (v?: string) => {
  if (!v) return '-'
  return `${v.slice(0, 10)} ${v.slice(11, 16)}`
}

const getChangeTypeText = (type: number) => {
  const map: Record<number, string> = { 1: '预约授权', 6: '体验卡兑换' }
  return map[type] || '未知'
}

onMounted(() => {
  fetchData()
  loadDoctorNames()
})
</script>

<style scoped lang="scss">
.authorization-record-page {
  /* 筛选区压缩为一行：不换行 + 缩小控件宽度与间距；与下方列表留出间距 */
  .search-form {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    margin-bottom: 20px;

    :deep(.el-form-item) {
      margin-right: 10px;
      margin-bottom: 0;
      flex-shrink: 0;

      &:last-child {
        margin-right: 0;
      }
    }

    :deep(.el-form-item__label) {
      padding-right: 6px;
    }
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
    align-items: center;
    gap: 12px;
  }
}
</style>
