<template>
  <div class="department-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>科室管理</span>
          <el-button v-if="isManager" type="primary" @click="handleAdd">
            新增科室
          </el-button>
        </div>
      </template>
      <el-table :data="departmentList" v-loading="loading">
        <el-table-column prop="deptCode" label="科室编码" width="120" />
        <el-table-column prop="deptName" label="科室名称" />
        <el-table-column prop="deptType" label="科室类型">
          <template #default="{ row }">
            <el-tag>{{ getDeptTypeText(row.deptType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-if="isManager"
              :model-value="row.status === 1"
              @change="(val: boolean) => handleStatusChange(row, val)"
              inline-prompt
              active-text="启用"
              inactive-text="禁用"
            />
            <el-tag v-else :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" v-if="isManager">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="科室编码" prop="deptCode">
          <el-input v-model="form.deptCode" placeholder="请输入科室编码" />
        </el-form-item>
        <el-form-item label="科室名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入科室名称" />
        </el-form-item>
        <el-form-item label="科室类型" prop="deptType">
          <el-select v-model="form.deptType" placeholder="请选择科室类型">
            <el-option label="门诊" :value="1" />
            <el-option label="养护" :value="2" />
            <el-option label="检测" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreDepartment' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { departmentApi } from '@/api'
import type { Department } from '@/types'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增科室')
const isManager = computed(() => userStore.userInfo?.userType === 2) // 店长

const departmentList = ref<Department[]>([])

const form = reactive({
  id: null as number | null,
  deptCode: '',
  deptName: '',
  deptType: 1,
  sortOrder: 0,
  status: 1
})

const rules = {
  deptCode: [{ required: true, message: '请输入科室编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
  deptType: [{ required: true, message: '请选择科室类型', trigger: 'change' }]
}

const getDeptTypeText = (type: number) => {
  const map: Record<number, string> = {
    1: '门诊',
    2: '养护',
    3: '检测',
    4: '其他'
  }
  return map[type] || '未知'
}

const loadDepartments = async () => {
  if (!userStore.storeId) return
  loading.value = true
  try {
    departmentList.value = await departmentApi.getDepartmentList(userStore.storeId)
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增科室'
  form.id = null
  form.deptCode = ''
  form.deptName = ''
  form.deptType = 1
  form.sortOrder = 0
  form.status = 1
  dialogVisible.value = true
}

const handleEdit = (row: Department) => {
  dialogTitle.value = '编辑科室'
  Object.assign(form, {
    id: row.id,
    deptCode: row.deptCode,
    deptName: row.deptName,
    deptType: row.deptType,
    sortOrder: row.sortOrder,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = (row: Department) => {
  ElMessageBox.confirm(`确定删除科室"${row.deptName}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await departmentApi.deleteDepartment(row.id)
      ElMessage.success('删除成功')
      loadDepartments()
    } catch {
      // 错误已在拦截器处理
    }
  }).catch(() => {})
}

const handleStatusChange = async (row: Department, val: boolean) => {
  try {
    await departmentApi.updateDepartmentStatus(row.id, val ? 1 : 0)
    ElMessage.success('状态更新成功')
    loadDepartments()
  } catch {
    // 错误已在拦截器处理
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const data: Partial<Department> = {
      storeId: userStore.storeId,
      deptCode: form.deptCode,
      deptName: form.deptName,
      deptType: form.deptType,
      sortOrder: form.sortOrder
    }
    if (form.id) {
      await departmentApi.updateDepartment(form.id, data)
      ElMessage.success('修改成功')
    } else {
      await departmentApi.createDepartment(data)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadDepartments()
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadDepartments()
})
</script>

<style scoped>
.department-page {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
