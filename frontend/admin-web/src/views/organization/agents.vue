<template>
  <div class="agents-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>代理商管理</span>
          <el-button type="primary" @click="handleAdd" v-permission="'organization:agent:create'">
            <el-icon><Plus /></el-icon>新增代理商
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="运营中心">
          <el-select v-model="queryForm.centerId" placeholder="选择运营中心" clearable @change="handleSearch">
            <el-option v-for="item in centerOptions" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="代理商名称/编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="agentCode" label="代理商编码" width="120" />
        <el-table-column prop="agentName" label="代理商名称" min-width="150" />
        <el-table-column label="所属运营中心" width="140">
          <template #default="{ row }">{{ row.centerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="storeCount" label="医院数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success" class="clickable-tag" @click="handleShowStores(row)">{{ row.storeCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="区域" width="120" />
        <el-table-column prop="contactName" label="负责人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="contactEmail" label="邮箱" width="180" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)" v-permission="'organization:agent:update'">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)" v-permission="'organization:agent:delete'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]" :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handleCurrentChange" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="运营中心" prop="centerId">
          <el-select v-model="formData.centerId" placeholder="选择运营中心">
            <el-option v-for="item in centerOptions" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理商编码" prop="agentCode">
          <el-input v-model="formData.agentCode" placeholder="请输入代理商编码" />
        </el-form-item>
        <el-form-item label="代理商名称" prop="agentName">
          <el-input v-model="formData.agentName" placeholder="请输入代理商名称" />
        </el-form-item>
        <el-form-item label="区域" prop="region">
          <el-input v-model="formData.region" placeholder="如：北京、上海" />
        </el-form-item>
        <el-form-item label="负责人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入负责人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="contactEmail">
          <el-input v-model="formData.contactEmail" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 医院明细弹窗 -->
    <el-dialog v-model="storeDialog.visible" :title="storeDialog.title" width="750px" destroy-on-close>
      <el-table :data="storeDialog.data" v-loading="storeDialog.loading" stripe max-height="400">
        <el-table-column prop="storeCode" label="医院编码" width="120" />
        <el-table-column prop="storeName" label="医院名称" min-width="150" />
        <el-table-column label="省/市/区" width="180">
          <template #default="{ row }">{{ [row.provinceName, row.cityName, row.districtName].filter(Boolean).join(' / ') || '-' }}</template>
        </el-table-column>
        <el-table-column label="机构性质" width="120">
          <template #default="{ row }">
            <el-tag :type="row.institutionType === 1 ? 'success' : row.institutionType === 2 ? 'warning' : 'info'">
              {{ row.institutionType === 1 ? '公立医疗机构' : row.institutionType === 2 ? '民营医疗机构' : '其他' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminAgents' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { orgApi } from '@/api'
import { storeApi } from '@/api'
import type { Agent, OpsCenter, Store } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const tableData = ref<Agent[]>([])
const centerOptions = ref<OpsCenter[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const queryForm = reactive({ centerId: undefined as number | undefined, keyword: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('新增代理商')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<Agent>>({
  agentCode: '',
  agentName: '',
  centerId: undefined,
  region: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  status: 1
})

const formRules: FormRules = {
  centerId: [{ required: true, message: '请选择运营中心', trigger: 'change' }],
  agentCode: [{ required: true, message: '请输入代理商编码', trigger: 'blur' }],
  agentName: [{ required: true, message: '请输入代理商名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const fetchCenters = async () => {
  try {
    centerOptions.value = await orgApi.getAllCenters()
  } catch (error) {
    console.error('获取运营中心列表失败', error)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await orgApi.getAgentList({
      centerId: queryForm.centerId,
      keyword: queryForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取代理商列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => { queryForm.centerId = undefined; queryForm.keyword = ''; pagination.page = 1; fetchData() }
const handleSizeChange = (val: number) => { pagination.size = val; fetchData() }
const handleCurrentChange = (val: number) => { pagination.page = val; fetchData() }

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增代理商'
  Object.assign(formData, { agentCode: '', agentName: '', centerId: undefined, region: '', contactName: '', contactPhone: '', contactEmail: '', status: 1 })
  currentId.value = null
  dialogVisible.value = true
}

const handleEdit = (row: Agent) => {
  isEdit.value = true
  dialogTitle.value = '编辑代理商'
  currentId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: Agent) => {
  try {
    await ElMessageBox.confirm(`确定要删除代理商"${row.agentName}"吗？`, '提示', { type: 'warning' })
    await orgApi.deleteAgent(row.id)
    ElMessage.success('删除成功')
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
          await orgApi.updateAgent(currentId.value, formData)
          ElMessage.success('更新成功')
        } else {
          await orgApi.createAgent(formData)
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

onMounted(() => { fetchCenters(); fetchData() })

// ===== 医院明细弹窗 =====
const storeDialog = reactive({
  visible: false,
  title: '',
  data: [] as Store[],
  loading: false
})

const handleShowStores = async (row: Agent) => {
  storeDialog.visible = true
  storeDialog.title = `${row.agentName} - 医院列表`
  storeDialog.loading = true
  storeDialog.data = []
  try {
    const res = await storeApi.getStoreList({ agentId: row.id, page: 1, size: 100 })
    storeDialog.data = res.list
  } catch (error) {
    console.error('获取医院列表失败', error)
  } finally {
    storeDialog.loading = false
  }
}
</script>

<style scoped lang="scss">
.agents-page {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-form { margin-bottom: 20px; }
  .pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; }
  .clickable-tag { cursor: pointer; }
  .clickable-tag:hover { opacity: 0.8; }
}
</style>
