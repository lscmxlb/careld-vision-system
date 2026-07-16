<template>
  <div class="device-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>TV设备管理</span>
          <el-button type="primary" @click="handleBind">
            <el-icon><Plus /></el-icon>绑定设备
          </el-button>
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
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="选择状态" clearable>
            <el-option label="在线" :value="1" />
            <el-option label="离线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 设备表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="160" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="storeName" label="所属门店" />
        <el-table-column prop="status" label="在线状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              <el-icon v-if="row.status === 1"><CircleCheck /></el-icon>
              <el-icon v-else><CircleClose /></el-icon>
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="calibrationStatus" label="校准状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.calibrationStatus === 1 ? 'success' : 'warning'">
              {{ row.calibrationStatus === 1 ? '已校准' : '未校准' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最后同步" width="160">
          <template #default="{ row }">
            {{ row.lastSyncTime ? formatDate(row.lastSyncTime) : '从未' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)">详情</el-button>
            <el-button type="warning" size="small" @click="handleCalibrate(row)">校准</el-button>
            <el-button type="danger" size="small" @click="handleUnbind(row)">解绑</el-button>
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

    <!-- 绑定设备弹窗 -->
    <el-dialog v-model="bindVisible" title="绑定设备" width="500px">
      <el-form ref="bindFormRef" :model="bindForm" :rules="bindRules" label-width="100px">
        <el-form-item label="选择门店" prop="storeId">
          <el-select v-model="bindForm.storeId" placeholder="选择门店">
            <el-option
              v-for="item in storeOptions"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="bindForm.deviceCode" placeholder="请输入TV设备编码" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="bindForm.deviceName" placeholder="请输入设备名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleBindSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 设备详情弹窗 -->
    <el-dialog v-model="detailVisible" title="设备详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备编码">{{ currentDevice?.deviceCode }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ currentDevice?.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="所属门店">{{ currentDevice?.storeName }}</el-descriptions-item>
        <el-descriptions-item label="在线状态">
          <el-tag :type="currentDevice?.status === 1 ? 'success' : 'danger'">
            {{ currentDevice?.status === 1 ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="校准状态">
          <el-tag :type="currentDevice?.calibrationStatus === 1 ? 'success' : 'warning'">
            {{ currentDevice?.calibrationStatus === 1 ? '已校准' : '未校准' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后同步">
          {{ currentDevice?.lastSyncTime ? formatDate(currentDevice?.lastSyncTime) : '从未' }}
        </el-descriptions-item>
      </el-descriptions>
      <template v-if="currentDevice?.calibrationData">
        <el-divider />
        <h4>校准数据</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="像素密度">{{ currentDevice.calibrationData.pixelPerMm }} px/mm</el-descriptions-item>
          <el-descriptions-item label="屏幕宽度">{{ currentDevice.calibrationData.screenWidthMm }} mm</el-descriptions-item>
          <el-descriptions-item label="屏幕高度">{{ currentDevice.calibrationData.screenHeightMm }} mm</el-descriptions-item>
          <el-descriptions-item label="校准时间">{{ currentDevice.calibrationData.calibrationTime }}</el-descriptions-item>
          <el-descriptions-item label="校准人">{{ currentDevice.calibrationData.calibratedBy }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminDevice' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { deviceApi, storeApi } from '@/api'
import type { Device, Store } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const tableData = ref<Device[]>([])
const storeOptions = ref<Store[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const queryForm = reactive({
  storeId: undefined as number | undefined,
  status: undefined as number | undefined
})

// 绑定弹窗
const bindVisible = ref(false)
const bindFormRef = ref<FormInstance>()
const submitLoading = ref(false)
const bindForm = reactive({
  storeId: undefined as number | undefined,
  deviceCode: '',
  deviceName: ''
})

const bindRules: FormRules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

// 详情弹窗
const detailVisible = ref(false)
const currentDevice = ref<Device | null>(null)

const fetchStores = async () => {
  try {
    const res = await storeApi.getAllStores()
    storeOptions.value = res
  } catch (error) {
    console.error('获取门店列表失败', error)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await deviceApi.getDeviceList({
      storeId: queryForm.storeId,
      status: queryForm.status,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取设备列表失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.storeId = undefined
  queryForm.status = undefined
  pagination.page = 1
  fetchData()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

const handleBind = () => {
  bindForm.storeId = undefined
  bindForm.deviceCode = ''
  bindForm.deviceName = ''
  bindVisible.value = true
}

const handleBindSubmit = async () => {
  if (!bindFormRef.value) return
  await bindFormRef.value.validate(async (valid) => {
    if (valid) {
      if (!bindForm.storeId) {
        ElMessage.warning('请选择绑定门店')
        return
      }
      submitLoading.value = true
      try {
        await deviceApi.bindDevice({
          storeId: bindForm.storeId,
          deviceCode: bindForm.deviceCode,
          deviceName: bindForm.deviceName
        })
        ElMessage.success('绑定成功')
        bindVisible.value = false
        fetchData()
      } catch (error) {
        console.error('绑定失败', error)
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleView = (row: Device) => {
  currentDevice.value = row
  detailVisible.value = true
}

const handleCalibrate = (row: Device) => {
  ElMessageBox.prompt('请输入校准参数（JSON格式）', '设备校准', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValue: JSON.stringify({
      pixelPerMm: 3.78,
      screenWidthMm: 1210,
      screenHeightMm: 680
    }, null, 2)
  }).then(({ value }) => {
    try {
      const calibrationData = JSON.parse(value)
      deviceApi.updateCalibration(row.id, {
        calibrationStatus: 1,
        calibrationData: {
          ...calibrationData,
          calibrationTime: new Date().toISOString(),
          calibratedBy: '运营管理员'
        }
      })
      ElMessage.success('校准成功')
      fetchData()
    } catch {
      ElMessage.error('参数格式错误')
    }
  }).catch(() => {})
}

const handleUnbind = async (row: Device) => {
  try {
    await ElMessageBox.confirm(`确定要解绑设备"${row.deviceName}"吗？`, '提示', {
      type: 'warning'
    })
    await deviceApi.unbindDevice(row.id)
    ElMessage.success('解绑成功')
    fetchData()
  } catch {
    // 取消
  }
}

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
.device-page {
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
