<template>
  <div class="audit-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>档案审核工作台</span>
          <el-radio-group v-model="auditStatus" @change="handleStatusChange">
            <el-radio-button :label="0">待审核</el-radio-button>
            <el-radio-button :label="1">已通过</el-radio-button>
            <el-radio-button :label="2">已驳回</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="门店">
          <el-select v-model="queryForm.storeId" placeholder="选择门店" clearable>
            <el-option
              v-for="item in storeOptions"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="儿童姓名/手机号"
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
        <el-table-column prop="childCode" label="档案编号" width="140" />
        <el-table-column prop="name" label="儿童姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            {{ row.gender === 1 ? '男' : '女' }}
          </template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="家长手机号" width="130" />
        <el-table-column prop="storeName" label="所属门店" />
        <el-table-column prop="eyeCondition" label="视力状况" />
        <el-table-column prop="createdAt" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column v-if="auditStatus !== 0" prop="auditedAt" label="审核时间" width="160">
          <template #default="{ row }">
            {{ row.auditedAt ? formatDate(row.auditedAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">查看</el-button>
            <template v-if="auditStatus === 0">
              <el-button type="success" size="small" @click="handleAudit(row, 1)">通过</el-button>
              <el-button type="danger" size="small" @click="handleAudit(row, 2)">驳回</el-button>
            </template>
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
    <el-dialog
      v-model="detailVisible"
      title="档案详情"
      width="700px"
      destroy-on-close
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="档案编号">{{ currentRow?.childCode }}</el-descriptions-item>
        <el-descriptions-item label="所属门店">{{ currentRow?.storeName }}</el-descriptions-item>
        <el-descriptions-item label="儿童姓名">{{ currentRow?.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ currentRow?.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ currentRow?.birthDate }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentRow?.age }}岁</el-descriptions-item>
        <el-descriptions-item label="家长手机号">{{ currentRow?.phone }}</el-descriptions-item>
        <el-descriptions-item label="视力状况">{{ currentRow?.eyeCondition }}</el-descriptions-item>
        <el-descriptions-item label="病史" :span="2">{{ currentRow?.medicalHistory || '无' }}</el-descriptions-item>
        <el-descriptions-item label="过敏信息" :span="2">{{ currentRow?.allergyInfo || '无' }}</el-descriptions-item>
        <el-descriptions-item label="家族病史" :span="2">{{ currentRow?.familyHistory || '无' }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="auditStatus === 0">
        <el-divider />
        <el-form :model="auditForm" label-width="80px">
          <el-form-item label="审核结果">
            <el-radio-group v-model="auditForm.auditStatus">
              <el-radio :label="1">通过</el-radio>
              <el-radio :label="2">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核备注">
            <el-input
              v-model="auditForm.auditRemark"
              type="textarea"
              :rows="3"
              placeholder="请输入审核备注（驳回时必填）"
            />
          </el-form-item>
        </el-form>
      </template>

      <template v-if="auditStatus !== 0">
        <el-divider />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="审核结果">
            <el-tag :type="currentRow?.auditStatus === 1 ? 'success' : 'danger'">
              {{ currentRow?.auditStatus === 1 ? '已通过' : '已驳回' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="审核备注">{{ currentRow?.auditRemark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ currentRow?.auditedBy }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ currentRow?.auditedAt ? formatDate(currentRow?.auditedAt) : '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <template v-if="auditStatus === 0">
          <el-button type="primary" :loading="submitLoading" @click="handleSubmitAudit">提交审核</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminStoreAudit' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { childApi, storeApi } from '@/api'
import type { Child, Store } from '@/types'

// 状态
const auditStatus = ref(0)
const loading = ref(false)
const tableData = ref<Child[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 查询表单
const queryForm = reactive({
  storeId: undefined as number | undefined,
  keyword: ''
})

// 门店选项
const storeOptions = ref<Store[]>([])

// 详情弹窗
const detailVisible = ref(false)
const currentRow = ref<Child | null>(null)
const submitLoading = ref(false)

const auditForm = reactive({
  auditStatus: 1,
  auditRemark: ''
})

// 获取门店列表
const fetchStores = async () => {
  try {
    const res = await storeApi.getAllStores()
    storeOptions.value = res
  } catch (error) {
    console.error('获取门店列表失败', error)
  }
}

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await childApi.getChildList({
      auditStatus: auditStatus.value,
      storeId: queryForm.storeId,
      keyword: queryForm.keyword,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取档案列表失败', error)
  } finally {
    loading.value = false
  }
}

// 状态切换
const handleStatusChange = () => {
  pagination.page = 1
  fetchData()
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryForm.storeId = undefined
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

// 查看详情
const handleView = (row: Child) => {
  currentRow.value = row
  auditForm.auditStatus = 1
  auditForm.auditRemark = ''
  detailVisible.value = true
}

// 快速审核
const handleAudit = async (row: Child, status: number) => {
  if (status === 2) {
    // 驳回需要填写原因
    try {
      const { value } = await ElMessageBox.prompt('请输入驳回原因', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /\S+/,
        inputErrorMessage: '驳回原因不能为空'
      })
      await submitAudit(row.id, status, value)
    } catch {
      // 取消
    }
  } else {
    try {
      await ElMessageBox.confirm('确定要通过该档案吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await submitAudit(row.id, status, '')
    } catch {
      // 取消
    }
  }
}

// 提交审核
const handleSubmitAudit = async () => {
  if (!currentRow.value) return
  if (auditForm.auditStatus === 2 && !auditForm.auditRemark.trim()) {
    ElMessage.warning('驳回时必须填写审核备注')
    return
  }
  await submitAudit(currentRow.value.id, auditForm.auditStatus, auditForm.auditRemark)
}

const submitAudit = async (id: number, status: number, remark: string) => {
  submitLoading.value = true
  try {
    await childApi.auditChild(id, {
      auditStatus: status,
      auditRemark: remark
    })
    ElMessage.success('审核成功')
    detailVisible.value = false
    fetchData()
  } catch (error) {
    console.error('审核失败', error)
  } finally {
    submitLoading.value = false
  }
}

// 格式化日期
const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchStores()
  fetchData()
})
</script>

<style scoped lang="scss">
.audit-page {
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
}
</style>
