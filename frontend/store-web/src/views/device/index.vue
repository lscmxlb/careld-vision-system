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

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="160" />
        <el-table-column prop="deviceName" label="设备名称" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="calibrationStatus" label="校准" width="100">
          <template #default="{ row }">
            <el-tag :type="row.calibrationStatus === 1 ? 'success' : 'warning'">
              {{ row.calibrationStatus === 1 ? '已校准' : '未校准' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最后同步" width="160">
          <template #default="{ row }">
            {{ row.lastSyncTime || '从未' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleSync(row)" :loading="row._syncing">同步</el-button>
            <el-button type="danger" size="small" @click="handleUnbind(row)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper" v-if="pagination.total > pagination.size">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 绑定弹窗 -->
    <el-dialog v-model="bindVisible" title="绑定设备" width="400px">
      <el-form :model="bindForm" :rules="bindRules" ref="bindFormRef" label-width="100px">
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input v-model="bindForm.deviceCode" placeholder="输入TV设备编码" />
        </el-form-item>
        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="bindForm.deviceName" placeholder="输入设备名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindVisible = false">取消</el-button>
        <el-button type="primary" :loading="bindLoading" @click="handleBindSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreDevice' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { Device } from '@/types'
import { deviceApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const bindVisible = ref(false)
const bindLoading = ref(false)
const bindFormRef = ref<FormInstance>()

const tableData = ref<(Device & { _syncing?: boolean })[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })

const bindForm = reactive({ deviceCode: '', deviceName: '' })
const bindRules = {
  deviceCode: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await deviceApi.getDeviceList({
      storeId: userStore.storeId,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list.map(item => ({ ...item, _syncing: false }))
    pagination.total = res.pagination.total
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

const handleBind = () => {
  bindForm.deviceCode = ''
  bindForm.deviceName = ''
  bindVisible.value = true
}

const handleBindSubmit = async () => {
  if (!bindFormRef.value) return
  await bindFormRef.value.validate()
  bindLoading.value = true
  try {
    await deviceApi.bindDevice({
      storeId: userStore.storeId!,
      deviceCode: bindForm.deviceCode,
      deviceName: bindForm.deviceName
    })
    ElMessage.success('绑定成功')
    bindVisible.value = false
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    bindLoading.value = false
  }
}

const handleSync = async (row: Device & { _syncing?: boolean }) => {
  row._syncing = true
  try {
    await deviceApi.syncDevice(row.id)
    ElMessage.success(`开始同步设备: ${row.deviceName}`)
    fetchData()
  } catch {
    // 错误已在拦截器处理
  } finally {
    row._syncing = false
  }
}

const handleUnbind = async (row: Device) => {
  try {
    await ElMessageBox.confirm(`确定解绑设备"${row.deviceName}"?`, '提示', { type: 'warning' })
    await deviceApi.unbindDevice(row.id)
    ElMessage.success('解绑成功')
    fetchData()
  } catch {
    // 用户取消或API错误
  }
}

onMounted(fetchData)
</script>

<style scoped lang="scss">
.device-page {
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
