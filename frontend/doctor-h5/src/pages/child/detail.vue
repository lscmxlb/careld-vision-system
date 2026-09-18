<template>
  <view class="page">
    <view v-if="detail" class="hero">
      <view class="hero-top">
        <view class="hero-name-row">
          <text class="hero-name">{{ detail.name || '未命名' }}</text>
          <text class="tag" :class="auditTag(detail.auditStatus)">{{ auditLabel(detail.auditStatus) }}</text>
          <text v-if="detail.status === 0" class="tag tag-danger">已禁用</text>
        </view>
        <text class="hero-remain">{{ detail.remainingCount ?? 0 }}<text class="hero-remain-unit">次</text></text>
      </view>
      <view class="hero-meta">
        <text>{{ detail.gender === 1 ? '男孩' : detail.gender === 0 ? '女孩' : '未知' }}</text>
        <text class="hero-dot">·</text>
        <text>{{ ageLabel }}</text>
        <text class="hero-dot">·</text>
        <text>{{ detail.phone || '—' }}</text>
      </view>
      <view v-if="detail.auditStatus === 2 && detail.auditRemark" class="hero-reject">驳回原因：{{ detail.auditRemark }}</view>
    </view>

    <view class="tabs">
      <view v-for="tab in TABS" :key="tab" class="tab" :class="{ 'tab-active': activeTab === tab }" @click="activeTab = tab">
        {{ tab }}
      </view>
    </view>

    <!-- 基本信息 -->
    <view v-if="activeTab === '基本信息'" class="tab-body">
      <view v-if="detail" class="card">
        <view class="section-title">档案信息</view>
        <view class="kv"><text class="kv-key">档案编号</text><text class="kv-value">{{ detail.childCode || '—' }}</text></view>
        <view class="kv"><text class="kv-key">出生日期</text><text class="kv-value">{{ detail.birthDate || '—' }}</text></view>
        <view class="kv"><text class="kv-key">所在学校</text><text class="kv-value">{{ detail.school || '—' }}</text></view>
        <view class="kv"><text class="kv-key">家长姓名</text><text class="kv-value">{{ detail.parentName || '—' }}</text></view>
        <view class="kv"><text class="kv-key">关系</text><text class="kv-value">{{ detail.relation || '—' }}</text></view>
        <view class="kv"><text class="kv-key">家庭地址</text><text class="kv-value">{{ detail.homeAddress || '—' }}</text></view>
        <view class="kv"><text class="kv-key">建档日期</text><text class="kv-value">{{ (detail.createdAt || '').slice(0, 10) || '—' }}</text></view>
        <view class="kv"><text class="kv-key">主治医师</text><text class="kv-value">{{ detail.doctorName || '—' }}</text></view>
        <view class="kv"><text class="kv-key">来源</text><text class="kv-value">{{ detail.sourceType === 1 ? '家长自建' : '门店建档' }}</text></view>
        <view class="kv"><text class="kv-key">养护次数</text><text class="kv-value">{{ detail.careCount ?? 0 }}</text></view>
      </view>

      <view v-if="detail" class="card">
        <view class="section-title">视力情况</view>
        <view class="kv"><text class="kv-key">视力状况</text><text class="kv-value">{{ detail.eyeCondition || '—' }}</text></view>
        <view class="vision-row">
          <view class="vision-item">
            <text class="vision-label">双眼</text>
            <text class="vision-value">{{ displayVision(detail.nakedVisionBoth) }}</text>
          </view>
          <view class="vision-item">
            <text class="vision-label">左眼</text>
            <text class="vision-value">{{ displayVision(detail.nakedVisionLeft) }}</text>
          </view>
          <view class="vision-item">
            <text class="vision-label">右眼</text>
            <text class="vision-value">{{ displayVision(detail.nakedVisionRight) }}</text>
          </view>
        </view>
      </view>

      <view v-if="detail" class="card">
        <view class="section-title">其它信息</view>
        <view class="kv"><text class="kv-key">分娩方式</text><text class="kv-value">{{ detail.deliveryType || '—' }}</text></view>
        <view class="kv">
          <text class="kv-key">日常作息</text>
          <text class="kv-value">{{ detail.bedtime || '—' }} 至 {{ detail.wakeTime || '—' }}</text>
        </view>
        <view class="kv-block"><text class="kv-key">既往病史</text><text class="kv-text">{{ detail.medicalHistory || '—' }}</text></view>
        <view class="kv-block"><text class="kv-key">过敏信息</text><text class="kv-text">{{ detail.allergyInfo || '—' }}</text></view>
      </view>
    </view>

    <!-- 服务记录 -->
    <view v-else-if="activeTab === '服务记录'" class="tab-body">
      <view v-if="serviceRecords.length" class="card">
        <view v-for="item in serviceRecords" :key="item.id" class="record-row">
          <view class="record-left">
            <view class="record-head">
              <text class="tag" :class="item.changeCount > 0 ? 'tag-success' : 'tag-danger'">
                {{ changeTypeLabel(item.changeType) }}
              </text>
              <text class="record-count" :class="item.changeCount > 0 ? 'up' : 'down'">
                {{ item.changeCount > 0 ? '+' : '' }}{{ item.changeCount }}
              </text>
            </view>
            <view class="record-meta">
              <text>{{ formatDateTime(item.createdAt) }}</text>
              <text v-if="item.doctorName" class="record-dot">·</text>
              <text v-if="item.doctorName">{{ item.doctorName }}</text>
            </view>
            <view v-if="item.paymentMethod || item.paymentAmount != null" class="record-meta">
              <text>{{ item.paymentMethod || '—' }}</text>
              <text class="record-dot">·</text>
              <text>¥{{ item.paymentAmount ?? 0 }}</text>
            </view>
            <view v-if="item.remark" class="record-remark">{{ item.remark }}</view>
          </view>
          <view class="record-right">
            <text class="record-after">{{ item.remainingAfter ?? '—' }}</text>
            <text class="record-after-label">剩余</text>
          </view>
        </view>
      </view>
      <view v-else class="empty"><text class="empty-icon">📄</text><text class="empty-text">暂无服务记录</text></view>
    </view>

    <!-- 预约记录 -->
    <view v-else-if="activeTab === '预约记录'" class="tab-body">
      <view v-if="reserves.length" class="list">
        <ReserveCard v-for="row in reserves" :key="row.id" :row="row" show-date readonly @detail="goReserveDetail" />
      </view>
      <view v-else class="empty"><text class="empty-icon">📅</text><text class="empty-text">暂无预约记录</text></view>
    </view>

    <!-- 养护记录 -->
    <view v-else class="tab-body">
      <view v-if="careRecords.length" class="list">
        <view v-for="row in careRecords" :key="row.id" class="card care-card" @click="goCareDetail(row)">
          <view class="care-head">
            <text class="care-date">{{ row.careDate }}</text>
            <text class="tag" :class="CARE_STATUS_MAP[row.status]?.tag || 'tag-grey'">
              {{ CARE_STATUS_MAP[row.status]?.label || '未知' }}
            </text>
          </view>
          <view class="care-meta">
            <text v-if="row.timeSlot">{{ row.timeSlot }}</text>
            <text v-if="row.timeSlot" class="care-dot">·</text>
            <text>{{ row.executorName || '—' }}</text>
          </view>
          <view class="care-vision">
            <view class="care-vision-item">
              <text class="care-vision-label">左眼</text>
              <text class="care-vision-value">{{ displayVision(row.visionBeforeLeft) }} → {{ displayVision(row.visionAfterLeft) }}</text>
            </view>
            <view class="care-vision-item">
              <text class="care-vision-label">右眼</text>
              <text class="care-vision-value">{{ displayVision(row.visionBeforeRight) }} → {{ displayVision(row.visionAfterRight) }}</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty"><text class="empty-icon">📈</text><text class="empty-text">暂无养护记录</text></view>
    </view>

    <view v-if="detail" class="sticky-bar">
      <view v-if="canAudit" class="btn btn-outline" @click="openAudit">审核</view>
      <view class="btn btn-plain" @click="goEdit">编辑档案</view>
      <view class="btn btn-outline" @click="toggleStatus">{{ detail.status === 1 ? '禁用' : '启用' }}</view>
      <view class="btn btn-primary" @click="goGrant">预约授权</view>
    </view>

    <view v-if="auditVisible" class="mask" @click="auditVisible = false">
      <view class="sheet" @click.stop>
        <view class="sheet-header">
          <text class="sheet-title">档案审核 - {{ detail?.name }}</text>
          <text class="sheet-close" @click="auditVisible = false">✕</text>
        </view>
        <view class="sheet-body">
          <view class="field">
            <text class="field-label required">审核结果</text>
            <view class="audit-options">
              <view class="audit-option" :class="{ 'audit-option-active': auditForm.auditStatus === 1 }" @click="auditForm.auditStatus = 1">通过</view>
              <view class="audit-option" :class="{ 'audit-option-active': auditForm.auditStatus === 2 }" @click="auditForm.auditStatus = 2">驳回</view>
            </view>
          </view>
          <view v-if="auditForm.auditStatus === 1" class="field">
            <text class="field-label required">主治医生</text>
            <picker mode="selector" :range="doctorNames" :value="doctorIndex" @change="onDoctorChange">
              <view class="field-value" :class="{ 'field-placeholder': !auditForm.doctorId }">
                {{ doctorName || '请选择主治医生' }}
              </view>
            </picker>
          </view>
          <view class="field">
            <text class="field-label" :class="{ required: auditForm.auditStatus === 2 }">审核备注</text>
            <textarea v-model="auditForm.auditRemark" class="audit-textarea" placeholder="驳回时请填写原因" placeholder-class="field-placeholder" maxlength="200" />
          </view>
        </view>
        <view class="sheet-footer">
          <view class="btn btn-plain" @click="auditVisible = false">取消</view>
          <view class="btn btn-primary" :class="{ 'is-disabled': auditLoading }" @click="submitAudit">
            {{ auditLoading ? '提交中…' : '确定' }}
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { medicalStaffApi, reserveApi } from '@/api/reserve'
import { careRecordApi } from '@/api/care-record'
import { useUserStore } from '@/stores/user'
import { ageText, displayVision, formatDateTime } from '@/utils/format'
import { AUDIT_STATUS_MAP, CARE_STATUS_MAP, CHANGE_TYPE_MAP } from '@/utils/dict'
import ReserveCard from '@/components/ReserveCard.vue'
import type { CareRecord, Child, ChildServiceRecord, MedicalStaff, Reserve } from '@/types'

