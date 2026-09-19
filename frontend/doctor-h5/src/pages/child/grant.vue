<template>
  <view class="page">
    <view v-if="child" class="hero">
      <text class="hero-name">{{ child.name || '未命名' }}<text class="hero-phone">[{{ child.phone || '—' }}]</text></text>
      <view class="hero-remain-row">
        <text class="hero-remain-label">当前可用次数</text>
        <text class="hero-remain">{{ child.remainingCount ?? 0 }}<text class="hero-remain-unit">次</text></text>
      </view>
    </view>

    <view class="form-body">
      <view class="card">
        <view class="section-title">授权信息</view>

        <view class="field">
          <text class="field-label required">开单医师</text>
          <view class="field-control">
            <picker mode="selector" :range="doctorNames" :value="doctorIndex" @change="onDoctorChange">
              <view class="select-box">
                <text class="select-text" :class="{ 'field-placeholder': !form.doctorId }">
                  {{ doctorName || '请选择开单医师' }}
                </text>
                <view class="select-arrow" />
              </view>
            </picker>
          </view>
        </view>

        <view class="field">
          <text class="field-label required">缴费方式</text>
          <view class="field-control">
            <picker mode="selector" :range="PAYMENT_METHODS" :value="methodIndex" @change="onMethodChange">
              <view class="select-box">
                <text class="select-text">{{ form.paymentMethod }}</text>
                <view class="select-arrow" />
              </view>
            </picker>
          </view>
        </view>

        <view class="field">
          <text class="field-label required">授权次数</text>
          <view class="stepper">
            <view class="stepper-btn" :class="{ 'is-off': (form.changeCount || 1) <= 1 }" @click="stepCount(-1)">−</view>
            <input v-model.number="form.changeCount" class="stepper-input" type="number" @input="calcPaymentAmount" />
            <view class="stepper-btn" :class="{ 'is-off': (form.changeCount || 0) >= 99 }" @click="stepCount(1)">＋</view>
            <text class="stepper-unit">次</text>
          </view>
        </view>

        <view class="field">
          <text class="field-label required">缴费金额</text>
          <view class="amount-wrap">
            <input v-model.number="form.paymentAmount" class="field-input" type="digit" :disabled="autoAmount" />
            <text class="amount-unit">元</text>
          </view>
        </view>

        <view v-if="autoAmount" class="amount-tip">
          按「{{ form.changeCount }} 次 × {{ departmentStandard }} 元/次」自动计算
        </view>
        <view v-else class="amount-tip">当前缴费方式不计费，可手动填写金额</view>

        <view class="field field-block">
          <text class="field-label">备注</text>
          <textarea v-model="form.remark" class="field-textarea" placeholder="请录入诊疗单号码、购买套餐、活动赠送等信息" placeholder-class="field-placeholder" maxlength="200" />
        </view>
      </view>
    </view>

    <view class="sticky-bar">
      <view class="btn btn-plain" @click="cancel">取消</view>
      <view class="btn btn-primary" :class="{ 'is-disabled': submitting }" @click="submit">
        {{ submitting ? '提交中…' : '确定授予' }}
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onBackPress, onLoad } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { departmentApi, medicalStaffApi } from '@/api/reserve'
import { useUserStore } from '@/stores/user'
import { CHARGED_METHODS, PAYMENT_METHODS } from '@/utils/dict'
import type { Child, MedicalStaff } from '@/types'
import { toast } from '@/utils/request'

const userStore = useUserStore()

const childId = ref(0)
const from = ref('')
const child = ref<Child | null>(null)
const submitting = ref(false)
const leaving = ref(false)

const doctors = ref<MedicalStaff[]>([])
const departmentStandard = ref(0)

const form = ref({
  doctorId: undefined as number | undefined,
  changeCount: 1 as number | undefined,
  paymentMethod: '自费支付',
  paymentAmount: 0 as number | undefined,
  remark: '',
})

const snapshot = ref('')

const doctorNames = computed(() => doctors.value.map((d) => d.name))
const doctorName = computed(() => doctors.value.find((d) => d.id === form.value.doctorId)?.name || '')
const doctorIndex = computed(() => Math.max(0, doctors.value.findIndex((d) => d.id === form.value.doctorId)))
const methodIndex = computed(() => Math.max(0, PAYMENT_METHODS.indexOf(form.value.paymentMethod)))
const autoAmount = computed(() => CHARGED_METHODS.includes(form.value.paymentMethod))

function capture() {
  snapshot.value = JSON.stringify(form.value)
}

function isDirty() {
  return JSON.stringify(form.value) !== snapshot.value
}

