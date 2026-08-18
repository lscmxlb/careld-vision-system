<template>
  <div class="user-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增用户
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="用户类型">
          <el-select v-model="queryForm.userType" placeholder="选择类型" clearable>
            <el-option label="总部" :value="1" />
            <el-option label="门店维护" :value="2" />
            <el-option label="家长" :value="3" />
            <el-option label="运营中心" :value="4" />
            <el-option label="代理商" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="运营中心">
          <el-select v-model="queryForm.centerId" placeholder="选择运营中心" clearable @change="onCenterChange">
            <el-option v-for="item in centerOptions" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理商">
          <el-select v-model="queryForm.agentId" placeholder="选择代理商" clearable>
            <el-option v-for="item in agentOptions" :key="item.id" :label="item.agentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属门店">
          <el-select v-model="queryForm.storeId" placeholder="选择门店" clearable>
            <el-option v-for="item in storeOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="用户名/姓名/手机号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 用户表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="100" />
        <el-table-column prop="userType" label="用户类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getUserTypeTag(row.userType)">{{ getUserTypeLabel(row.userType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="centerName" label="运营中心" width="140">
          <template #default="{ row }">{{ row.centerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="agentName" label="代理商" width="140">
          <template #default="{ row }">{{ row.agentName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="storeName" label="所属门店">
          <template #default="{ row }">{{ row.storeName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="160">
          <template #default="{ row }">
            {{ row.lastLoginTime ? formatDate(row.lastLoginTime) : '从未' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button :type="row.status === 1 ? 'danger' : 'success'" size="small" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]" :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="formData.userType" placeholder="选择用户类型" @change="onFormUserTypeChange">
            <el-option label="总部" :value="1" />
            <el-option label="门店维护" :value="2" />
            <el-option label="家长" :value="3" />
            <el-option label="运营中心" :value="4" />
            <el-option label="代理商" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="运营中心" prop="centerId" v-if="[2, 4, 5].includes(formData.userType!)">
          <el-select v-model="formData.centerId" placeholder="选择运营中心" @change="onFormCenterChange">
            <el-option v-for="item in centerOptions" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理商" prop="agentId" v-if="[2, 5].includes(formData.userType!)">
          <el-select v-model="formData.agentId" placeholder="选择代理商" @change="onFormAgentChange">
            <el-option v-for="item in formAgentOptions" :key="item.id" :label="item.agentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属门店" prop="storeId" v-if="formData.userType === 2">
          <el-select v-model="formData.storeId" placeholder="选择门店">
            <el-option v-for="item in formStoreOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="formData.password" type="password" placeholder="请输入初始密码" />
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
defineOptions({ name: 'AdminUser' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { userApi, storeApi, orgApi } from '@/api'
import type { User, Store, OpsCenter, Agent } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const tableData = ref<User[]>([])
const centerOptions = ref<OpsCenter[]>([])
const agentOptions = ref<Agent[]>([])
const storeOptions = ref<Store[]>([])
const formAgentOptions = ref<Agent[]>([])
const formStoreOptions = ref<Store[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const queryForm = reactive({
  userType: undefined as number | undefined,
  centerId: undefined as number | undefined,
  agentId: undefined as number | undefined,
  storeId: undefined as number | undefined,
  keyword: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<User> & { password?: string }>({
  username: '',
  realName: '',
  userType: 2,
  centerId: undefined,
  agentId: undefined,
  storeId: undefined,
  phone: '',
  password: ''
})

const formRules = computed<FormRules>(() => {
  const ut = formData.userType
  const rules: FormRules = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
    userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }],
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
  // 运营中心: 门店维护(2)、运营中心(4)、代理商(5)需要选运营中心
  if (ut && [2, 4, 5].includes(ut)) {
    rules.centerId = [{ required: true, message: '请选择运营中心', trigger: 'change' }]
  }
  // 代理商: 门店维护(2)、代理商(5)需要选代理商
  if (ut && [2, 5].includes(ut)) {
    rules.agentId = [{ required: true, message: '请选择代理商', trigger: 'change' }]
  }
  // 门店: 只有门店维护(2)需要选门店
  if (ut === 2) {
    rules.storeId = [{ required: true, message: '请选择门店', trigger: 'change' }]
  }
  return rules
})

const getUserTypeLabel = (type: number) => {
  const map: Record<number, string> = { 1: '总部', 2: '门店维护', 3: '家长', 4: '运营中心', 5: '代理商' }
  return map[type] || '未知'
}

const getUserTypeTag = (type: number) => {
  const map: Record<number, string> = { 1: 'primary', 2: 'success', 3: 'info', 4: 'warning', 5: 'danger' }
  return map[type] || 'info'
}

const fetchCenters = async () => {
  try { centerOptions.value = await orgApi.getAllCenters() } catch (e) { console.error('获取运营中心失败', e) }
}

const fetchAgents = async (centerId?: number) => {
  try { agentOptions.value = await orgApi.getAllAgents(centerId) } catch (e) { console.error('获取代理商失败', e) }
}

const fetchStores = async () => {
  try { storeOptions.value = await storeApi.getAllStores() } catch (e) { console.error('获取门店列表失败', e) }
}

const onCenterChange = async (val: number | undefined) => {
  queryForm.agentId = undefined
  queryForm.storeId = undefined
  await fetchAgents(val)
}

const onFormUserTypeChange = () => {
  // 用户类型切换时清空下级选择
  formData.centerId = undefined
  formData.agentId = undefined
  formData.storeId = undefined
  formAgentOptions.value = []
  formStoreOptions.value = []
}

const onFormCenterChange = async (val: number | undefined) => {
  formData.agentId = undefined
  formData.storeId = undefined
  formAgentOptions.value = await orgApi.getAllAgents(val)
  formStoreOptions.value = []
}

const onFormAgentChange = async (val: number | undefined) => {
  formData.storeId = undefined
  if (val) {
    try {
      const res = await storeApi.getStoreList({ agentId: val, page: 1, size: 1000 })
      formStoreOptions.value = res.list
    } catch (e) { console.error('获取门店列表失败', e) }
  } else {
    formStoreOptions.value = []
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await userApi.getUserList({
      userType: queryForm.userType,
      storeId: queryForm.storeId,
      keyword: queryForm.keyword,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取用户列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => {
  queryForm.userType = undefined
  queryForm.centerId = undefined
  queryForm.agentId = undefined
  queryForm.storeId = undefined
  queryForm.keyword = ''
  pagination.page = 1
  fetchData()
}
const handleSizeChange = (val: number) => { pagination.size = val; fetchData() }
const handleCurrentChange = (val: number) => { pagination.page = val; fetchData() }

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(formData, { username: '', realName: '', userType: 2, centerId: undefined, agentId: undefined, storeId: undefined, phone: '', password: '' })
  formAgentOptions.value = []
  formStoreOptions.value = []
  currentId.value = null
  dialogVisible.value = true
}

const handleEdit = async (row: User) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  currentId.value = row.id
  Object.assign(formData, row)
  // 保存原始值（onFormCenterChange/onFormAgentChange 会清空下级选项）
  const savedCenterId = row.centerId
  const savedAgentId = row.agentId
  const savedStoreId = row.storeId
  // 先加载代理商列表（根据运营中心）
  if (savedCenterId) {
    formAgentOptions.value = await orgApi.getAllAgents(savedCenterId)
  } else {
    formAgentOptions.value = []
  }
  // 再加载门店列表（根据代理商）
  if (savedAgentId) {
    try {
      const res = await storeApi.getStoreList({ agentId: savedAgentId, page: 1, size: 1000 })
      formStoreOptions.value = res.list
    } catch { formStoreOptions.value = [] }
  } else {
    formStoreOptions.value = []
  }
  // 恢复原始值
  formData.centerId = savedCenterId
  formData.agentId = savedAgentId
  formData.storeId = savedStoreId
  dialogVisible.value = true
}

const handleResetPwd = async (row: User) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputType: 'password',
      inputValidator: (val) => {
        if (!val || val.length < 6) return '密码长度不能少于6位'
        return true
      }
    })
    await userApi.resetPassword(row.id, value)
    ElMessage.success('密码重置成功')
  } catch { /* 取消 */ }
}

const handleToggleStatus = async (row: User) => {
  try {
    await ElMessageBox.confirm(`确定要${row.status === 1 ? '禁用' : '启用'}用户"${row.realName}"吗？`, '提示', { type: 'warning' })
    await userApi.updateUserStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchData()
  } catch { /* 取消 */ }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value && currentId.value) {
          await userApi.updateUser(currentId.value, formData)
          ElMessage.success('更新成功')
        } else {
          await userApi.createUser(formData)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        fetchData()
      } catch (error) {
        console.error('提交失败', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit'
  })
}

onMounted(() => {
  fetchCenters()
  fetchStores()
  fetchData()
})
</script>

<style scoped lang="scss">
.user-page {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-form { margin-bottom: 20px; }
  .pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; }
}
</style>