const TABS = ['基本信息', '服务记录', '预约记录', '养护记录'] as const

const userStore = useUserStore()

const childId = ref(0)
const detail = ref<Child | null>(null)
const serviceRecords = ref<ChildServiceRecord[]>([])
const reserves = ref<Reserve[]>([])
const careRecords = ref<CareRecord[]>([])
const activeTab = ref<string>('基本信息')

const ageLabel = computed(() => ageText(detail.value?.birthDate, detail.value?.age) || '年龄未知')
const canAudit = computed(() => detail.value?.sourceType === 1 && detail.value?.auditStatus === 0)

function auditTag(status: number) {
  return AUDIT_STATUS_MAP[status]?.tag || 'tag-grey'
}

function auditLabel(status: number) {
  return AUDIT_STATUS_MAP[status]?.label || '未知'
}

function changeTypeLabel(type: number) {
  return CHANGE_TYPE_MAP[type]?.label || '未知'
}

async function loadDetail() {
  detail.value = await childApi.getChildDetail(childId.value)
  uni.setNavigationBarTitle({ title: detail.value.name || '儿童档案' })
}

async function loadServiceRecords() {
  serviceRecords.value = (await childApi.getServiceRecords(childId.value)) || []
}

async function loadReserves() {
  const res = await reserveApi.getReserveList({ childId: childId.value, storeId: userStore.storeId, page: 1, size: 100 })
  reserves.value = res?.list || []
}

