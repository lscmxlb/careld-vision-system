<template>
  <div class="store-list-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>门店管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增门店
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :model="queryForm" inline class="search-form">
        <el-form-item label="省份">
          <el-select v-model="queryForm.provinceCode" placeholder="选择省份" clearable>
            <el-option
              v-for="item in provinces"
              :key="item.code"
              :label="item.name"
              :value="item.code"
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
            placeholder="门店名称/编码"
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
        <el-table-column prop="storeCode" label="门店编码" width="120" />
        <el-table-column prop="storeName" label="门店名称" min-width="150" />
        <el-table-column prop="provinceName" label="省份" width="100" />
        <el-table-column prop="cityName" label="城市" width="100" />
        <el-table-column prop="contactName" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column prop="deviceCount" label="设备数" width="80" />
        <el-table-column prop="staffCount" label="员工数" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" @click="handleView(row)">详情</el-button>
            <el-button
              :type="row.status === 1 ? 'danger' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="门店编码" prop="storeCode">
          <el-input v-model="formData.storeCode" placeholder="请输入门店编码" />
        </el-form-item>
        <el-form-item label="门店名称" prop="storeName">
          <el-input v-model="formData.storeName" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="所属地区" prop="districtCode">
          <el-cascader
            v-model="areaValue"
            :options="areaOptions"
            :props="{ value: 'code', label: 'name' }"
            placeholder="选择省/市/区"
            @change="handleAreaChange"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="formData.contactName" placeholder="请输入联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="营业时间" prop="businessHours">
          <el-time-picker
            v-model="businessTime"
            is-range
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="HH:mm"
            @change="handleTimeChange"
          />
        </el-form-item>
        <el-form-item label="网络类型" prop="networkType">
          <el-radio-group v-model="formData.networkType">
            <el-radio :label="1">有线网络</el-radio>
            <el-radio :label="2">无线网络</el-radio>
            <el-radio :label="3">混合网络</el-radio>
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
defineOptions({ name: 'AdminStoreList' })
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { storeApi } from '@/api'
import type { Store, StoreQuery } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()

// 表格数据
const loading = ref(false)
const tableData = ref<Store[]>([])
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 查询表单
const queryForm = reactive<StoreQuery>({
  page: 1,
  size: 10,
  status: undefined,
  provinceCode: undefined,
  keyword: ''
})

// 省份选项（模拟数据）
const provinces = ref([
  { code: '110000', name: '北京市' },
  { code: '310000', name: '上海市' },
  { code: '440000', name: '广东省' },
  { code: '330000', name: '浙江省' },
  { code: '320000', name: '江苏省' }
])

// 地区级联选项
const areaOptions = ref([
  {
    code: '110000',
    name: '北京市',
    children: [
      {
        code: '110100',
        name: '北京市',
        children: [
          { code: '110101', name: '东城区' },
          { code: '110102', name: '西城区' },
          { code: '110105', name: '朝阳区' },
          { code: '110106', name: '丰台区' }
        ]
      }
    ]
  }
])

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增门店')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const isEdit = ref(false)
const currentId = ref<number | null>(null)

const formData = reactive<Partial<Store>>({
  storeCode: '',
  storeName: '',
  provinceCode: '',
  provinceName: '',
  cityCode: '',
  cityName: '',
  districtCode: '',
  districtName: '',
  address: '',
  contactName: '',
  contactPhone: '',
  businessHours: '',
  networkType: 1
})

const areaValue = ref<string[]>([])
const businessTime = ref<[Date, Date] | null>(null)

const formRules: FormRules = {
  storeCode: [{ required: true, message: '请输入门店编码', trigger: 'blur' }],
  storeName: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  districtCode: [{ required: true, message: '请选择所属地区', trigger: 'change' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await storeApi.getStoreList({
      ...queryForm,
      page: pagination.page,
      size: pagination.size
    })
    tableData.value = res.data.list
    pagination.total = res.data.pagination.total
  } catch (error) {
    console.error('获取门店列表失败', error)
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
  queryForm.status = undefined
  queryForm.provinceCode = undefined
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
  dialogTitle.value = '新增门店'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: Store) => {
  isEdit.value = true
  dialogTitle.value = '编辑门店'
  currentId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 查看详情
const handleView = (row: Store) => {
  router.push(`/store/detail/${row.id}`)
}

// 切换状态
const handleToggleStatus = async (row: Store) => {
  try {
    await ElMessageBox.confirm(
      `确定要${row.status === 1 ? '禁用' : '启用'}门店"${row.storeName}"吗？`,
      '提示',
      { type: 'warning' }
    )
    await storeApi.updateStoreStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success('操作成功')
    fetchData()
  } catch {
    // 取消操作
  }
}

// 地区选择变化
const handleAreaChange = (value: string[]) => {
  if (value.length === 3) {
    formData.provinceCode = value[0]
    formData.cityCode = value[1]
    formData.districtCode = value[2]
    // 这里需要根据code查找对应的name
  }
}

// 时间选择变化
const handleTimeChange = (value: [Date, Date] | null) => {
  if (value) {
    const format = (date: Date) => {
      return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    }
    formData.businessHours = `${format(value[0])}-${format(value[1])}`
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(formData, {
    storeCode: '',
    storeName: '',
    provinceCode: '',
    provinceName: '',
    cityCode: '',
    cityName: '',
    districtCode: '',
    districtName: '',
    address: '',
    contactName: '',
    contactPhone: '',
    businessHours: '',
    networkType: 1
  })
  areaValue.value = []
  businessTime.value = null
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
  fetchData()
})
</script>

<style scoped lang="scss">
.store-list-page {
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
