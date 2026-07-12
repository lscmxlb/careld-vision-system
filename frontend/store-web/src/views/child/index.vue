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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="childCode" label="档案编号" width="140" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="age" label="年龄" width="80" />
        <el-table-column prop="phone" label="家长手机" width="130" />
        <el-table-column prop="eyeCondition" label="视力状况" />
        <el-table-column prop="auditStatus" label="审核状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getAuditStatusType(row.auditStatus)">
              {{ getAuditStatusText(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" @click="handleView(row)">查看</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" />
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker v-model="formData.birthDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="0">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="视力状况" prop="eyeCondition">
          <el-select v-model="formData.eyeCondition">
            <el-option label="正常" value="正常" />
            <el-option label="轻度近视" value="轻度近视" />
            <el-option label="中度近视" value="中度近视" />
            <el-option label="高度近视" value="高度近视" />
            <el-option label="远视" value="远视" />
            <el-option label="散光" value="散光" />
          </el-select>
        </el-form-item>
        <el-form-item label="病史">
          <el-input v-model="formData.medicalHistory" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="过敏信息">
          <el-input v-model="formData.allergyInfo" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="家族病史">
          <el-input v-model="formData.familyHistory" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreChild' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Child } from '@/types'
import { childApi } from '@/api'
import { useUserStore } from '@/stores/user'

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
  auditStatus: undefined as number | undefined
})
const pagination = reactive({ page: 1, size: 10, total: 0 })

const formData = reactive({
  name: '',
  phone: '',
  birthDate: '',
  gender: 1,
  eyeCondition: '',
  medicalHistory: '',
  allergyInfo: '',
  familyHistory: ''
})

const formRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  eyeCondition: [{ required: true, message: '请选择视力状况', trigger: 'change' }]
}

const getAuditStatusType = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getAuditStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已驳回' }
  return map[status] || '未知'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await childApi.getChildList({
      storeId: userStore.storeId,
      keyword: queryForm.keyword || undefined,
      auditStatus: queryForm.auditStatus,
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
  queryForm.keyword = ''
  queryForm.auditStatus = undefined
  pagination.page = 1
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新建档案'
  Object.assign(formData, {
    name: '', phone: '', birthDate: '', gender: 1,
    eyeCondition: '', medicalHistory: '', allergyInfo: '', familyHistory: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: Child) => {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑档案'
  Object.assign(formData, {
    name: row.name,
    phone: row.phone,
    birthDate: row.birthDate,
    gender: row.gender,
    eyeCondition: row.eyeCondition,
    medicalHistory: row.medicalHistory || '',
    allergyInfo: row.allergyInfo || '',
    familyHistory: row.familyHistory || ''
  })
  dialogVisible.value = true
}

const handleView = (row: Child) => {
  ElMessage.info(`查看档案: ${row.name}`)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const data = {
      name: formData.name,
      phone: formData.phone,
      birthDate: formData.birthDate,
      gender: formData.gender,
      eyeCondition: formData.eyeCondition,
      medicalHistory: formData.medicalHistory || undefined,
      allergyInfo: formData.allergyInfo || undefined,
      familyHistory: formData.familyHistory || undefined
    }
    if (isEdit.value && editingId.value) {
      await childApi.updateChild(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await childApi.createChild(data)
      ElMessage.success('提交审核成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.child-page {
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
}
</style>
