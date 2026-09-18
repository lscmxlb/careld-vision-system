<template>
  <view class="page">
    <template v-if="child">
      <view class="hero">
        <view class="hero-top">
          <text class="hero-name">{{ child.name || '未命名' }}</text>
          <text class="tag" :class="auditTag(child.auditStatus)">{{ auditLabel(child.auditStatus) }}</text>
        </view>
        <view class="hero-meta">
          <text>{{ genderText(child.gender) }}</text>
          <text class="hero-dot">·</text>
          <text>{{ ageLabel(child) }}</text>
          <text class="hero-dot">·</text>
          <text>{{ child.phone || '—' }}</text>
        </view>
        <view class="hero-remain">
          可用次数 <text class="hero-remain-num">{{ child.remainingCount ?? 0 }}</text> 次
        </view>
      </view>

      <view v-if="child.auditStatus === 2 && child.auditRemark" class="reject-tip">
        驳回原因：{{ child.auditRemark }}
      </view>
      <view v-else-if="child.auditStatus === 0" class="pending-tip">
        档案待医院审核，审核通过后即可预约养护
      </view>

      <view class="card">
        <view class="section-title">档案信息</view>
        <view class="kv"><text class="kv-key">档案编号</text><text class="kv-value">{{ child.childCode || '—' }}</text></view>
        <view class="kv"><text class="kv-key">儿童姓名</text><text class="kv-value">{{ child.name || '—' }}</text></view>
        <view class="kv"><text class="kv-key">所属医院</text><text class="kv-value">{{ child.storeName || '—' }}</text></view>
        <view class="kv"><text class="kv-key">主治医师</text><text class="kv-value">{{ child.doctorName || '—' }}</text></view>
        <view class="kv"><text class="kv-key">性别</text><text class="kv-value">{{ genderText(child.gender) }}</text></view>
        <view class="kv"><text class="kv-key">出生日期</text><text class="kv-value">{{ child.birthDate || '—' }}</text></view>
        <view class="kv"><text class="kv-key">家长姓名</text><text class="kv-value">{{ child.parentName || '—' }}</text></view>
        <view class="kv"><text class="kv-key">关系</text><text class="kv-value">{{ child.relation || '—' }}</text></view>
        <view class="kv"><text class="kv-key">手机号码</text><text class="kv-value">{{ child.phone || '—' }}</text></view>
        <view class="kv"><text class="kv-key">所在学校</text><text class="kv-value">{{ child.school || '—' }}</text></view>
        <view class="kv"><text class="kv-key">家庭地址</text><text class="kv-value">{{ child.homeAddress || '—' }}</text></view>
      </view>

      <view class="card">
        <view class="section-title">视力情况</view>
        <view class="kv"><text class="kv-key">视力状况</text><text class="kv-value">{{ child.eyeCondition || '—' }}</text></view>
        <view class="kv">
          <text class="kv-key">裸眼视力</text>
          <text class="kv-value">
            双眼 {{ displayVision(child.nakedVisionBoth) }}　左眼 {{ displayVision(child.nakedVisionLeft) }}　右眼 {{ displayVision(child.nakedVisionRight) }}
          </text>
        </view>
      </view>

      <view class="card">
        <view class="section-title">其它信息</view>
        <view class="kv"><text class="kv-key">既往病史</text><text class="kv-value">{{ child.medicalHistory || '—' }}</text></view>
        <view class="kv"><text class="kv-key">过敏信息</text><text class="kv-value">{{ child.allergyInfo || '—' }}</text></view>
      </view>

      <view class="card entry-card">
        <view class="entry-row" @click="goTrend">
          <text class="entry-label">历次养护视力趋势</text>
          <text class="entry-arrow">›</text>
        </view>
        <view class="entry-row" @click="goReport">
          <text class="entry-label">养护检测报告</text>
          <text class="entry-arrow">›</text>
        </view>
      </view>
    </template>

    <view v-else-if="loading" class="loading-tip">加载中…</view>

    <view class="sticky-bar">
      <view class="btn btn-plain" @click="goRecords">养护记录</view>
      <view
        class="btn btn-primary"
        :class="{ 'is-disabled': !canReserve }"
        @click="goReserve"
      >
        立即预约
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { ageText, displayVision } from '@/utils/format'
import { AUDIT_STATUS_MAP } from '@/utils/dict'
import { toast } from '@/utils/request'
import { setReserveChildId } from '@/utils/reserveIntent'
import type { Child } from '@/types'

const childId = ref(0)
const child = ref<Child | null>(null)
const loading = ref(false)

const canReserve = computed(() => child.value?.auditStatus === 1 && (child.value?.remainingCount ?? 0) > 0)

function genderText(gender: number) {
  return gender === 1 ? '男' : gender === 0 ? '女' : '未知'
}

function ageLabel(item: Child) {
  return ageText(item.birthDate, item.age) || '年龄未知'
}

function auditTag(status: number) {
  return AUDIT_STATUS_MAP[status]?.tag || 'tag-grey'
}

function auditLabel(status: number) {
  return AUDIT_STATUS_MAP[status]?.label || '未知'
}

async function load() {
  if (!childId.value) return
  loading.value = true
  try {
    child.value = await childApi.getChildDetail(childId.value)
  } finally {
    loading.value = false
  }
}

function reserveTip() {
  if (child.value?.auditStatus === 0) return '档案待医院审核，暂不可预约'
  if (child.value?.auditStatus === 2) return '档案已被驳回，暂不可预约'
  return '可用次数不足，请联系医院前台'
}

function goReserve() {
  if (!canReserve.value) {
    toast(reserveTip())
    return
  }
  setReserveChildId(childId.value)
  uni.switchTab({ url: '/pages/appointment/index' })
}

function goTrend() {
  uni.navigateTo({ url: `/pages/trend/index?childId=${childId.value}` })
}

function goReport() {
  uni.navigateTo({ url: `/pages/report/index?childId=${childId.value}` })
}

function goRecords() {
  uni.switchTab({ url: '/pages/care-record/index' })
}

onLoad((options) => {
  childId.value = Number(options?.id || 0)
})

onShow(load)
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 200rpx;
}

.hero {
  padding: 32rpx;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6, #0d9488);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-name {
  font-size: 38rpx;
  font-weight: 700;
  color: #fff;
}

.hero .tag {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.hero-meta {
  display: flex;
  align-items: center;
  margin-top: 14rpx;
  font-size: 27rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-dot {
  margin: 0 12rpx;
  color: rgba(255, 255, 255, 0.5);
}

.hero-remain {
  margin-top: 22rpx;
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.9);
}

.hero-remain-num {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
  margin: 0 6rpx;
}

.reject-tip {
  margin: 20rpx 24rpx 0;
  padding: 16rpx 20rpx;
  font-size: 25rpx;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 12rpx;
}

.pending-tip {
  margin: 20rpx 24rpx 0;
  padding: 16rpx 20rpx;
  font-size: 25rpx;
  color: #b45309;
  background: #fffbeb;
  border-radius: 12rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 16rpx;
}

.kv {
  display: flex;
  align-items: flex-start;
  padding: 12rpx 0;
  font-size: 27rpx;
}

.kv-key {
  width: 190rpx;
  flex: none;
  color: #94a3b8;
}

.kv-value {
  flex: 1;
  color: #1e293b;
}

.entry-card {
  padding: 8rpx 26rpx;
}

.entry-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f1f5f9;
}

.entry-row:last-child {
  border-bottom: none;
}

.entry-label {
  font-size: 28rpx;
  color: #1e293b;
}

.entry-arrow {
  font-size: 34rpx;
  color: #cbd5e1;
}
</style>