async function loadCareRecords() {
  careRecords.value = (await careRecordApi.getRecordsByChild(childId.value, userStore.storeId)) || []
}

function loadAll() {
  if (!childId.value) return
  loadDetail()
  loadServiceRecords()
  loadReserves()
  loadCareRecords()
}

function goEdit() {
  uni.navigateTo({ url: `/pages/child/form?id=${childId.value}` })
}

function goGrant() {
  uni.navigateTo({ url: `/pages/child/grant?id=${childId.value}` })
}

/* ---------------- 档案审核（家长自建档案） ---------------- */
const auditVisible = ref(false)
const auditLoading = ref(false)
const doctors = ref<MedicalStaff[]>([])
const auditForm = ref({ auditStatus: 1, auditRemark: '', doctorId: undefined as number | undefined })

const doctorNames = computed(() => doctors.value.map((d) => d.name))
const doctorName = computed(() => doctors.value.find((d) => d.id === auditForm.value.doctorId)?.name || '')
const doctorIndex = computed(() => Math.max(0, doctors.value.findIndex((d) => d.id === auditForm.value.doctorId)))

async function loadDoctors() {
  try {
    const res = await medicalStaffApi.getStaffList({ staffRole: 1, status: 1, page: 1, size: 200 })
    doctors.value = res?.list || []
  } catch {
    doctors.value = []
  }
}

