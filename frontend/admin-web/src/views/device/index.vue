<template>
  <div class="device-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增设备
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="门店">
          <el-select v-model="queryForm.storeId" placeholder="选择门店" clearable filterable>
            <el-option v-for="item in storeOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="到期状态">
          <el-select v-model="queryForm.expireStatus" placeholder="选择状态" clearable>
            <el-option label="正常" :value="0" />
            <el-option label="即将到期" :value="1" />
            <el-option label="已到期" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="设备编码/名称/SN" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 设备表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="140" />
        <el-table-column prop="deviceName" label="设备名称" width="130" />
        <el-table-column prop="deviceTypeName" label="设备类型" width="120" />
        <el-table-column prop="deviceSn" label="设备SN" width="140" />
        <el-table-column prop="storeName" label="所属门店" min-width="120" />
        <el-table-column label="在线状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '在线' : '离线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="到期状态" width="100">
          <template #default="{ row }">
            <el-tag :type="expireStatusType(row.expireStatus)">{{ expireStatusLabel(row.expireStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="installDate" label="安装日期" width="120" />
        <el-table-column prop="maintenanceDate" label="维护日期" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" size="small" @click="handleCalibrate(row)">校准</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑设备弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="formData.deviceCode" placeholder="请输入设备编码" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="formData.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
        <el-form-item label="设备类型" prop="deviceTypeId">
          <el-select v-model="formData.deviceTypeId" placeholder="选择设备类型" @change="onTypeChange">
            <el-option v-for="item in deviceTypeOptions" :key="item.id" :label="item.typeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备SN" prop="deviceSn">
          <el-input v-model="formData.deviceSn" placeholder="请手动输入设备SN号" />
        </el-form-item>
        <el-form-item label="所属门店" prop="storeId">
          <el-select v-model="formData.storeId" placeholder="选择门店" filterable>
            <el-option v-for="item in storeOptions" :key="item.id" :label="item.storeName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="维护日期" prop="maintenanceDate">
          <el-date-picker v-model="formData.maintenanceDate" type="date" value-format="YYYY-MM-DD" placeholder="选择维护日期" />
        </el-form-item>
        <el-form-item label="安装日期" prop="installDate">
          <el-date-picker v-model="formData.installDate" type="date" value-format="YYYY-MM-DD" placeholder="选择安装日期" />
        </el-form-item>
        <el-form-item label="预警天数" prop="warningDays">
          <el-input-number v-model="formData.warningDays" :min="1" placeholder="如：30" />
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
defineOptions({ name: 'AdminDevice' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { deviceApi, storeApi, deviceTypeApi } from '@/api'
import type { Device, Store, DeviceType } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const tableData = ref<Device[]>([])
const storeOptions = ref<Store[]>([])
const deviceTypeOptions = ref<DeviceType[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })

const queryForm = reactive({
  storeId: undefined as number | undefined,
  expireStatus: undefined as number | undefined,
  keyword: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增设备')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<Device>>({
  deviceCode: '',
  deviceName: '',
  deviceTypeId: undefined,
  deviceSn: '',
  storeId: undefined,
  maintenanceDate: '',
  installDate: '',
  warningDays: 30
})

const formRules: FormRules = {
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  deviceTypeId: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  deviceSn: [{ required: true, message: '请输入设备SN', trigger: 'blur' }],
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }]
}

const expireStatusLabel = (status?: number) => {
  const map: Record<number, string> = { 0: '正常', 1: '即将到期', 2: '已到期' }
  return map[status ?? 0] || '正常'
}

const expireStatusType = (status?: number) => {
  const map: Record<number, string> = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[status ?? 0] || 'success'
}

const onTypeChange = (val: number | undefined) => {
  // 设备类型变更时不再自动填充使用寿命
}

const fetchStores = async () => {
  try { storeOptions.value = await storeApi.getAllStores() } catch (e) { console.error('获取门店列表失败', e) }
}

const fetchDeviceTypes = async () => {
  try { deviceTypeOptions.value = await deviceTypeApi.getAllDeviceTypes() } catch (e) { console.error('获取设备类型失败', e) }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await deviceApi.getDeviceList({
      storeId: queryForm.storeId,
      keyword: queryForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size
    })
    // 前端过滤到期状态（后端返回expireStatus字段）
    if (queryForm.expireStatus !== undefined) {
      tableData.value = res.list.filter(d => d.expireStatus === queryForm.expireStatus)
      pagination.total = tableData.value.length
    } else {
      tableData.value = res.list
      pagination.total = res.pagination.total
    }
  } catch (error) {
    console.error('获取设备列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.page = 1; fetchData() }
const handleReset = () => {
  queryForm.storeId = undefined
  queryForm.expireStatus = undefined
  queryForm.keyword = ''
  pagination.page = 1
  fetchData()
}
const handleSizeChange = (val: number) => { pagination.size = val; fetchData() }
const handleCurrentChange = (val: number) => { pagination.page = val; fetchData() }

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增设备'
  Object.assign(formData, { deviceCode: '', deviceName: '', deviceTypeId: undefined, deviceSn: '', storeId: undefined, maintenanceDate: '', installDate: '', warningDays: 30 })
  currentId.value = null
  dialogVisible.value = true
}

const handleEdit = (row: Device) => {
  isEdit.value = true
  dialogTitle.value = '编辑设备'
  currentId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: Device) => {
  try {
    await ElMessageBox.confirm(`确定要删除设备"${row.deviceName}"吗？`, '提示', { type: 'warning' })
    await deviceApi.deleteDevice(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* 取消 */ }
}

const handleCalibrate = (row: Device) => {
  ElMessageBox.prompt('请输入校准参数（JSON格式）', '设备校准', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValue: JSON.stringify({ pixelPerMm: 3.78, screenWidthMm: 1210, screenHeightMm: 680 }, null, 2)
  }).then(({ value }) => {
    try {
      const calibrationData = JSON.parse(value)
      deviceApi.updateCalibration(row.id, {
        calibrationStatus: 1,
        calibrationData: { ...calibrationData, calibrationTime: new Date().toISOString(), calibratedBy: '运营管理员' }
      })
      ElMessage.success('校准成功')
      fetchData()
    } catch {
      ElMessage.error('参数格式错误')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value && currentId.value) {
          await deviceApi.updateDevice(currentId.value, formData)
          ElMessage.success('更新成功')
        } else {
          await deviceApi.createDevice(formData)
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
  fetchStores()
  fetchDeviceTypes()
  fetchData()
})
</script>

<style scoped lang="scss">
.device-page {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-form { margin-bottom: 20px; }
  .pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; }
}
</style>
