<template>
  <div class="store-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>医院管理</span>
          <el-button type="primary" @click="handleAdd" v-permission="'store:list:create'">
            <el-icon><Plus /></el-icon>新增医院
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="运营中心" v-if="isHqUser">
          <el-select v-model="queryForm.centerId" placeholder="选择运营中心" clearable @change="onCenterChange">
            <el-option v-for="item in centerOptions" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="代理商" v-if="isHqUser">
          <el-select v-model="queryForm.agentId" placeholder="选择代理商" clearable @change="handleSearch">
            <el-option v-for="item in agentOptions" :key="item.id" :label="item.agentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="选择状态">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
            <el-option label="全部" :value="undefined" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="医院名称/编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="storeCode" label="医院编码" width="120" />
        <el-table-column prop="storeName" label="医院名称" min-width="150" />
        <el-table-column label="省/市/区" width="180">
          <template #default="{ row }">{{ [row.provinceName, row.cityName, row.districtName].filter(Boolean).join(' / ') || '-' }}</template>
        </el-table-column>
        <el-table-column label="机构性质" width="120">
          <template #default="{ row }">
            <el-tag :type="institutionTypeTag(row.institutionType)">{{ institutionTypeLabel(row.institutionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运营中心" width="120">
          <template #default="{ row }">
            <span v-if="row.centerName">{{ row.centerName }}</span>
            <el-tag v-else type="danger" size="small">未设置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="代理商" width="120">
          <template #default="{ row }">
            <span v-if="row.agentName">{{ row.agentName }}</span>
            <el-tag v-else type="danger" size="small">未设置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="业务负责人" width="100">
          <template #default="{ row }">{{ getAgentContactName(row.agentId) }}</template>
        </el-table-column>
        <el-table-column prop="joinDate" label="加盟时间" width="120" />
        <el-table-column prop="bedCount" label="床位数" width="80" />
        <el-table-column prop="deviceCount" label="设备数" width="80" />
        <el-table-column prop="staffCount" label="员工数" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)" v-permission="'store:list:update'">编辑</el-button>
            <el-button type="success" size="small" @click="handleViewDevices(row)">设备明细</el-button>
            <el-button :type="row.status === 1 ? 'danger' : 'success'" size="small" @click="handleToggleStatus(row)" v-permission="'store:list:update'">
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="医院编码" prop="storeCode">
          <el-input v-model="formData.storeCode" placeholder="请输入医院编码" />
        </el-form-item>
        <el-form-item label="所属运营中心" prop="centerId">
          <el-select v-model="formData.centerId" placeholder="选择运营中心" @change="onFormCenterChange">
            <el-option v-for="item in centerOptions" :key="item.id" :label="item.centerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属代理商" prop="agentId">
          <el-select v-model="formData.agentId" placeholder="选择代理商" filterable>
            <el-option v-for="item in formAgentOptions" :key="item.id" :label="item.agentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="医院名称" prop="storeName">
          <el-input v-model="formData.storeName" placeholder="请输入医院名称" />
        </el-form-item>
        <el-form-item label="所在地区" prop="regionCodes">
          <el-cascader
            v-model="formData.regionCodes"
            :options="regionData"
            placeholder="选择省/市/区"
            clearable
            style="width: 100%"
            @change="onRegionChange"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="加盟时间" prop="joinDate">
          <el-date-picker v-model="formData.joinDate" type="date" value-format="YYYY-MM-DD" placeholder="选择加盟时间" />
        </el-form-item>
        <el-form-item label="床位数量" prop="bedCount">
          <el-input-number v-model="formData.bedCount" :min="0" placeholder="请输入床位数量" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="机构性质" prop="institutionType">
          <el-radio-group v-model="formData.institutionType">
            <el-radio :label="1">公立医疗机构</el-radio>
            <el-radio :label="2">民营医疗机构</el-radio>
            <el-radio :label="3">其他</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设备明细弹窗 -->
    <el-dialog v-model="deviceDialogVisible" :title="`${currentStoreName} - 设备明细`" width="800px" destroy-on-close>
      <el-table :data="storeDevices" v-loading="deviceLoading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="140" />
        <el-table-column prop="deviceName" label="设备名称" width="140" />
        <el-table-column prop="deviceTypeName" label="设备类型" width="120" />
        <el-table-column prop="deviceSn" label="设备SN" width="140" />
        <el-table-column prop="installDate" label="安装日期" width="120" />
        <el-table-column prop="maintenanceDate" label="维护日期" width="120" />
        <el-table-column label="到期状态" width="100">
          <template #default="{ row }">
            <el-tag :type="expireStatusType(row.expireStatus)">{{ expireStatusLabel(row.expireStatus) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminStoreList' })
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { storeApi, orgApi, deviceApi } from '@/api'
import { useUserStore } from '@/stores/user'
import type { Store, StoreQuery, OpsCenter, Agent, Device } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'
import { regionData } from 'element-china-area-data'

const loading = ref(false)
const userStore = useUserStore()
const isHqUser = computed(() => userStore.userInfo?.userType === 1)
const tableData = ref<Store[]>([])
const centerOptions = ref<OpsCenter[]>([])
const agentOptions = ref<Agent[]>([])
const allAgents = ref<Agent[]>([])
const formAgentOptions = ref<Agent[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const queryForm = reactive<StoreQuery & { centerId?: number }>({
  page: 1,
  size: 10,
  status: 1,
  agentId: undefined,
  centerId: undefined,
  keyword: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增医院')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<Store> & { regionCodes?: string[]; centerId?: number }>({
  storeCode: '',
  centerId: undefined,
  storeName: '',
  agentId: undefined,
  provinceCode: '',
  provinceName: '',
  cityCode: '',
  cityName: '',
  districtCode: '',
  districtName: '',
  address: '',
  contactName: '',
  contactPhone: '',
  joinDate: '',
  bedCount: 0,
  institutionType: undefined,
  regionCodes: []
})

const formRules: FormRules = {
  storeCode: [{ required: true, message: '请输入医院编码', trigger: 'blur' }],
  centerId: [{ required: true, message: '请选择运营中心', trigger: 'change' }],
  storeName: [{ required: true, message: '请输入医院名称', trigger: 'blur' }],
  regionCodes: [{ required: true, message: '请选择所在地区', trigger: 'change' }],
  agentId: [{ required: true, message: '请选择代理商', trigger: 'change' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  institutionType: [{ required: true, message: '请选择机构性质', trigger: 'change' }]
}

const institutionTypeLabel = (type?: number) => {
  const map: Record<number, string> = { 1: '公立医疗机构', 2: '民营医疗机构', 3: '其他' }
  return map[type ?? 0] || '未设置'
}

const institutionTypeTag = (type?: number) => {
  const map: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'info' }
  return map[type ?? 0] || 'danger'
}

const onRegionChange = (codes: string[] | undefined) => {
  if (codes && codes.length === 3) {
    formData.provinceCode = codes[0]
    formData.cityCode = codes[1]
    formData.districtCode = codes[2]
    // 从 regionData 中查找名称
    const province = regionData.find(p => p.value === codes[0])
    formData.provinceName = province?.label || ''
    const city = province?.children?.find((c: any) => c.value === codes[1])
    formData.cityName = city?.label || ''
    const district = city?.children?.find((d: any) => d.value === codes[2])
    formData.districtName = district?.label || ''
  } else {
    formData.provinceCode = ''
    formData.provinceName = ''
    formData.cityCode = ''
    formData.cityName = ''
    formData.districtCode = ''
    formData.districtName = ''
  }
}

// 设备明细
const deviceDialogVisible = ref(false)
const deviceLoading = ref(false)
const storeDevices = ref<Device[]>([])
const currentStoreName = ref('')

const getAgentName = (agentId?: number) => {
  const agent = allAgents.value.find(a => a.id === agentId)
  return agent?.agentName || '-'
}

const getAgentContactName = (agentId?: number) => {
  const agent = allAgents.value.find(a => a.id === agentId)
  return agent?.contactName || '-'
}

const expireStatusLabel = (status?: number) => {
  const map: Record<number, string> = { 0: '正常', 1: '即将到期', 2: '已到期' }
  return map[status ?? 0] || '正常'
}

const expireStatusType = (status?: number) => {
  const map: Record<number, string> = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[status ?? 0] || 'success'
}

const fetchCenters = async () => {
  try { centerOptions.value = await orgApi.getAllCenters() } catch (e) { console.error('获取运营中心失败', e) }
}

const fetchAllAgents = async () => {
  try { allAgents.value = await orgApi.getAllAgents() } catch (e) { console.error('获取代理商失败', e) }
}

const onCenterChange = async (val: number | undefined) => {
  queryForm.agentId = undefined
  if (val) {
    agentOptions.value = await orgApi.getAllAgents(val)
  } else {
    agentOptions.value = []
  }
  handleSearch()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await storeApi.getStoreList({
      ...queryForm,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取医院列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => {
  queryForm.status = 1
  queryForm.agentId = undefined
  queryForm.centerId = undefined
  queryForm.keyword = ''
  agentOptions.value = []
  pagination.page = 1
  fetchData()
}
const handleSizeChange = (val: number) => { pagination.size = val; fetchData() }
const handleCurrentChange = (val: number) => { pagination.page = val; fetchData() }

const onFormCenterChange = async (val: number | undefined) => {
  formData.agentId = undefined
  if (val) {
    formAgentOptions.value = await orgApi.getAllAgents(val)
  } else {
    formAgentOptions.value = []
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增医院'
  Object.assign(formData, { storeCode: '', centerId: undefined, storeName: '', agentId: undefined, provinceCode: '', provinceName: '', cityCode: '', cityName: '', districtCode: '', districtName: '', address: '', contactName: '', contactPhone: '', joinDate: '', bedCount: 0, institutionType: undefined, regionCodes: [] })
  formAgentOptions.value = []
  currentId.value = null
  dialogVisible.value = true
}

/**
 * 根据存储的省市区信息从 regionData 中查找匹配的级联编码
 * 兼容多种编码格式：2位/4位/6位混合，以及名称不完全匹配的情况
 */
const findRegionCodes = (row: Store): string[] => {
  // 策略1：名称匹配（处理直辖市"市辖区"问题）
  if (row.provinceName) {
    const province = regionData.find(p => p.label === row.provinceName)
    if (province) {
      // 尝试精确匹配城市名
      let city = province.children?.find((c: any) => c.label === row.cityName)
      // 直辖市处理：城市名与省名相同但cascader用的是"市辖区"
      if (!city && row.cityName === row.provinceName) {
        city = province.children?.find((c: any) => c.label === '市辖区')
      }
      if (city) {
        const district = city.children?.find((d: any) => d.label === row.districtName)
        if (district) return [province.value, city.value, district.value]
        return [province.value, city.value]
      }
      return [province.value]
    }
  }

  // 策略2：编码截断匹配
  // cascader格式：省=2位, 市=4位, 区=6位
  const truncCode = (code: string, len: number) => {
    if (!code) return ''
    const s = String(code)
    return s.length > len ? s.substring(0, len) : s
  }
  const pCode = truncCode(String(row.provinceCode || ''), 2)
  const cCode = truncCode(String(row.cityCode || ''), 4)
  const dCode = truncCode(String(row.districtCode || ''), 6)

  if (pCode) {
    const province = regionData.find(p => p.value === pCode)
    if (province && cCode) {
      const city = province.children?.find((c: any) => c.value === cCode)
      if (city) {
        const district = city.children?.find((d: any) => d.value === dCode)
        if (district) return [province.value, city.value, district.value]
        return [province.value, city.value]
      }
      return [province.value]
    }
  }

  return []
}

const handleEdit = async (row: Store) => {
  isEdit.value = true
  dialogTitle.value = '编辑医院'
  currentId.value = row.id
  Object.assign(formData, row)
  // 保存原始值
  const savedCenterId = row.centerId ? Number(row.centerId) : undefined
  const savedAgentId = row.agentId
  // 根据运营中心加载代理商列表
  if (savedCenterId) {
    formAgentOptions.value = await orgApi.getAllAgents(savedCenterId)
  } else {
    formAgentOptions.value = []
  }
  formData.centerId = savedCenterId
  formData.agentId = savedAgentId
  // 先清空再设置，确保级联选择器正确回显
  formData.regionCodes = []
  dialogVisible.value = true
  nextTick(() => {
    formData.regionCodes = findRegionCodes(row)
  })
}

const handleViewDevices = async (row: Store) => {
  currentStoreName.value = row.storeName
  deviceDialogVisible.value = true
  deviceLoading.value = true
  try {
    const res = await deviceApi.getDeviceList({ storeId: row.id, page: 1, size: 100 })
    storeDevices.value = res.list
  } catch (error) {
    console.error('获取设备列表失败', error)
  } finally {
    deviceLoading.value = false
  }
}

const handleToggleStatus = async (row: Store) => {
  try {
    await ElMessageBox.confirm(`确定要${row.status === 1 ? '禁用' : '启用'}医院"${row.storeName}"吗？`, '提示', { type: 'warning' })
    await storeApi.updateStoreStatus(row.id, row.status === 1 ? 0 : 1)
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
          await storeApi.updateStore(currentId.value, formData)
          ElMessage.success('更新成功')
        } else {
          await storeApi.createStore(formData)
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

onMounted(() => {
  fetchCenters()
  fetchAllAgents()
  fetchData()
})
</script>

<style scoped lang="scss">
.store-list-page {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-form { margin-bottom: 20px; }
  .pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; }
}
</style>
