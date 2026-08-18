<template>
  <div class="centers-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>运营中心管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增运营中心
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="中心名称/编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="centerCode" label="中心编码" width="120" />
        <el-table-column prop="centerName" label="中心名称" min-width="150" />
        <el-table-column prop="region" label="覆盖区域" width="120" />
        <el-table-column prop="agentCount" label="代理商数量" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="primary" class="clickable-tag" @click="handleShowAgents(row)">{{ row.agentCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storeCount" label="门店数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="success" class="clickable-tag" @click="handleShowStores(row)">{{ row.storeCount ?? 0 }}</el-tag>
          </template>
        </el-table-column>
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
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
        <el-form-item label="中心编码" prop="centerCode">
          <el-input v-model="formData.centerCode" placeholder="请输入中心编码" />
        </el-form-item>
        <el-form-item label="中心名称" prop="centerName">
          <el-input v-model="formData.centerName" placeholder="请输入中心名称" />
        </el-form-item>
        <el-form-item label="覆盖区域" prop="region">
          <el-input v-model="formData.region" placeholder="如：华北、华东" />
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

    <!-- 代理商明细弹窗 -->
    <el-dialog v-model="detailDialog.visible" :title="detailDialog.title" width="700px" destroy-on-close>
      <el-table :data="detailDialog.data" v-loading="detailDialog.loading" stripe max-height="400">
        <template v-if="detailDialog.type === 'agents'">
          <el-table-column prop="agentCode" label="代理商编码" width="120" />
          <el-table-column prop="agentName" label="代理商名称" min-width="150" />
          <el-table-column prop="region" label="区域" width="100" />
          <el-table-column prop="contactName" label="负责人" width="100" />
          <el-table-column prop="contactPhone" label="联系电话" width="130" />
          <el-table-column prop="storeCount" label="门店数" width="80" align="center" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
        </template>
        <template v-else>
          <el-table-column prop="storeCode" label="门店编码" width="120" />
          <el-table-column prop="storeName" label="门店名称" min-width="150" />
          <el-table-column label="省/市/区" width="180">
            <template #default="{ row }">{{ [row.provinceName, row.cityName, row.districtName].filter(Boolean).join(' / ') || '-' }}</template>
          </el-table-column>
          <el-table-column prop="agentName" label="所属代理商" width="130" />
          <el-table-column prop="contactName" label="联系人" width="100" />
          <el-table-column prop="contactPhone" label="联系电话" width="130" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
        </template>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminOpsCenters' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { orgApi } from '@/api'
import { storeApi } from '@/api'
import type { OpsCenter, Agent, Store } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const tableData = ref<OpsCenter[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const queryForm = reactive({ keyword: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('新增运营中心')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<OpsCenter>>({
  centerCode: '',
  centerName: '',
  hqId: 1,
  region: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  status: 1
})

const formRules: FormRules = {
  centerCode: [{ required: true, message: '请输入中心编码', trigger: 'blur' }],
  centerName: [{ required: true, message: '请输入中心名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await orgApi.getCenterList({
      keyword: queryForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取运营中心列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => { queryForm.keyword = ''; pagination.page = 1; fetchData() }
const handleSizeChange = (val: number) => { pagination.size = val; fetchData() }
const handleCurrentChange = (val: number) => { pagination.page = val; fetchData() }

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增运营中心'
  Object.assign(formData, { centerCode: '', centerName: '', hqId: 1, region: '', contactName: '', contactPhone: '', contactEmail: '', status: 1 })
  currentId.value = null
  dialogVisible.value = true
}

const handleEdit = (row: OpsCenter) => {
  isEdit.value = true
  dialogTitle.value = '编辑运营中心'
  currentId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: OpsCenter) => {
  try {
    await ElMessageBox.confirm(`确定要删除运营中心"${row.centerName}"吗？`, '提示', { type: 'warning' })
    await orgApi.deleteCenter(row.id)
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
          await orgApi.updateCenter(currentId.value, formData)
          ElMessage.success('更新成功')
        } else {
          await orgApi.createCenter(formData)
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

onMounted(() => { fetchData() })

// ===== 明细弹窗 =====
const detailDialog = reactive({
  visible: false,
  title: '',
  type: '' as 'agents' | 'stores',
  data: [] as Agent[] | Store[],
  loading: false
})

const handleShowAgents = async (row: OpsCenter) => {
  detailDialog.visible = true
  detailDialog.title = `${row.centerName} - 代理商列表`
  detailDialog.type = 'agents'
  detailDialog.loading = true
  detailDialog.data = []
  try {
    detailDialog.data = await orgApi.getAllAgents(row.id)
  } catch (error) {
    console.error('获取代理商列表失败', error)
  } finally {
    detailDialog.loading = false
  }
}

const handleShowStores = async (row: OpsCenter) => {
  detailDialog.visible = true
  detailDialog.title = `${row.centerName} - 门店列表`
  detailDialog.type = 'stores'
  detailDialog.loading = true
  detailDialog.data = []
  try {
    // 先获取该运营中心下的所有代理商
    const agents = await orgApi.getAllAgents(row.id)
    const agentIds = agents.map(a => a.id)
    if (agentIds.length === 0) {
      detailDialog.data = []
    } else {
      // 获取所有门店，前端按 agentId 过滤
      const allStores = await storeApi.getAllStores()
      detailDialog.data = allStores.filter(s => s.agentId && agentIds.includes(s.agentId))
    }
  } catch (error) {
    console.error('获取门店列表失败', error)
  } finally {
    detailDialog.loading = false
  }
}
</script>

<style scoped lang="scss">
.centers-page {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-form { margin-bottom: 20px; }
  .pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; }
  .clickable-tag { cursor: pointer; }
  .clickable-tag:hover { opacity: 0.8; }
}
</style>