function calcPaymentAmount() {
  if ((form.value.changeCount || 0) > 99) form.value.changeCount = 99
  if ((form.value.changeCount || 0) < 1) form.value.changeCount = 1
  if (!autoAmount.value) {
    form.value.paymentAmount = 0
    return
  }
  const count = form.value.changeCount || 0
  form.value.paymentAmount = Math.round(count * departmentStandard.value * 100) / 100
}

function stepCount(delta: number) {
  form.value.changeCount = (form.value.changeCount || 0) + delta
  calcPaymentAmount()
}

function onMethodChange(e: any) {
  form.value.paymentMethod = PAYMENT_METHODS[Number(e.detail.value)] || PAYMENT_METHODS[0]
  calcPaymentAmount()
}

function onDoctorChange(e: any) {
  form.value.doctorId = doctors.value[Number(e.detail.value)]?.id
}

async function loadDoctors() {
  try {
    const res = await medicalStaffApi.getStaffList({ staffRole: 1, status: 1, page: 1, size: 200 })
    doctors.value = res?.list || []
    // 默认选择当前登录的医师（按姓名匹配；医生助理不在列表中则不预选）
    if (!form.value.doctorId) {
      const me = doctors.value.find((d) => d.name === userStore.displayName)
      if (me?.id) form.value.doctorId = me.id
    }
  } catch {
    doctors.value = []
  }
}

async function loadDepartmentStandard() {
  try {
    const list = await departmentApi.getDepartmentList(userStore.storeId)
    const firstEnabled = (list || []).find((d) => d.status === 1)
    departmentStandard.value = Number(firstEnabled?.chargeStandard) || 0
  } catch {
    departmentStandard.value = 0
  }
}

async function submit() {
  if (!form.value.doctorId) {
    toast('请选择开单医师')
    return
  }
  if (!form.value.changeCount || form.value.changeCount < 1) {
    toast('请填写授权次数')
    return
  }
  if (form.value.paymentAmount == null || form.value.paymentAmount < 0) {
    toast('请填写缴费金额')
    return
  }
  submitting.value = true
  try {
    await childApi.grantServiceRecord(childId.value, {
      changeCount: form.value.changeCount,
      paymentAmount: form.value.paymentAmount,
      paymentMethod: form.value.paymentMethod,
      doctorId: form.value.doctorId,
      doctorName: doctorName.value,
      remark: form.value.remark || undefined,
    })
    capture()
    toast(`已预约授权 ${form.value.changeCount} 次`)
    leaving.value = true
    setTimeout(() => backAfterDone(), 500)
  } finally {
    submitting.value = false
  }
}

function backAfterDone() {
  if (from.value === 'home') {
    uni.switchTab({ url: '/pages/home/index' })
  } else if (from.value === 'child') {
    uni.switchTab({ url: '/pages/child/index' })
  } else {
    uni.navigateBack()
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
  childId.value = Number(options?.id || 0)
  from.value = String(options?.from || '')
  if (!childId.value) return
  await loadDoctors()
  child.value = await childApi.getChildDetail(childId.value)
  // 先取科室收费标准，再按默认值（1 次 × 自费支付）计算金额
  await loadDepartmentStandard()
  calcPaymentAmount()
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

.hero {
  padding: 32rpx;
  background: linear-gradient(135deg, #60a5fa, #2563eb, #1d4ed8);
}

.hero-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
}

.hero-phone {
  font-size: 26rpx;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.85);
}

.hero-remain-row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: 20rpx;
}

.hero-remain-label {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-remain {
  font-size: 44rpx;
  font-weight: 700;
  color: #fff;
}

.hero-remain-unit {
  font-size: 22rpx;
  font-weight: 400;
  margin-left: 4rpx;
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

.field-input {
  flex: 1;
  font-size: 32rpx;
  font-weight: 600;
  color: #1d4ed8;
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

.stepper {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: flex-end;
}

.stepper-btn {
  width: 64rpx;
  height: 64rpx;
  line-height: 60rpx;
  text-align: center;
  font-size: 36rpx;
  color: #2563eb;
  background: #f1f5f9;
  border-radius: 10rpx;
}

.stepper-btn.is-off {
  color: #cbd5e1;
}

.stepper-input {
  width: 110rpx;
  height: 64rpx;
  margin: 0 16rpx;
  text-align: center;
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  background: #f8fafc;
  border-radius: 10rpx;
}

.stepper-unit {
  margin-left: 12rpx;
  font-size: 26rpx;
  color: #64748b;
}

.amount-wrap {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: flex-end;
}

.amount-unit {
  margin-left: 10rpx;
  font-size: 26rpx;
  color: #64748b;
}

.amount-tip {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
