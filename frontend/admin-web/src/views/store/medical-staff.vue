<template>
  <div class="medical-staff-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>医务人员管理</span>
          <el-button
            v-permission="'store:staff:create'"
            type="primary"
            @click="handleAdd"
          >
            <el-icon><Plus /></el-icon>新增医务人员
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline>
        <el-form-item label="所属医院">
          <el-select
            v-model="queryForm.storeId"
            placeholder="全部医院"
            clearable
            filterable
            style="width: 240px;"
            @change="handleStoreChange"
          >
            <el-option v-for="s in enabledStores" :key="s.id" :label="s.storeName" :value="s.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="姓名/手机号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="queryForm.staffRole" placeholder="全部" clearable style="width: 120px;">
            <el-option label="医生" :value="1" />
            <el-option label="医生助理" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="所属医院" min-width="160">
          <template #default="{ row }">{{ storeNameMap[row.storeId] || row.storeId }}</template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="staffRole" label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.staffRole === 1 ? 'primary' : 'info'" size="small">
              {{ getRoleText(row.staffRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号（登录账号）" width="170">
          <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">{{ getGenderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="operation-btns">
              <el-button v-permission="'store:staff:update'" type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                v-permission="'store:staff:update'"
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button v-permission="'store:staff:resetpwd'" type="info" size="small" @click="handleResetPwd(row)">重置密码</el-button>
              <el-button v-permission="'store:staff:delete'" type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </div>
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

    <!-- 新增/编辑弹窗（与医院端同一接口同一张表，数据天然一致） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="所属医院" prop="storeId">
          <el-select v-model="formData.storeId" placeholder="请选择医院" filterable style="width: 100%;">
            <el-option
              v-for="s in stores"
              :key="s.id"
              :label="s.status === 1 ? s.storeName : `${s.storeName}（已禁用）`"
              :value="s.id!"
              :disabled="s.status !== 1"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="formData.name" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" maxlength="11" placeholder="登录账号" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="formData.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="staffRole">
          <el-radio-group v-model="formData.staffRole">
            <el-radio :value="1">医生</el-radio>
            <el-radio :value="2">医生助理</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="登录密码" prop="loginPassword">
          <el-input v-model="formData.loginPassword" type="password" show-password placeholder="留空则使用默认密码" />
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
defineOptions({ name: 'AdminMedicalStaff' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { MedicalStaff, Store } from '@/types'
import { medicalStaffApi, storeApi } from '@/api'

const formRef = ref<FormInstance>()
const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<MedicalStaff[]>([])
const stores = ref<Store[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增医务人员')
const isEdit = ref(false)
const editingId = ref<number | null>(null)

const queryForm = reactive({
  storeId: undefined as number | undefined,
  keyword: '',
  staffRole: undefined as number | undefined,
  status: undefined as number | undefined
})
const pagination = reactive({ page: 1, size: 10, total: 0 })

const formData = reactive({
  storeId: undefined as number | undefined,
  name: '',
  phone: '',
  gender: 0,
  staffRole: 1,
  loginPassword: ''
})

const formRules = {
  storeId: [{ required: true, message: '请选择所属医院', trigger: 'change' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  staffRole: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const storeNameMap = computed(() => {
  const map: Record<number, string> = {}
  stores.value.forEach((s) => { if (s.id != null) map[s.id] = s.storeName })
  return map
})

// 筛选下拉只列启用医院；弹窗需展示禁用医院名称（不可选为调入目标）
const enabledStores = computed(() => stores.value.filter((s) => s.status === 1))

const getGenderText = (gender: number) => ({ 0: '未知', 1: '男', 2: '女' })[gender] || '未知'
const getRoleText = (role: number) => ({ 1: '医生', 2: '医生助理' })[role] || '未知'

// 列表手机号脱敏显示（编辑弹窗仍用完整号码）
const maskPhone = (phone?: string): string => {
  if (!phone || phone.length !== 11) return phone || ''
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

// 创建时间：后端返回 ISO（2026-09-15T11:52:57），展示时把 T 换成空格
const formatDateTime = (value?: string): string => {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 19)
}

const fetchStores = async () => {
  try {
    stores.value = await storeApi.getAllStores({ includeDisabled: true })
  } catch {
    // 错误已在拦截器处理
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await medicalStaffApi.getStaffList({
      storeId: queryForm.storeId,
      keyword: queryForm.keyword || undefined,
      staffRole: queryForm.staffRole,
      status: queryForm.status,
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

const handleStoreChange = () => {
  pagination.page = 1
  fetchData()
}

const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.storeId = undefined
  queryForm.keyword = ''
  queryForm.staffRole = undefined
  queryForm.status = undefined
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
  dialogTitle.value = '新增医务人员'
  Object.assign(formData, {
    storeId: queryForm.storeId,
    name: '', phone: '', gender: 0, staffRole: 1, loginPassword: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row: MedicalStaff) => {
  isEdit.value = true
  editingId.value = row.id ?? null
  dialogTitle.value = '编辑医务人员'
  Object.assign(formData, {
    storeId: row.storeId,
    name: row.name,
    phone: row.phone,
    gender: row.gender,
    staffRole: row.staffRole,
    loginPassword: ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value && editingId.value) {
      await medicalStaffApi.updateStaff(editingId.value, {
        storeId: formData.storeId,
        name: formData.name,
        phone: formData.phone,
        gender: formData.gender,
        staffRole: formData.staffRole
      })
      ElMessage.success('更新成功')
    } else {
      await medicalStaffApi.createStaff({
        storeId: formData.storeId,
        name: formData.name,
        phone: formData.phone,
        gender: formData.gender,
        staffRole: formData.staffRole,
        loginPassword: formData.loginPassword || undefined
      })
      ElMessage.success('新增成功，医务人员可用手机号+密码登录医院端')
    }
    dialogVisible.value = false
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = async (row: MedicalStaff) => {
  try {
    await medicalStaffApi.changeStatus(row.id!, row.status === 1 ? 0 : 1)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    fetchData()
  } catch {
    // 错误已在拦截器处理
  }
}

const handleResetPwd = async (row: MedicalStaff) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入「${row.name}」的新密码，留空则重置为默认密码`,
      '重置密码',
      { inputPlaceholder: '默认登录密码：4009993608', inputPattern: /^.{0,64}$/, cancelButtonText: '取消', confirmButtonText: '确定' }
    )
    await medicalStaffApi.resetPassword(row.id!, value || undefined)
    ElMessage.success('密码已重置')
  } catch {
    // 用户取消或错误已在拦截器处理
  }
}

const handleDelete = async (row: MedicalStaff) => {
  try {
    await ElMessageBox.confirm(`确定删除医务人员「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await medicalStaffApi.deleteStaff(row.id!)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 用户取消或错误已在拦截器处理
  }
}

onMounted(async () => {
  await fetchStores()
  fetchData()
})
</script>

<style scoped lang="scss">
.medical-staff-page {
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

  .operation-btns {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;
  }
}
</style>
