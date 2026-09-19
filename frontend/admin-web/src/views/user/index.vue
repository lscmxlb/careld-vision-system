<template>
  <div class="user-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd" v-permission="'user:create'">
            <el-icon><Plus /></el-icon>新增用户
          </el-button>
        </div>
      </template>

      <!-- 用户类别Tab -->
      <el-radio-group v-model="queryForm.userType" @change="handleTabChange" style="margin-bottom: 16px;">
        <el-radio-button v-for="item in availableUserTypes" :key="item.value" :label="item.label" :value="item.value" />
      </el-radio-group>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="运营中心" v-if="currentUserType === 1">
          <el-select v-model="queryForm.centerId" placeholder="选择运营中心" clearable @change="onCenterChange" style="width: 180px">
            <el-option v-for="item in centerOptions" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理商" v-if="currentUserType === 1">
          <el-select v-model="queryForm.agentId" placeholder="选择代理商" clearable style="width: 180px">
            <el-option v-for="item in agentOptions" :key="item.id" :label="item.agentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属医院" v-if="currentUserType === 1">
          <el-select v-model="queryForm.storeId" placeholder="选择医院" clearable style="width: 180px">
            <el-option v-for="item in storeOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="选择状态" style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
            <el-option label="全部" :value="undefined" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="用户名/姓名/手机号" clearable @keyup.enter="handleSearch" style="width: 200px" />
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
        <el-table-column prop="storeName" label="所属医院">
          <template #default="{ row }">{{ row.storeName || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="queryForm.userType === 3" prop="childCount" label="儿童档案数量" width="110">
          <template #default="{ row }">{{ row.childCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ row.userType === 3 ? maskPhone(row.phone) : row.phone }}</template>
        </el-table-column>
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
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canEditUser(row.userType)" type="primary" size="small" @click="handleEdit(row)" v-permission="'user:update'">编辑</el-button>
            <el-button v-if="canEditUser(row.userType)" type="warning" size="small" @click="handleResetPwd(row)" v-permission="'user:resetPwd'">重置密码</el-button>
            <el-button :type="row.status === 1 ? 'danger' : 'success'" size="small" @click="handleToggleStatus(row)" v-permission="'user:toggleStatus'">
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
            <el-option v-for="item in addableUserTypes" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-form-item label="所属医院" prop="storeId" v-if="[2, 3].includes(formData.userType!)">
          <el-select v-model="formData.storeId" placeholder="选择医院" :style="formData.userType === 3 ? 'width: 100%' : ''">
            <el-option v-for="item in formStoreOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="formData.password" type="password" placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="角色">
          <div v-if="selectedRoleIds.length > 0" style="display: flex; align-items: center; gap: 8px; min-height: 32px;">
            <el-tag v-for="rid in selectedRoleIds" :key="rid" type="info" size="large">
              {{ roleOptions.find(r => r.id === rid)?.roleName || '-' }}
            </el-tag>
          </div>
          <span v-else style="color: #909399; line-height: 32px;">请先选择用户类型</span>
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
import { userApi, storeApi, orgApi, roleApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { User, Store, OpsCenter, Agent, Role } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const userStore = useUserStore()
const currentUserType = computed(() => userStore.userInfo?.userType || 1)

// 用户类型层级（数字越大级别越高）
const userTypeLevel = (type: number): number => {
  const levelMap: Record<number, number> = { 1: 5, 4: 4, 5: 3, 2: 2, 3: 1 }
  return levelMap[type] || 0
}

// 所有用户类型（按级别从高到低）
const ALL_USER_TYPES = [
  { label: '总部', value: 1 },
  { label: '运营中心', value: 4 },
  { label: '代理商', value: 5 },
  { label: '医院维护', value: 2 },
  { label: '家长', value: 3 }
]

// Tab可切换的用户类别：admin显示全部，其他角色仅严格下级
const availableUserTypes = computed(() => {
  if (isAdmin()) return ALL_USER_TYPES
  const currentLevel = userTypeLevel(currentUserType.value)
  return ALL_USER_TYPES.filter(t => userTypeLevel(t.value) < currentLevel)
})

// 新增用户表单可选类型：admin可选全部，其他角色仅严格下级
const addableUserTypes = computed(() => {
  if (isAdmin()) return ALL_USER_TYPES
  const currentLevel = userTypeLevel(currentUserType.value)
  return ALL_USER_TYPES.filter(t => userTypeLevel(t.value) < currentLevel)
})

// 默认查询下级用户类型（首个下级）
const getDefaultUserType = () => {
  return availableUserTypes.value[0]?.value
}

// 判断是否为admin超级管理员
const isAdmin = (): boolean => {
  return userStore.userInfo?.username === 'admin'
}

// 判断当前用户是否可以编辑/重置密码目标用户
const canEditUser = (rowUserType: number): boolean => {
  // 只有admin超级管理员才能对总部人员进行编辑和重置密码
  if (rowUserType === 1) {
    return isAdmin()
  }
  // 非总部用户：仅严格下级可编辑
  return userTypeLevel(rowUserType) < userTypeLevel(currentUserType.value)
}

// 家长列表手机号脱敏显示（编辑弹窗仍用原始值，不脱敏）
const maskPhone = (phone?: string): string => {
  if (!phone || phone.length !== 11) return phone || ''
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

// 用户类型与角色编码的对应关系（选用户类型后自动绑定角色，不可手动改）
const ROLE_CODE_BY_USER_TYPE: Record<number, string> = {
  1: 'super_admin',    // 总部 → 超级管理员
  2: 'hospital_admin',  // 医院维护 → 医院管理员
  3: 'parent',          // 家长 → 家长
  4: 'center_admin',    // 运营中心 → 运营中心管理员
  5: 'agent_admin'      // 代理商 → 代理商管理员
}

// 根据用户类型自动绑定对应角色（只读，不可多选）
const autoAssignRole = () => {
  if (!formData.userType) {
    selectedRoleIds.value = []
    return
  }
  const targetRoleCode = ROLE_CODE_BY_USER_TYPE[formData.userType]
  const matchedRole = roleOptions.value.find(r => r.roleCode === targetRoleCode)
  if (matchedRole) {
    selectedRoleIds.value = [matchedRole.id]
  }
}

// 根据当前用户类型自动设置筛选条件
const applyDefaultFilters = () => {
  const type = currentUserType.value
  if (type === 4) {
    // 运营中心用户：自动设置centerId
    queryForm.centerId = userStore.userInfo?.centerId
  } else if (type === 5) {
    // 代理商用户：自动设置agentId
    queryForm.agentId = userStore.userInfo?.agentId
  } else if (type === 2) {
    // 医院维护用户：自动设置storeId
    queryForm.storeId = userStore.userInfo?.storeId
  }
}

const loading = ref(false)
const tableData = ref<User[]>([])
const centerOptions = ref<OpsCenter[]>([])
const agentOptions = ref<Agent[]>([])
const storeOptions = ref<Store[]>([])
const formAgentOptions = ref<Agent[]>([])
const formStoreOptions = ref<Store[]>([])
const roleOptions = ref<Role[]>([])
const selectedRoleIds = ref<number[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const queryForm = reactive({
  userType: undefined as number | undefined,
  centerId: undefined as number | undefined,
  agentId: undefined as number | undefined,
  storeId: undefined as number | undefined,
  status: 1 as number | undefined,
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
    password: isEdit.value ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
  // 运营中心: 医院维护(2)、运营中心(4)、代理商(5)需要选运营中心
  if (ut && [2, 4, 5].includes(ut)) {
    rules.centerId = [{ required: true, message: '请选择运营中心', trigger: 'change' }]
  }
  // 代理商: 医院维护(2)、代理商(5)需要选代理商
  if (ut && [2, 5].includes(ut)) {
    rules.agentId = [{ required: true, message: '请选择代理商', trigger: 'change' }]
  }
  // 医院: 医院维护(2)和家长(3)需要选医院
  if (ut === 2 || ut === 3) {
    rules.storeId = [{ required: true, message: '请选择医院', trigger: 'change' }]
  }
  return rules
})

const getUserTypeLabel = (type: number) => {
  const map: Record<number, string> = { 1: '总部', 2: '医院维护', 3: '家长', 4: '运营中心', 5: '代理商' }
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
  try { storeOptions.value = await storeApi.getAllStores() } catch (e) { console.error('获取医院列表失败', e) }
}

const onCenterChange = async (val: number | undefined) => {
  queryForm.agentId = undefined
  queryForm.storeId = undefined
  await fetchAgents(val)
}

const onFormUserTypeChange = async () => {
  // 用户类型切换时清空下级选择
  formData.centerId = undefined
  formData.agentId = undefined
  formData.storeId = undefined
  formAgentOptions.value = []
  formStoreOptions.value = []
  // 根据用户类型自动绑定角色
  autoAssignRole()
  // 家长类型：医院列表按当前用户数据权限返回（后端 /stores/all 已按组织链过滤）
  if (formData.userType === 3) {
    try {
      formStoreOptions.value = await storeApi.getAllStores()
    } catch (e) { console.error('获取医院列表失败', e) }
  }
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
      // 只加载启用状态的医院，禁用医院不显示在下拉中
      const res = await storeApi.getStoreList({ agentId: val, status: 1, page: 1, size: 1000 })
      formStoreOptions.value = res.list
    } catch (e) { console.error('获取医院列表失败', e) }
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
      centerId: queryForm.centerId,
      agentId: queryForm.agentId,
      status: queryForm.status,
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

const handleTabChange = () => {
  // Tab切换时重置分页并查询
  pagination.page = 1
  fetchData()
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => {
  // 重置为默认状态
  queryForm.userType = getDefaultUserType()
  queryForm.centerId = undefined
  queryForm.agentId = undefined
  queryForm.storeId = undefined
  queryForm.status = 1
  queryForm.keyword = ''
  applyDefaultFilters()
  pagination.page = 1
  fetchData()
}
const handleSizeChange = (val: number) => { pagination.size = val; fetchData() }
const handleCurrentChange = (val: number) => { pagination.page = val; fetchData() }

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  // 优先使用当前 Tab 选中的用户类型作为默认值；如无权限创建该类型则回退到第一个可创建类型
  const currentTabType = queryForm.userType
  const canCreateCurrent = addableUserTypes.value.some(t => t.value === currentTabType)
  const defaultType = canCreateCurrent ? currentTabType : (addableUserTypes.value[0]?.value || 2)
  Object.assign(formData, { username: '', realName: '', userType: defaultType, centerId: undefined, agentId: undefined, storeId: undefined, phone: '', password: '' })
  formAgentOptions.value = []
  formStoreOptions.value = []
  // 根据默认用户类型自动绑定角色
  autoAssignRole()
  currentId.value = null
  dialogVisible.value = true
}

const handleEdit = async (row: User) => {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  currentId.value = row.id
  Object.assign(formData, row)
  // 根据用户类型自动绑定角色（保证角色与用户类型一致）
  autoAssignRole()
  // 保存原始值（onFormCenterChange/onFormAgentChange 会清空下级选项）
  const savedCenterId = row.centerId
  const savedAgentId = row.agentId
  const savedStoreId = row.storeId
  if (row.userType === 3) {
    // 家长：直接加载医院列表（按当前用户数据权限过滤，只含启用医院）
    try {
      formStoreOptions.value = await storeApi.getAllStores()
    } catch { formStoreOptions.value = [] }
    // 回显兼容：若已绑定的医院被禁用不在选项中，追加临时选项保证回显
    if (savedStoreId && !formStoreOptions.value.some(s => s.id === savedStoreId) && row.storeName) {
      formStoreOptions.value = [...formStoreOptions.value, { id: savedStoreId, storeName: row.storeName } as Store]
    }
    formData.storeId = savedStoreId
  } else if (savedCenterId) {
    // 先加载代理商列表（根据运营中心）
    formAgentOptions.value = await orgApi.getAllAgents(savedCenterId)
    // 再加载医院列表（根据代理商，只含启用医院）
    if (savedAgentId) {
      try {
        const res = await storeApi.getStoreList({ agentId: savedAgentId, status: 1, page: 1, size: 1000 })
        formStoreOptions.value = res.list
        // 回显兼容：若已绑定的医院被禁用不在选项中，追加临时选项保证回显
        if (savedStoreId && !formStoreOptions.value.some(s => s.id === savedStoreId) && row.storeName) {
          formStoreOptions.value = [...formStoreOptions.value, { id: savedStoreId, storeName: row.storeName } as Store]
        }
      } catch { formStoreOptions.value = [] }
    } else {
      formStoreOptions.value = []
    }
    // 恢复原始值
    formData.centerId = savedCenterId
    formData.agentId = savedAgentId
    formData.storeId = savedStoreId
  } else {
    formAgentOptions.value = []
    formStoreOptions.value = []
  }
  // 加载用户已有角色
  try {
    const roles = await roleApi.getUserRoles(row.id)
    selectedRoleIds.value = roles.map((r: any) => r.id)
  } catch { selectedRoleIds.value = [] }
  dialogVisible.value = true
}

const handleResetPwd = async (row: User) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入「${row.realName}」的新密码，留空则重置为默认密码`,
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '默认登录密码：4009993608',
        inputValidator: (val) => {
          if (val && val.length < 6) return '密码长度不能少于6位'
          return true
        }
      }
    )
    await userApi.resetPassword(row.id, value || '4009993608')
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
          // 编辑时不传password字段
          const { password, ...editData } = formData
          await userApi.updateUser(currentId.value, editData)
          // 分配角色
          await roleApi.assignUserRoles(currentId.value, selectedRoleIds.value)
          ElMessage.success('更新成功')
        } else {
          const newUserId = await userApi.createUser(formData)
          // 分配角色
          if (selectedRoleIds.value.length > 0) {
            await roleApi.assignUserRoles(newUserId, selectedRoleIds.value)
          }
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

const fetchRoles = async () => {
  try { roleOptions.value = await roleApi.getRoleList() } catch (e) { console.error('获取角色列表失败', e) }
}

onMounted(async () => {
  await userStore.fetchUserInfo()
  fetchCenters()
  fetchStores()
  fetchRoles()
  // 设置默认用户类型和筛选条件
  queryForm.userType = getDefaultUserType()
  applyDefaultFilters()
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
