<template>
  <div class="vision-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>视力记录管理</span>
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
        <el-form-item label="检测类型">
          <el-select v-model="queryForm.testType" placeholder="选择类型" clearable>
            <el-option label="检测前" :value="1" />
            <el-option label="检测后" :value="2" />
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
            placeholder="儿童姓名/记录编号"
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
        <el-table-column prop="recordCode" label="记录编号" width="140" />
        <el-table-column prop="childName" label="儿童姓名" width="100" />
        <el-table-column prop="storeName" label="所属门店" min-width="130" show-overflow-tooltip />
        <el-table-column prop="eyeType" label="眼别" width="80">
          <template #default="{ row }">
            <el-tag :type="getEyeTypeTag(row.eyeType)" size="small">
              {{ getEyeTypeLabel(row.eyeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="visionLevel" label="视力等级" width="100" />
        <el-table-column prop="visionDecimal" label="小数记录" width="90">
          <template #default="{ row }">
            {{ row.visionDecimal ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="testType" label="检测类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.testType === 1 ? 'info' : 'success'" size="small">
              {{ row.testType === 1 ? '检测前' : '检测后' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="testerName" label="检测人" width="90" />
        <el-table-column prop="testTime" label="检测时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.testTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button type="success" size="small" @click="handleCompare(row)">对比</el-button>
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
    <el-dialog v-model="detailVisible" title="视力记录详情" width="550px" destroy-on-close>
      <el-descriptions :column="2" border v-if="currentRecord">
        <el-descriptions-item label="记录编号">{{ currentRecord.recordCode }}</el-descriptions-item>
        <el-descriptions-item label="儿童姓名">{{ currentRecord.childName }}</el-descriptions-item>
        <el-descriptions-item label="所属门店">{{ currentRecord.storeName }}</el-descriptions-item>
        <el-descriptions-item label="检测类型">
          <el-tag :type="currentRecord.testType === 1 ? 'info' : 'success'" size="small">
            {{ currentRecord.testType === 1 ? '检测前' : '检测后' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="眼别">
          <el-tag :type="getEyeTypeTag(currentRecord.eyeType)" size="small">
            {{ getEyeTypeLabel(currentRecord.eyeType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="视力等级">{{ currentRecord.visionLevel }}</el-descriptions-item>
        <el-descriptions-item label="小数记录">{{ currentRecord.visionDecimal ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="检测人">{{ currentRecord.testerName }}</el-descriptions-item>
        <el-descriptions-item label="检测时间" :span="2">{{ formatDateTime(currentRecord.testTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentRecord.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(currentRecord.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 视力对比弹窗 -->
    <el-dialog v-model="compareVisible" title="视力检测对比" width="650px" destroy-on-close>
      <div v-loading="compareLoading">
        <template v-if="compareData">
          <div class="compare-header">
            <span>儿童：{{ compareData.childName }}</span>
            <span>预约日期：{{ compareData.reserveDate }}</span>
          </div>
          <el-row :gutter="20">
            <el-col :span="11">
              <div class="compare-card before">
                <h4>检测前</h4>
                <p>检测时间：{{ formatDateTime(compareData.beforeTest.testTime) }}</p>
                <p>左眼：<strong>{{ compareData.beforeTest.leftEye }}</strong></p>
                <p>右眼：<strong>{{ compareData.beforeTest.rightEye }}</strong></p>
                <p>检测人：{{ compareData.beforeTest.testerName }}</p>
              </div>
            </el-col>
            <el-col :span="2" class="compare-arrow">
              <el-icon :size="32"><Right /></el-icon>
            </el-col>
            <el-col :span="11">
              <div class="compare-card after">
                <h4>检测后</h4>
                <p>检测时间：{{ formatDateTime(compareData.afterTest.testTime) }}</p>
                <p>左眼：<strong>{{ compareData.afterTest.leftEye }}</strong></p>
                <p>右眼：<strong>{{ compareData.afterTest.rightEye }}</strong></p>
                <p>检测人：{{ compareData.afterTest.testerName }}</p>
              </div>
            </el-col>
          </el-row>
          <el-divider />
          <div class="compare-result">
            <h4>改善情况</h4>
            <el-row :gutter="20">
              <el-col :span="12">
                <p>左眼改善：<strong class="improvement">{{ compareData.improvement.leftEye }}</strong></p>
              </el-col>
              <el-col :span="12">
                <p>右眼改善：<strong class="improvement">{{ compareData.improvement.rightEye }}</strong></p>
              </el-col>
            </el-row>
          </div>
        </template>
        <el-empty v-else description="暂无对比数据" />
      </div>
      <template #footer>
        <el-button @click="compareVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminVision' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Right } from '@element-plus/icons-vue'
import { visionApi, storeApi } from '@/api'
import type { VisionRecord, VisionRecordQuery, VisionCompare, Store } from '@/types'

// 表格数据
const loading = ref(false)
const tableData = ref<VisionRecord[]>([])
const storeOptions = ref<Store[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 查询表单
const queryForm = reactive<VisionRecordQuery>({
  storeId: undefined,
  testType: undefined,
  keyword: '',
  startDate: undefined,
  endDate: undefined
})

const dateRange = ref<[string, string] | null>(null)

// 详情弹窗
const detailVisible = ref(false)
const currentRecord = ref<VisionRecord | null>(null)

// 对比弹窗
const compareVisible = ref(false)
const compareLoading = ref(false)
const compareData = ref<VisionCompare | null>(null)

// 眼别映射
const getEyeTypeLabel = (type: number) => {
  const map: Record<number, string> = {
    1: '左眼',
    2: '右眼',
    3: '双眼'
  }
  return map[type] || '未知'
}

const getEyeTypeTag = (type: number) => {
  const map: Record<number, string> = {
    1: '',
    2: 'success',
    3: 'warning'
  }
  return map[type] || 'info'
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

// 获取门店列表
const fetchStores = async () => {
  try {
    const res = await storeApi.getAllStores()
    storeOptions.value = res
  } catch (error) {
    console.error('获取门店列表失败', error)
  }
}

// 获取视力记录列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await visionApi.getVisionRecordList({
      ...queryForm,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.data.list
    pagination.total = res.data.pagination.total
  } catch (error) {
    console.error('获取视力记录列表失败', error)
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
  queryForm.testType = undefined
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
const handleView = async (row: VisionRecord) => {
  try {
    const detail = await visionApi.getVisionRecordDetail(row.id)
    currentRecord.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('获取记录详情失败')
  }
}

// 视力对比
const handleCompare = async (row: VisionRecord) => {
  if (!row.reserveId) {
    ElMessage.warning('该记录无关联预约，无法进行对比')
    return
  }
  compareVisible.value = true
  compareLoading.value = true
  compareData.value = null
  try {
    compareData.value = await visionApi.compareVision({
      childId: row.childId,
      reserveId: row.reserveId
    })
  } catch (error) {
    console.error('获取对比数据失败', error)
  } finally {
    compareLoading.value = false
  }
}

onMounted(() => {
  fetchStores()
  fetchData()
})
</script>

<style scoped lang="scss">
.vision-page {
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

  .compare-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20px;
    font-size: 14px;
    color: #666;
  }

  .compare-card {
    padding: 16px;
    border-radius: 8px;
    border: 1px solid #e4e7ed;

    h4 {
      margin: 0 0 12px 0;
      font-size: 15px;
    }

    p {
      margin: 6px 0;
      font-size: 13px;
      color: #606266;
    }

    &.before {
      background: #f5f7fa;
    }

    &.after {
      background: #f0f9eb;
    }
  }

  .compare-arrow {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #409eff;
  }

  .compare-result {
    h4 {
      margin: 0 0 12px 0;
      font-size: 15px;
    }

    .improvement {
      color: #67c23a;
      font-size: 15px;
    }
  }
}
</style>
