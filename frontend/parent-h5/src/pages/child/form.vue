<template>
  <view class="page">
    <view class="form-body">
      <view class="card">
        <view class="section-title">医院信息</view>

        <view class="field">
          <text class="field-label required">所在地区</text>
          <picker
            mode="multiSelector"
            :range="regionColumns"
            :value="regionIndex"
            @columnchange="onRegionColumnChange"
            @change="onRegionChange"
          >
            <view class="field-value" :class="{ 'field-placeholder': !form.provinceName }">
              {{ regionText }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label required">医院</text>
          <picker mode="selector" :range="storeNames" :value="storeIndex" @change="onStoreChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.storeId }">
              {{ storeName || (form.provinceName ? '请选择医院' : '请先选择省/市/区') }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label required">主治医师</text>
          <picker mode="selector" :range="doctorNames" :value="doctorIndex" @change="onDoctorChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.doctorId }">
              {{ doctorName || doctorPlaceholder }}
            </view>
          </picker>
        </view>

        <view v-if="form.storeId && !doctors.length && !doctorLoading" class="form-tip">
          该医院暂无可选医师，请选择其他医院
        </view>
      </view>

      <view class="card">
        <view class="section-title">基本信息</view>

        <view class="field">
          <text class="field-label required">儿童姓名</text>
          <input v-model="form.name" class="field-input" type="text" placeholder="请输入儿童姓名" placeholder-class="field-placeholder" maxlength="20" />
        </view>

        <view class="field">
          <text class="field-label required">性别</text>
          <view class="radio-row">
            <view class="radio-item" :class="{ 'radio-active': form.gender === 1 }" @click="form.gender = 1">男</view>
            <view class="radio-item" :class="{ 'radio-active': form.gender === 0 }" @click="form.gender = 0">女</view>
          </view>
        </view>

        <view class="field">
          <text class="field-label required">出生日期</text>
          <picker mode="date" :value="form.birthDate" :end="todayStr()" @change="onBirthChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.birthDate }">
              {{ form.birthDate || '请选择出生日期' }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label required">家长姓名</text>
          <input v-model="form.parentName" class="field-input" type="text" placeholder="请输入家长姓名" placeholder-class="field-placeholder" maxlength="20" />
        </view>

        <view class="field">
          <text class="field-label required">与儿童关系</text>
          <picker mode="selector" :range="RELATION_OPTIONS" :value="relationIndex" @change="onRelationChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.relation }">
              {{ form.relation || '请选择与儿童关系' }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label required">手机号码</text>
          <input v-model="form.phone" class="field-input" type="number" placeholder="11位中国手机号" placeholder-class="field-placeholder" maxlength="11" />
        </view>

        <view class="field">
          <text class="field-label">所在学校</text>
          <input v-model="form.school" class="field-input" type="text" placeholder="选填" placeholder-class="field-placeholder" maxlength="50" />
        </view>
      </view>

      <view class="card">
        <view class="section-title">视力情况</view>

        <view class="field field-block">
          <text class="field-label required">视力状况</text>
          <view class="eye-grid">
            <view
              v-for="opt in EYE_CONDITION_OPTIONS"
              :key="opt"
              class="eye-item"
              :class="{ 'eye-active': form.eyeConditions.includes(opt) }"
              @click="toggleEye(opt)"
            >
              {{ opt }}
            </view>
          </view>
        </view>

        <view class="field field-block">
          <text class="field-label">裸眼视力</text>
          <view class="vision-line">
            <VisionPicker v-model="form.nakedVisionBoth" label="双眼" :show-sub="false" />
            <VisionPicker v-model="form.nakedVisionLeft" label="左眼" :show-sub="false" />
            <VisionPicker v-model="form.nakedVisionRight" label="右眼" :show-sub="false" />
          </view>
        </view>
      </view>

      <view class="card">
        <view class="section-title">其它信息</view>

        <view class="field field-block">
          <text class="field-label">既往病史</text>
          <textarea v-model="form.medicalHistory" class="field-textarea" placeholder="选填" placeholder-class="field-placeholder" maxlength="200" />
        </view>

        <view class="field field-block">
          <text class="field-label">过敏信息</text>
          <textarea v-model="form.allergyInfo" class="field-textarea" placeholder="选填" placeholder-class="field-placeholder" maxlength="200" />
        </view>
      </view>

      <view class="form-tip">提交后档案将进入医院审核，审核通过后即可预约养护。</view>
    </view>

    <view class="sticky-bar">
      <view class="btn btn-plain" @click="cancel">取消</view>
      <view class="btn btn-primary" :class="{ 'is-disabled': submitting }" @click="submit">
        {{ submitting ? '提交中…' : '提交建档' }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onBackPress, onLoad } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { userApi } from '@/api/auth'
import { storeApi } from '@/api/store'
import { medicalStaffApi } from '@/api/reserve'
import { useUserStore } from '@/stores/user'
import { todayStr } from '@/utils/format'
import { EYE_CONDITION_OPTIONS, RELATION_OPTIONS, resolveEyeConditions } from '@/utils/dict'
import VisionPicker from '@/components/VisionPicker.vue'
import { toast } from '@/utils/request'
import type { CreateChildRequest, MedicalStaff, Store } from '@/types'

const userStore = useUserStore()

const submitting = ref(false)
const leaving = ref(false)

const form = ref({
  provinceName: '',
  cityName: '',
  districtName: '',
  storeId: undefined as number | undefined,
  doctorId: undefined as number | undefined,
  name: '',
  parentName: '',
  relation: '',
  phone: '',
  birthDate: '',
  gender: 1,
  school: '',
  eyeConditions: [] as string[],
  nakedVisionBoth: '',
  nakedVisionLeft: '',
  nakedVisionRight: '',
  medicalHistory: '',
  allergyInfo: '',
})

/* ---------------- 医院信息（省市区 → 医院 → 主治医师） ---------------- */
const stores = ref<Store[]>([])
const regionColumns = ref<string[][]>([[], [], []])
const regionIndex = ref<number[]>([0, 0, 0])

const doctors = ref<MedicalStaff[]>([])
const doctorLoading = ref(false)

function uniqueNames(list: Store[], key: keyof Store) {
  const seen = new Set<string>()
  const out: string[] = []
  list.forEach((s) => {
    const name = String(s[key] || '')
    if (!name || seen.has(name)) return
    seen.add(name)
    out.push(name)
  })
  return out
}

const provinceNames = computed(() => uniqueNames(stores.value, 'provinceName'))

/** 按当前高亮列重建级联选项（省→市→区） */
function rebuildColumns(provinceIdx = regionIndex.value[0], cityIdx = regionIndex.value[1]) {
  const province = provinceNames.value[provinceIdx] || ''
  const cityList = province ? uniqueNames(stores.value.filter((s) => s.provinceName === province), 'cityName') : []
  const city = cityList[cityIdx] || ''
  const districtList = city
    ? uniqueNames(stores.value.filter((s) => s.provinceName === province && s.cityName === city), 'districtName')
    : []
  regionColumns.value = [provinceNames.value, cityList, districtList]
}

const regionText = computed(() => {
  const text = [form.value.provinceName, form.value.cityName, form.value.districtName].filter(Boolean).join(' / ')
  return text || '请选择省/市/区'
})

function onRegionColumnChange(e: any) {
  const column = Number(e.detail.column)
  const value = Number(e.detail.value)
  if (column === 0) {
    regionIndex.value = [value, 0, 0]
    rebuildColumns(value, 0)
  } else if (column === 1) {
    regionIndex.value = [regionIndex.value[0], value, 0]
    rebuildColumns(regionIndex.value[0], value)
  } else {
    regionIndex.value = [regionIndex.value[0], regionIndex.value[1], value]
  }
}

function onRegionChange(e: any) {
  const idx: number[] = e.detail.value || regionIndex.value
  regionIndex.value = idx
  rebuildColumns(idx[0], idx[1])
  form.value.provinceName = regionColumns.value[0][idx[0]] || ''
  form.value.cityName = regionColumns.value[1][idx[1]] || ''
  form.value.districtName = regionColumns.value[2][idx[2]] || ''
  // 切换地区后原医院/医师不在候选内，需重选
  form.value.storeId = undefined
  form.value.doctorId = undefined
  doctors.value = []
}

const filteredStores = computed(() =>
  stores.value.filter(
    (s) =>
      s.provinceName === form.value.provinceName &&
      s.cityName === form.value.cityName &&
      s.districtName === form.value.districtName,
  ),
)
const storeNames = computed(() => filteredStores.value.map((s) => s.storeName))
const storeIndex = computed(() => Math.max(0, filteredStores.value.findIndex((s) => s.id === form.value.storeId)))
const storeName = computed(() => filteredStores.value.find((s) => s.id === form.value.storeId)?.storeName || '')

function onStoreChange(e: any) {
  form.value.storeId = filteredStores.value[Number(e.detail.value)]?.id
  form.value.doctorId = undefined
  doctors.value = []
  loadDoctors()
}

const doctorNames = computed(() => doctors.value.map((d) => d.name))
const doctorName = computed(() => doctors.value.find((d) => d.id === form.value.doctorId)?.name || '')
const doctorIndex = computed(() => Math.max(0, doctors.value.findIndex((d) => d.id === form.value.doctorId)))
const doctorPlaceholder = computed(() => {
  if (!form.value.storeId) return '请先选择医院'
  if (doctorLoading.value) return '加载中…'
  if (!doctors.value.length) return '该医院暂无可选医师'
  return '请选择主治医师'
})

async function loadDoctors() {
  if (!form.value.storeId) return
  doctorLoading.value = true
  try {
    const res = await medicalStaffApi.getStaffList({
      storeId: form.value.storeId,
      staffRole: 1,
      status: 1,
      page: 1,
      size: 200,
    })
    doctors.value = res?.list || []
  } catch (err: any) {
    console.error('加载医师列表失败:', err)
    uni.showToast({ title: err?.message || '加载医师列表失败', icon: 'none' })
    doctors.value = []
  } finally {
    doctorLoading.value = false
  }
}

function onDoctorChange(e: any) {
  form.value.doctorId = doctors.value[Number(e.detail.value)]?.id
}

const relationIndex = computed(() => Math.max(0, RELATION_OPTIONS.indexOf(form.value.relation)))

function onRelationChange(e: any) {
  form.value.relation = RELATION_OPTIONS[Number(e.detail.value)] || ''
}

const snapshot = ref('')

function capture() {
  snapshot.value = JSON.stringify(form.value)
}

function isDirty() {
  return JSON.stringify(form.value) !== snapshot.value
}

function onBirthChange(e: any) {
  form.value.birthDate = e.detail.value
}

/** 视力状况互斥：正常独占；轻度/中度/高度近视三选一 */
function toggleEye(opt: string) {
  const next = form.value.eyeConditions.includes(opt)
    ? form.value.eyeConditions.filter((v) => v !== opt)
    : [...form.value.eyeConditions, opt]
  form.value.eyeConditions = resolveEyeConditions(next, opt)
}

function validate(): string | null {
  if (!form.value.provinceName || !form.value.cityName || !form.value.districtName) return '请选择省/市/区'
  if (!form.value.storeId) return '请选择医院'
  if (!form.value.doctorId) return doctors.value.length ? '请选择主治医师' : '该医院暂无可选医师，请选择其他医院'
  if (!form.value.name.trim()) return '请输入儿童姓名'
  if (!form.value.parentName.trim()) return '请输入家长姓名'
  if (!form.value.relation) return '请选择与儿童关系'
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) return '请输入正确的11位中国手机号'
  if (!form.value.birthDate) return '请选择出生日期'
  if (!form.value.eyeConditions.length) return '请选择视力状况'
  return null
}

async function submit() {
  if (submitting.value) return
  const error = validate()
  if (error) {
    toast(error)
    return
  }
  submitting.value = true
  try {
    const data: CreateChildRequest = {
      name: form.value.name.trim(),
      parentName: form.value.parentName.trim(),
      relation: form.value.relation,
      storeId: form.value.storeId,
      doctorId: form.value.doctorId,
      doctorName: doctorName.value || undefined,
      phone: form.value.phone,
      birthDate: form.value.birthDate,
      gender: form.value.gender,
      school: form.value.school || undefined,
      eyeCondition: form.value.eyeConditions.join('、'),
      nakedVisionBoth: form.value.nakedVisionBoth || undefined,
      nakedVisionLeft: form.value.nakedVisionLeft || undefined,
      nakedVisionRight: form.value.nakedVisionRight || undefined,
      medicalHistory: form.value.medicalHistory || undefined,
      allergyInfo: form.value.allergyInfo || undefined,
    }
    await childApi.createChild(data)
    await syncAccountName(form.value.parentName.trim())
    capture()
    uni.showToast({ title: '已提交，待医院审核', icon: 'none', duration: 2500 })
    leaving.value = true
    setTimeout(() => goBack(), 600)
  } catch {
    // 错误提示已在请求层处理
  } finally {
    submitting.value = false
  }
}

/** 建档输入的家长姓名自动保存为当前账号姓名（失败不阻塞建档结果） */
async function syncAccountName(realName: string) {
  if (!realName || realName === userStore.userInfo?.realName) return
  try {
    await userApi.updateMyProfile({ realName })
    await userStore.fetchProfile()
  } catch {
    // 姓名同步失败不影响建档
  }
}

/** 返回上一级；无上一级（直接打开或刷新本页）时回到儿童档案列表 */
function goBack() {
  if (getCurrentPages().length > 1) {
    uni.navigateBack()
    return
  }
  uni.switchTab({ url: '/pages/child/index' })
}

function confirmDiscard() {
  uni.showModal({
    title: '提示',
    content: '有未保存的修改，确认放弃吗？',
    confirmText: '放弃修改',
    cancelText: '继续编辑',
    success: (res) => {
      if (res.confirm) {
        leaving.value = true
        goBack()
      }
    },
  })
}

function cancel() {
  if (leaving.value || !isDirty()) {
    leaving.value = true
    goBack()
    return
  }
  confirmDiscard()
}

async function loadStores() {
  try {
    stores.value = (await storeApi.getAllStores()) || []
  } catch {
    stores.value = []
  }
  rebuildColumns(0, 0)
}

onLoad(async () => {
  if (userStore.phone) form.value.phone = userStore.phone
  const realName = userStore.userInfo?.realName || ''
  // 账号姓名若为系统自动生成（家长+手机尾号4位）则不预填
  if (realName && !/^家长\d{4}$/.test(realName)) form.value.parentName = realName
  await loadStores()
  capture()
})

onBackPress(() => {
  if (leaving.value || !isDirty()) return false
  confirmDiscard()
  return true
})
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 180rpx;
}

.form-body {
  padding: 20rpx 24rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 8rpx;
}

.field {
  display: flex;
  align-items: center;
  min-height: 88rpx;
  border-bottom: 1rpx solid #f8fafc;
}

.field:last-child {
  border-bottom: none;
}

.field-block {
  flex-direction: column;
  align-items: stretch;
  padding: 20rpx 0;
}

.field-label {
  width: 180rpx;
  flex: none;
  font-size: 28rpx;
  color: #475569;
}

.field-label.required::before {
  content: '*';
  color: #ef4444;
  margin-right: 6rpx;
}

.field-value {
  flex: 1;
  font-size: 28rpx;
  color: #1e293b;
  text-align: right;
}

.field-input {
  flex: 1;
  font-size: 28rpx;
  color: #1e293b;
  text-align: right;
}

.field-placeholder {
  color: #cbd5e1;
}

.field-textarea {
  width: 100%;
  height: 140rpx;
  margin-top: 12rpx;
  padding: 16rpx;
  font-size: 28rpx;
  color: #1e293b;
  background: #f8fafc;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.radio-row {
  display: flex;
  flex: 1;
  justify-content: flex-end;
}

.radio-item {
  padding: 10rpx 40rpx;
  margin-left: 16rpx;
  font-size: 28rpx;
  color: #475569;
  background: #f1f5f9;
  border-radius: 10rpx;
}

.radio-active {
  color: #14b8a6;
  background: #ccfbf1;
  font-weight: 600;
}

.eye-grid {
  display: flex;
  flex-wrap: wrap;
  margin-top: 12rpx;
}

.eye-item {
  width: 23%;
  margin: 0 2% 16rpx 0;
  padding: 16rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #475569;
  background: #f1f5f9;
  border-radius: 10rpx;
  box-sizing: border-box;
}

.eye-item:nth-child(4n) {
  margin-right: 0;
}

.eye-active {
  color: #14b8a6;
  background: #ccfbf1;
  font-weight: 600;
}

.vision-line {
  display: flex;
  flex-direction: column;
  margin-top: 8rpx;
}

.form-tip {
  margin-top: 16rpx;
  padding: 20rpx 24rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #b45309;
  background: #fffbeb;
  border-radius: 12rpx;
}
</style>
