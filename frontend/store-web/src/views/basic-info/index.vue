<template>
  <div class="basic-info-page">
    <!-- 基础信息：科室管理 + 预约规则设置，共用保存配置 -->
    <el-card v-loading="deptLoading || configLoading">
      <!-- 第一组：科室管理 -->
      <div class="section">
        <div class="section-title">科室管理</div>
        <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="120px" style="max-width: 560px;">
          <el-form-item label="科室编码" prop="deptCode">
            <el-input v-model="deptForm.deptCode" placeholder="请输入科室编码" />
          </el-form-item>
          <el-form-item label="科室名称" prop="deptName">
            <el-input v-model="deptForm.deptName" placeholder="请输入科室名称" />
          </el-form-item>
          <el-form-item label="科室类型" prop="deptType">
            <el-select v-model="deptForm.deptType" placeholder="请选择科室类型">
              <el-option label="门诊" :value="1" />
              <el-option label="养护" :value="2" />
              <el-option label="检测" :value="3" />
              <el-option label="其他" :value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="收费标准">
            <el-input-number
              v-model="deptForm.chargeStandard"
              :min="0"
              :precision="2"
              :step="10"
              placeholder="0.00"
              style="width: 160px;"
            />
            <span class="charge-unit">元</span>
          </el-form-item>
        </el-form>
      </div>

      <!-- 第二组：预约规则设置 -->
      <div class="section">
        <div class="section-title">预约规则设置</div>

        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="配置家长/医生取消预约的时间窗与爽约、自动完成规则，保存后立即生效。"
          style="margin-bottom: 20px;"
        />

        <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="200px" style="max-width: 640px;">
          <el-form-item label="家长可取消的提前小时数" prop="parentCancelHours">
            <el-input-number v-model="configForm.parentCancelHours" :min="0" :max="72" />
            <div class="form-tip">预约开始前不足该时长，家长端将无法取消预约</div>
          </el-form-item>
          <el-form-item label="医生可取消的提前小时数" prop="doctorCancelHours">
            <el-input-number v-model="configForm.doctorCancelHours" :min="0" :max="72" />
            <div class="form-tip">预约开始前不足该时长，医院端将无法取消预约</div>
          </el-form-item>
          <el-form-item label="爽约判定缓冲（分钟）" prop="noShowBufferMinutes">
            <el-input-number v-model="configForm.noShowBufferMinutes" :min="0" :max="240" />
            <div class="form-tip">预约开始超过该时长仍未开始养护，可标记爽约</div>
          </el-form-item>
          <el-form-item label="爽约自动标记时长（小时）" prop="autoNoShowHours">
            <el-input-number v-model="configForm.autoNoShowHours" :min="1" :max="72" />
            <div class="form-tip">预约时段结束后超过该时长仍未处理，系统自动标记为爽约（不退还次数）</div>
          </el-form-item>
          <el-form-item label="养护自动完成时长（小时）" prop="autoCompleteHours">
            <el-input-number v-model="configForm.autoCompleteHours" :min="0.5" :max="8" :step="0.5" />
            <div class="form-tip">开始养护超过该时长后，系统自动完成并按养护后视力为空处理</div>
          </el-form-item>
          <el-form-item label="预约记录默认显示选择" prop="defaultShowStatuses">
            <el-checkbox-group v-model="configForm.defaultShowStatuses">
              <el-checkbox value="1">已预约</el-checkbox>
              <el-checkbox value="2">养护中</el-checkbox>
              <el-checkbox value="3">已完成</el-checkbox>
              <el-checkbox value="4">已取消</el-checkbox>
              <el-checkbox value="5">已爽约</el-checkbox>
            </el-checkbox-group>
            <div class="form-tip">医院端「预约记录」页进入时，默认只显示勾选状态的记录（至少勾选一项）</div>
          </el-form-item>
        </el-form>
      </div>

      <div class="footer-actions">
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存配置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StoreBasicInfo' })
import { ref, reactive, computed, onMounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { departmentApi, appointmentConfigApi } from '@/api'

const userStore = useUserStore()

// ---------- 科室管理 ----------
const deptFormRef = ref<FormInstance>()
const deptLoading = ref(false)

const deptForm = reactive({
  id: null as number | null,
  deptCode: '',
  deptName: '',
  deptType: 1,
  chargeStandard: undefined as number | undefined,
  sortOrder: 0
})

const deptRules = {
  deptCode: [{ required: true, message: '请输入科室编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }],
  deptType: [{ required: true, message: '请选择科室类型', trigger: 'change' }]
}

const loadDepartment = async () => {
  if (!userStore.storeId) return
  deptLoading.value = true
  try {
    const list = await departmentApi.getDepartmentList(userStore.storeId)
    const current = list[0]
    if (!current) return
    Object.assign(deptForm, {
      id: current.id,
      deptCode: current.deptCode,
      deptName: current.deptName,
      deptType: current.deptType,
      chargeStandard: current.chargeStandard ?? undefined,
      sortOrder: current.sortOrder
    })
  } catch {
    // 错误已在拦截器处理
  } finally {
    deptLoading.value = false
  }
}

/** 科室信息是否已填写（门店无科室记录时据此决定是否新建，避免信息静默丢失） */
const hasDeptInput = computed(
  () => !!(deptForm.deptCode || deptForm.deptName || deptForm.chargeStandard != null)
)

// ---------- 预约规则 ----------
const configFormRef = ref<FormInstance>()
const configLoading = ref(false)

const configForm = reactive({
  parentCancelHours: 2,
  doctorCancelHours: 1,
  noShowBufferMinutes: 15,
  autoNoShowHours: 12,
  autoCompleteHours: 1.5,
  defaultShowStatuses: ['1', '2', '3', '4', '5'] as string[]
})

const configRules = {
  parentCancelHours: [{ required: true, message: '请输入提前小时数', trigger: 'change' }],
  doctorCancelHours: [{ required: true, message: '请输入提前小时数', trigger: 'change' }],
  noShowBufferMinutes: [{ required: true, message: '请输入缓冲分钟数', trigger: 'change' }],
  autoNoShowHours: [{ required: true, message: '请输入自动标记时长', trigger: 'change' }],
  autoCompleteHours: [{ required: true, message: '请输入自动完成时长', trigger: 'change' }],
  defaultShowStatuses: [
    {
      validator: (_rule: unknown, value: string[], callback: (error?: Error) => void) => {
        if (!value || value.length === 0) {
          callback(new Error('至少勾选一项'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

const loadConfig = async () => {
  configLoading.value = true
  try {
    const config = await appointmentConfigApi.getConfig()
    Object.assign(configForm, {
      parentCancelHours: config.parentCancelHours,
      doctorCancelHours: config.doctorCancelHours,
      noShowBufferMinutes: config.noShowBufferMinutes,
      autoNoShowHours: config.autoNoShowHours ?? 12,
      autoCompleteHours: Number(config.autoCompleteHours),
      defaultShowStatuses: (config.defaultShowStatuses || '1,2,3,4,5').split(',').filter(Boolean)
    })
  } catch {
    // 错误已在拦截器处理
  } finally {
    configLoading.value = false
  }
}

// ---------- 统一保存与未保存提醒 ----------
const submitLoading = ref(false)
const baseline = ref('')

/** 两组表单的可比较快照（勾选组排序归一，避免点击顺序造成误判） */
const takeSnapshot = () =>
  JSON.stringify({
    deptCode: deptForm.deptCode,
    deptName: deptForm.deptName,
    deptType: deptForm.deptType,
    chargeStandard: deptForm.chargeStandard ?? null,
    parentCancelHours: configForm.parentCancelHours,
    doctorCancelHours: configForm.doctorCancelHours,
    noShowBufferMinutes: configForm.noShowBufferMinutes,
    autoNoShowHours: configForm.autoNoShowHours,
    autoCompleteHours: configForm.autoCompleteHours,
    defaultShowStatuses: [...configForm.defaultShowStatuses].sort().join(',')
  })

const isDirty = computed(() => baseline.value !== '' && takeSnapshot() !== baseline.value)

const handleSubmit = async () => {
  if (!deptFormRef.value || !configFormRef.value) return
  // 无科室记录且未填写科室信息时不校验科室组，配置仍可保存
  const validations: Promise<unknown>[] = [configFormRef.value.validate()]
  if (deptForm.id || hasDeptInput.value) {
    validations.push(deptFormRef.value.validate())
  }
  try {
    await Promise.all(validations)
  } catch {
    return
  }
  submitLoading.value = true
  try {
    // chargeStandard 显式传 null：允许清空已配置的收费标准
    const deptPayload = {
      storeId: userStore.storeId,
      deptCode: deptForm.deptCode,
      deptName: deptForm.deptName,
      deptType: deptForm.deptType,
      chargeStandard: deptForm.chargeStandard ?? null,
      sortOrder: deptForm.sortOrder
    }
    if (deptForm.id) {
      await departmentApi.updateDepartment(deptForm.id, deptPayload)
    } else if (hasDeptInput.value) {
      // 门店尚无科室记录：首次保存时创建，否则科室信息不会落库
      deptForm.id = await departmentApi.createDepartment(deptPayload)
    }
    await appointmentConfigApi.saveConfig({
      ...configForm,
      defaultShowStatuses: configForm.defaultShowStatuses.join(',')
    })
    ElMessage.success('保存成功')
    baseline.value = takeSnapshot()
  } catch {
    // 错误已在拦截器处理
  } finally {
    submitLoading.value = false
  }
}

onBeforeRouteLeave(async () => {
  if (!isDirty.value) return true
  try {
    await ElMessageBox.confirm('有未保存的修改，确认放弃吗？', '提示', {
      confirmButtonText: '放弃修改',
      cancelButtonText: '继续编辑',
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
})

onMounted(async () => {
  await Promise.all([loadDepartment(), loadConfig()])
  baseline.value = takeSnapshot()
})
</script>

<style scoped lang="scss">
.basic-info-page {
  .section + .section {
    margin-top: 28px;
  }

  .section-title {
    padding-left: 8px;
    border-left: 3px solid var(--el-color-primary);
    font-size: 15px;
    font-weight: 600;
    line-height: 1.2;
    color: #303133;
    margin-bottom: 16px;
  }

  .footer-actions {
    margin-top: 4px;
  }

  .form-tip {
    color: #999;
    font-size: 12px;
    line-height: 1.5;
    margin-top: 4px;
  }

  .charge-unit {
    margin-left: 8px;
    color: #606266;
    font-size: 14px;
  }
}
</style>