function openAudit() {
  auditForm.value = { auditStatus: 1, auditRemark: '', doctorId: undefined }
  loadDoctors()
  auditVisible.value = true
}

function onDoctorChange(e: any) {
  const index = Number(e.detail.value)
  auditForm.value.doctorId = doctors.value[index]?.id
}

async function submitAudit() {
  if (auditForm.value.auditStatus === 2 && !auditForm.value.auditRemark.trim()) {
    uni.showToast({ title: '驳回时请填写审核备注', icon: 'none' })
    return
  }
  if (auditForm.value.auditStatus === 1 && !auditForm.value.doctorId) {
    uni.showToast({ title: '审核通过前请指定主治医生', icon: 'none' })
    return
  }
  auditLoading.value = true
  try {
    const passed = auditForm.value.auditStatus === 1
    await childApi.auditChild(childId.value, {
      auditStatus: auditForm.value.auditStatus,
      auditRemark: auditForm.value.auditRemark || undefined,
      doctorId: passed ? auditForm.value.doctorId : undefined,
      doctorName: passed ? doctorName.value : undefined,
    })
    uni.showToast({ title: passed ? '审核已通过' : '已驳回', icon: 'none' })
    auditVisible.value = false
    loadAll()
  } finally {
    auditLoading.value = false
  }
}

function goReserveDetail(row: Reserve) {
  uni.navigateTo({ url: `/pages/reserve/detail?id=${row.id}` })
}

function goCareDetail(row: CareRecord) {
  uni.navigateTo({ url: `/pages/care-record/detail?id=${row.id}` })
}

function toggleStatus() {
  if (!detail.value) return
  const next = detail.value.status === 1 ? 0 : 1
  const action = next === 0 ? '禁用' : '启用'
  uni.showModal({
    title: `${action}档案`,
    content: `确认${action}档案「${detail.value.name}」吗？${next === 0 ? '禁用后该档案将从列表隐藏且无法预约。' : ''}`,
    confirmText: action,
    success: async (res) => {
      if (!res.confirm) return
      await childApi.setChildStatus(childId.value, next)
      uni.showToast({ title: `${action}成功`, icon: 'none' })
      loadAll()
    },
  })
}

onLoad((options) => {
  childId.value = Number(options?.id || 0)
})

onShow(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 160rpx;
}

