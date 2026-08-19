<template>
  <div class="department-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>科室管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增科室
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="所属医院">
          <el-select v-model="queryForm.storeId" placeholder="选择医院" clearable filterable>
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
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="科室编码/名称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deptCode" label="科室编码" width="120" />
        <el-table-column prop="deptName" label="科室名称" min-width="150" />
        <el-table-column prop="storeName" label="所属医院" min-width="140" />
        <el-table-column prop="deptType" label="科室类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getDeptTypeTag(row.deptType)">
              {{ getDeptTypeLabel(row.deptType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="550px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="所属医院" prop="storeId">
          <el-select v-model="formData.storeId" placeholder="选择医院" filterable style="width: 100%">
            <el-option
              v-for="item in storeOptions"
              :key="item.id"
              :label="item.storeName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科室编码" prop="deptCode">
          <el-input v-model="formData.deptCode" placeholder="请输入科室编码" />
        </el-form-item>
        <el-form-item label="科室名称" prop="deptName">
          <el-input v-model="formData.deptName" placeholder="请输入科室名称" />
        </el-form-item>
        <el-form-item label="科室类型" prop="deptType">
          <el-select v-model="formData.deptType" placeholder="选择科室类型" style="width: 100%">
            <el-option label="门诊" :value="1" />
            <el-option label="养护" :value="2" />
            <el-option label="检测" :value="3" />
            <el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
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
defineOptions({ name: 'AdminDepartment' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { departmentApi, storeApi } from '@/api'
import type { Department, DepartmentQuery, Store } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

// 表格数据
const loading = ref(false)
const tableData = ref<Department[]>([])
const storeOptions = ref<Store[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 查询表单
const queryForm = reactive<DepartmentQuery>({
  storeId: undefined,
  status: undefined,
  keyword: ''
})

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增科室')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<Department>>({
  storeId: undefined,
  deptCode: '',
  deptName: '',
  deptType: 1,
  sortOrder: 0,
  status: 1,
  remark: ''
})

const formRules: FormRules = {
  storeId: [{ required: true, message: '请选择所属医院', trigger: 'change' }],
  deptCode: [{ required: true, message: '请输入科室编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
  deptType: [{ required: true, message: '请选择科室类型', trigger: 'change' }]
}

// 科室类型映射
const getDeptTypeLabel = (type: number) => {
  const map: Record<number, string> = {
    1: '门诊',
    2: '养护',
    3: '检测',
    4: '其他'
  }
  return map[type] || '未知'
}

const getDeptTypeTag = (type: number) => {
  const map: Record<number, string> = {
    1: 'primary',
    2: 'success',
    3: 'warning',
    4: 'info'
  }
  return map[type] || 'info'
}

// 获取医院列表（下拉用）
const fetchStores = async () => {
  try {
    const res = await storeApi.getAllStores()
    storeOptions.value = res
  } catch (error) {
    console.error('获取医院列表失败', error)
  }
}

// 获取科室列表
const fetchData = async () => {
  loading.value = true
  try {
    const res = await departmentApi.getDepartmentList({
      ...queryForm,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.list
    pagination.total = res.pagination.total
  } catch (error) {
    console.error('获取科室列表失败', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

// 重置
const handleReset = () => {
  queryForm.storeId = undefined
  queryForm.status = undefined
  queryForm.keyword = ''
  pagination.page = 1
  fetchData()
}

// 分页变化
const handleSizeChange = (val: number) => {
  pagination.size = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增科室'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: Department) => {
  isEdit.value = true
  dialogTitle.value = '编辑科室'
  currentId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 切换状态
const handleToggleStatus = async (row: Department) => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.status === 1 ? '禁用' : '启用'}科室"${row.deptName}"吗？`,
      '提示',
      { type: 'warning' }
    )
    await departmentApi.updateDepartmentStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchData()
  } catch {
    // 取消操作
  }
}

// 删除
const handleDelete = async (row: Department) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除科室"${row.deptName}"吗？此操作不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
    await departmentApi.deleteDepartment(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // 取消操作
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    storeId: undefined,
    deptCode: '',
    deptName: '',
    deptType: 1,
    sortOrder: 0,
    status: 1,
    remark: ''
  })
  currentId.value = null
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        if (isEdit.value && currentId.value) {
          await departmentApi.updateDepartment(currentId.value, formData)
          ElMessage.success('更新成功')
        } else {
          await departmentApi.createDepartment(formData)
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
  fetchData()
})
</script>

<style scoped lang="scss">
.department-page {
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
