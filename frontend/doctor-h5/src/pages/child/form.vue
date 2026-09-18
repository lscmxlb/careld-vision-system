<template>
  <view class="page">
    <view class="form-body">
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
          <text class="field-label">所在学校</text>
          <input v-model="form.school" class="field-input" type="text" placeholder="选填" placeholder-class="field-placeholder" maxlength="50" />
        </view>

        <view class="field">
          <text class="field-label required">家长姓名</text>
          <input v-model="form.parentName" class="field-input" type="text" placeholder="请输入家长姓名" placeholder-class="field-placeholder" maxlength="20" />
        </view>

        <view class="field">
          <text class="field-label">关系</text>
          <picker mode="selector" :range="RELATION_OPTIONS" :value="relationIndex" @change="onRelationChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.relation }">
              {{ form.relation || '请选择关系' }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label required">手机号码</text>
          <input v-model="form.phone" class="field-input" type="number" placeholder="11位中国手机号" placeholder-class="field-placeholder" maxlength="11" />
        </view>

        <view class="field">
          <text class="field-label">家庭地址</text>
          <input v-model="form.homeAddress" class="field-input" type="text" placeholder="选填" placeholder-class="field-placeholder" maxlength="100" />
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

        <view class="field">
          <text class="field-label">分娩方式</text>
          <picker mode="selector" :range="DELIVERY_TYPE_OPTIONS" :value="deliveryIndex" @change="onDeliveryChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.deliveryType }">
              {{ form.deliveryType || '请选择分娩方式' }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label">休息时间</text>
          <picker mode="time" :value="form.bedtime || '21:00'" @change="onBedtimeChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.bedtime }">
              {{ form.bedtime || '请选择休息时间' }}
            </view>
          </picker>
        </view>

        <view class="field">
          <text class="field-label">起床时间</text>
          <picker mode="time" :value="form.wakeTime || '07:00'" @change="onWakeChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.wakeTime }">
              {{ form.wakeTime || '请选择起床时间' }}
            </view>
          </picker>
        </view>

        <view class="field field-block">
          <text class="field-label">既往病史</text>
          <textarea v-model="form.medicalHistory" class="field-textarea" placeholder="选填" placeholder-class="field-placeholder" maxlength="200" />
        </view>

        <view class="field field-block">
          <text class="field-label">过敏信息</text>
          <textarea v-model="form.allergyInfo" class="field-textarea" placeholder="选填" placeholder-class="field-placeholder" maxlength="200" />
        </view>
      </view>

      <view v-if="!isEdit" class="card">
        <view class="section-title">主治医师</view>
        <view class="field">
          <text class="field-label required">主治医师</text>
          <picker mode="selector" :range="doctorNames" :value="doctorIndex" @change="onDoctorChange">
            <view class="field-value" :class="{ 'field-placeholder': !form.doctorId }">
              {{ doctorName || '请选择主治医师（已注册医生）' }}
            </view>
          </picker>
        </view>
        <view class="form-tip">门店建档将自动通过审核</view>
      </view>
    </view>

    <view class="sticky-bar">
      <view class="btn btn-plain" @click="cancel">取消</view>
      <view class="btn btn-primary" :class="{ 'is-disabled': submitting }" @click="submit">
        {{ submitting ? '保存中…' : '保存档案' }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onBackPress, onLoad } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { medicalStaffApi } from '@/api/reserve'
import { useUserStore } from '@/stores/user'
import { todayStr } from '@/utils/format'
import { DELIVERY_TYPE_OPTIONS, EYE_CONDITION_OPTIONS, RELATION_OPTIONS, resolveEyeConditions } from '@/utils/dict'
import VisionPicker from '@/components/VisionPicker.vue'
import type { CreateChildRequest, MedicalStaff } from '@/types'
import { toast } from '@/utils/request'

const userStore = useUserStore()

const childId = ref(0)
const isEdit = computed(() => childId.value > 0)
const submitting = ref(false)
const leaving = ref(false)

const form = ref({
  name: '',
  parentName: '',
  relation: '',
  doctorId: undefined as number | undefined,
  phone: '',
  birthDate: '',
  gender: 1,
  homeAddress: '',
  school: '',
  deliveryType: '',
  bedtime: '',
  wakeTime: '',
  eyeConditions: [] as string[],
  nakedVisionBoth: '',
  nakedVisionLeft: '',
  nakedVisionRight: '',
  medicalHistory: '',
  allergyInfo: '',
})

const snapshot = ref('')

const doctors = ref<MedicalStaff[]>([])
const doctorNames = computed(() => doctors.value.map((d) => d.name))
const doctorName = computed(() => doctors.value.find((d) => d.id === form.value.doctorId)?.name || '')
const doctorIndex = computed(() => Math.max(0, doctors.value.findIndex((d) => d.id === form.value.doctorId)))

const relationIndex = computed(() => Math.max(0, RELATION_OPTIONS.indexOf(form.value.relation)))
const deliveryIndex = computed(() => Math.max(0, DELIVERY_TYPE_OPTIONS.indexOf(form.value.deliveryType)))

function capture() {
  snapshot.value = JSON.stringify(form.value)
}

function isDirty() {
  return JSON.stringify(form.value) !== snapshot.value
}

function onBirthChange(e: any) {
  form.value.birthDate = e.detail.value
}

function onRelationChange(e: any) {
  form.value.relation = RELATION_OPTIONS[Number(e.detail.value)] || ''
}

function onDeliveryChange(e: any) {
  form.value.deliveryType = DELIVERY_TYPE_OPTIONS[Number(e.detail.value)] || ''
}

function onBedtimeChange(e: any) {
  form.value.bedtime = e.detail.value
}

function onWakeChange(e: any) {
  form.value.wakeTime = e.detail.value
}

function onDoctorChange(e: any) {
  form.value.doctorId = doctors.value[Number(e.detail.value)]?.id
}

/** 视力状况互斥：正常独占；轻度/中度/高度近视三选一 */
function toggleEye(opt: string) {
  const next = form.value.eyeConditions.includes(opt)
    ? form.value.eyeConditions.filter((v) => v !== opt)
    : [...form.value.eyeConditions, opt]
  form.value.eyeConditions = resolveEyeConditions(next, opt)
}

async function loadDoctors() {
  try {
    const res = await medicalStaffApi.getStaffList({ staffRole: 1, status: 1, page: 1, size: 200 })
    doctors.value = res?.list || []
  } catch {
    doctors.value = []
  }
}

async function loadDetail() {
  const detail = await childApi.getChildDetail(childId.value)
  form.value = {
    name: detail.name || '',
    parentName: detail.parentName || '',
    relation: detail.relation || '',
    doctorId: detail.doctorId,
    phone: detail.phone || '',
    birthDate: detail.birthDate || '',
    gender: detail.gender,
    homeAddress: detail.homeAddress || '',
    school: detail.school || '',
    deliveryType: detail.deliveryType || '',
    bedtime: detail.bedtime || '',
    wakeTime: detail.wakeTime || '',
    eyeConditions: detail.eyeCondition ? detail.eyeCondition.split(/[、,，]/).filter(Boolean) : [],
    nakedVisionBoth: detail.nakedVisionBoth || '',
    nakedVisionLeft: detail.nakedVisionLeft || '',
    nakedVisionRight: detail.nakedVisionRight || '',
    medicalHistory: detail.medicalHistory || '',
    allergyInfo: detail.allergyInfo || '',
  }
  capture()
}

function validate(): string | null {
  if (!form.value.name.trim()) return '请输入儿童姓名'
  if (!form.value.parentName.trim()) return '请输入家长姓名'
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) return '请输入正确的11位中国手机号'
  if (!form.value.birthDate) return '请选择出生日期'
  if (!form.value.eyeConditions.length) return '请选择视力状况'
  if (!isEdit.value && !form.value.doctorId) return '请选择主治医师'
  return null
}

async function submit() {
  const error = validate()
  if (error) {
    toast(error)
    return
  }
  submitting.value = true
  try {
    const data: CreateChildRequest = {
      name: form.value.name.trim(),
      parentName: form.value.parentName.trim() || undefined,
      relation: form.value.relation || undefined,
      doctorId: form.value.doctorId,
      doctorName: doctorName.value || undefined,
      phone: form.value.phone,
      storeId: userStore.storeId,
      birthDate: form.value.birthDate,
      gender: form.value.gender,
      homeAddress: form.value.homeAddress || undefined,
      school: form.value.school || undefined,
      deliveryType: form.value.deliveryType || undefined,
      bedtime: form.value.bedtime || undefined,
      wakeTime: form.value.wakeTime || undefined,
      eyeCondition: form.value.eyeConditions.join('、'),
      nakedVisionBoth: form.value.nakedVisionBoth || undefined,
      nakedVisionLeft: form.value.nakedVisionLeft || undefined,
      nakedVisionRight: form.value.nakedVisionRight || undefined,
      medicalHistory: form.value.medicalHistory || undefined,
      allergyInfo: form.value.allergyInfo || undefined,
    }
    if (isEdit.value) {
      await childApi.updateChild(childId.value, data)
    } else {
      await childApi.createChild(data)
    }
    capture()
    toast('保存成功')
    leaving.value = true
    setTimeout(() => uni.navigateBack(), 400)
  } finally {
    submitting.value = false
  }
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
        uni.navigateBack()
      }
    },
  })
}

function cancel() {
  if (leaving.value || !isDirty()) {
    leaving.value = true
    uni.navigateBack()
    return
  }
  confirmDiscard()
}

onLoad(async (options) => {
  const id = Number(options?.id || 0)
  if (id > 0) {
    childId.value = id
    await loadDetail()
  } else {
    capture()
    const phone = String(options?.phone || '')
    if (/^1[3-9]\d{9}$/.test(phone)) {
      form.value.phone = phone
      capture()
      toast('已带入家长手机号，完善儿童信息后提交即可')
    }
  }
  loadDoctors()
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
  color: #2563eb;
  background: #dbeafe;
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
  color: #2563eb;
  background: #dbeafe;
  font-weight: 600;
}

.vision-line {
  display: flex;
  flex-direction: column;
  margin-top: 8rpx;
}

.form-tip {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