.hero {
  padding: 32rpx 32rpx 36rpx;
  background: linear-gradient(135deg, #60a5fa, #2563eb, #1d4ed8);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-name-row {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.hero-name {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
  margin-right: 16rpx;
  max-width: 320rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-remain {
  font-size: 48rpx;
  font-weight: 700;
  color: #fff;
}

.hero-remain-unit {
  font-size: 24rpx;
  font-weight: 400;
  margin-left: 4rpx;
}

.hero-meta {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-dot {
  margin: 0 12rpx;
  color: rgba(255, 255, 255, 0.5);
}

.hero-reject {
  margin-top: 20rpx;
  padding: 14rpx 20rpx;
  font-size: 24rpx;
  color: #fff;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10rpx;
}

.hero .tag {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.tabs {
  display: flex;
  background: #fff;
  position: sticky;
  top: 0;
  z-index: 10;
}

.tab {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  color: #64748b;
  position: relative;
}

.tab-active {
  color: #2563eb;
  font-weight: 600;
}

.tab-active::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 6rpx;
  width: 48rpx;
  height: 6rpx;
  margin-left: -24rpx;
  background: #2563eb;
  border-radius: 4rpx;
}

.tab-body {
  padding: 20rpx 24rpx;
}

.tab-body .list {
  padding: 0;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16rpx;
}

.kv {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 0;
  font-size: 28rpx;
}

.kv-key {
  width: 160rpx;
  flex: none;
  color: #94a3b8;
}

.kv-value {
  flex: 1;
  color: #1e293b;
}

.kv-block {
  display: flex;
  flex-direction: column;
  padding: 12rpx 0;
  font-size: 28rpx;
}

.kv-block .kv-key {
  width: auto;
  margin-bottom: 8rpx;
}

.kv-text {
  color: #1e293b;
  line-height: 1.6;
}

.vision-row {
  display: flex;
  margin-top: 16rpx;
}

.vision-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 0;
  background: #f8fafc;
  border-radius: 12rpx;
  margin-right: 12rpx;
}

.vision-item:last-child {
  margin-right: 0;
}

.vision-label {
  font-size: 24rpx;
  color: #94a3b8;
}

.vision-value {
  margin-top: 8rpx;
  font-size: 34rpx;
  font-weight: 600;
  color: #0f172a;
}

.record-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
}

.record-row:last-child {
  border-bottom: none;
}

.record-left {
  flex: 1;
  min-width: 0;
}

.record-head {
  display: flex;
  align-items: center;
}

.record-count {
  margin-left: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
}

.record-count.up {
  color: #2563eb;
}

.record-count.down {
  color: #ef4444;
}

.record-meta {
  display: flex;
  align-items: center;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #64748b;
}

.record-dot {
  margin: 0 8rpx;
  color: #cbd5e1;
}

.record-remark {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #94a3b8;
}

.record-right {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: none;
  margin-left: 20rpx;
}

.record-after {
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}

.record-after-label {
  font-size: 22rpx;
  color: #94a3b8;
}

.care-card {
  margin-bottom: 20rpx;
}

.care-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.care-date {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
}

.care-meta {
  display: flex;
  align-items: center;
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #64748b;
}

.care-dot {
  margin: 0 10rpx;
  color: #cbd5e1;
}

.care-vision {
  display: flex;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #f1f5f9;
}

.care-vision-item {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.care-vision-label {
  font-size: 24rpx;
  color: #94a3b8;
}

.care-vision-value {
  margin-top: 6rpx;
  font-size: 28rpx;
  color: #1e293b;
}

.audit-options {
  display: flex;
}

.audit-option {
  padding: 14rpx 48rpx;
  margin-right: 20rpx;
  font-size: 28rpx;
  color: #475569;
  background: #f1f5f9;
  border-radius: 12rpx;
}

.audit-option-active {
  color: #2563eb;
  background: #dbeafe;
  font-weight: 600;
}

.audit-textarea {
  width: 100%;
  height: 140rpx;
  padding: 16rpx;
  font-size: 28rpx;
  color: #1e293b;
  background: #f8fafc;
  border-radius: 12rpx;
  box-sizing: border-box;
}
</style>
