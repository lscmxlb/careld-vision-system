<template>
  <div class="device-type-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备类型管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增设备类型
          </el-button>
        </div>
      </template>

      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryForm.keyword" placeholder="类型名称/编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon>搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="typeCode" label="类型编码" width="120" />
        <el-table-column prop="typeName" label="类型名称" min-width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="defaultServiceLife" label="默认使用寿命(月)" width="150" />
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
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="130px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model="formData.typeCode" placeholder="请输入类型编码" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="formData.typeName" placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="默认使用寿命(月)" prop="defaultServiceLife">
          <el-input-number v-model="formData.defaultServiceLife" :min="0" placeholder="如：36" />
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
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminDeviceTypes' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { deviceTypeApi } from '@/api'
import type { DeviceType } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const loading = ref(false)
const tableData = ref<DeviceType[]>([])
const pagination = reactive({ page: 1, size: 10, total: 0 })
const queryForm = reactive({ keyword: '' })
const dialogVisible = ref(false)
const dialogTitle = ref('新增设备类型')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<DeviceType>>({
  typeCode: '',
  typeName: '',
  description: '',
  defaultServiceLife: 36,
  status: 1
})

const formRules: FormRules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  defaultServiceLife: [{ required: true, message: '请输入默认使用寿命', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await deviceTypeApi.getDeviceTypeList({
      keyword: queryForm.keyword || undefined,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取设备类型列表失败', error)
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
  dialogTitle.value = '新增设备类型'
  Object.assign(formData, { typeCode: '', typeName: '', description: '', defaultServiceLife: 36, status: 1 })
  currentId.value = null
  dialogVisible.value = true
}

const handleEdit = (row: DeviceType) => {
  isEdit.value = true
  dialogTitle.value = '编辑设备类型'
  currentId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: DeviceType) => {
  try {
    await ElMessageBox.confirm(`确定要删除设备类型"${row.typeName}"吗？`, '提示', { type: 'warning' })
    await deviceTypeApi.deleteDeviceType(row.id)
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
          await deviceTypeApi.updateDeviceType(currentId.value, formData)
          ElMessage.success('更新成功')
        } else {
          await deviceTypeApi.createDeviceType(formData)
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
</script>

<style scoped lang="scss">
.device-type-page {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-form { margin-bottom: 20px; }
  .pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; }
}
</style>
