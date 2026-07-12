<template>
  <div class="vision-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>视力养护记录</span>
          <el-button type="primary" @click="handleExport">
            <el-icon><Download /></el-icon>导出记录
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="儿童姓名">
          <el-input v-model="queryForm.childName" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="childName" label="儿童姓名" width="100" />
        <el-table-column prop="testTime" label="检测时间" width="160" />
        <el-table-column label="左眼视力" width="100">
          <template #default="{ row }">
            <span :class="getVisionClass(row.leftEye)">{{ row.leftEye }}</span>
          </template>
        </el-table-column>
        <el-table-column label="右眼视力" width="100">
          <template #default="{ row }">
            <span :class="getVisionClass(row.rightEye)">{{ row.rightEye }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="beforeAfter" label="养护阶段" width="100">
          <template #default="{ row }">
            <el-tag :type="row.beforeAfter === 'before' ? 'info' : 'success'">
              {{ row.beforeAfter === 'before' ? '养护前' : '养护后' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="testerName" label="检测人" width="100" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
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

    <!-- 视力对比弹窗 -->
    <el-dialog v-model="compareVisible" title="视力对比" width="600px">
      <div v-if="currentRecord">
        <h4>{{ currentRecord.childName }} - 视力变化</h4>
        <el-descriptions :column="2" border style="margin-top: 20px;">
          <el-descriptions-item label="检测时间">{{ currentRecord.testTime }}</el-descriptions-item>
          <el-descriptions-item label="检测人">{{ currentRecord.testerName }}</el-descriptions-item>
          <el-descriptions-item label="左眼视力">{{ currentRecord.leftEye }}</el-descriptions-item>
          <el-descriptions-item label="右眼视力">{{ currentRecord.rightEye }}</el-descriptions-item>
          <el-descriptions-item label="养护阶段">
            <el-tag :type="currentRecord.beforeAfter === 'before' ? 'info' : 'success'">
              {{ currentRecord.beforeAfter === 'before' ? '养护前' : '养护后' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="改善情况">
            <span :class="getImprovementClass(currentRecord.improvement)">
              {{ currentRecord.improvement || '-' }}
            </span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreVision' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import type { VisionRecord } from '@/types'
import { visionApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const compareVisible = ref(false)
const currentRecord = ref<VisionRecord | null>(null)

const queryForm = reactive({
  childName: '',
  dateRange: [] as string[]
})

const pagination = reactive({ page: 1, size: 10, total: 0 })

const tableData = ref<VisionRecord[]>([])

const getVisionClass = (vision: string | undefined) => {
  if (!vision) return ''
  const val = parseFloat(vision)
  if (val >= 5.0) return 'vision-good'
  if (val >= 4.8) return 'vision-normal'
  return 'vision-poor'
}

const getImprovementClass = (improvement: string | undefined) => {
  if (!improvement) return ''
  return improvement.startsWith('+') ? 'improvement-up' : 'improvement-down'
}

const fetchData = async () => {
  loading.value = true
  try {
    const params: {
      storeId?: number
      page?: number
      size?: number
      startDate?: string
      endDate?: string
    } = {
      storeId: userStore.storeId,
      page: pagination.page,
      size: pagination.size
    }
    if (queryForm.dateRange.length === 2) {
      params.startDate = queryForm.dateRange[0]
      params.endDate = queryForm.dateRange[1]
    }
    const res = await visionApi.getVisionRecords(params)
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
  queryForm.childName = ''
  queryForm.dateRange = []
  pagination.page = 1
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

const handleView = async (row: VisionRecord) => {
  try {
    const detail = await visionApi.getVisionRecordDetail(row.id)
    currentRecord.value = detail
  } catch {
    currentRecord.value = row
  }
  compareVisible.value = true
}

const handleExport = () => {
  ElMessage.success('导出功能开发中')
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.vision-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .vision-good {
    color: #52c41a;
    font-weight: bold;
  }

  .vision-normal {
    color: #faad14;
  }

  .vision-poor {
    color: #f5222d;
  }

  .improvement-up {
    color: #52c41a;
    font-weight: bold;
  }

  .improvement-down {
    color: #f5222d;
  }
}
</style>
