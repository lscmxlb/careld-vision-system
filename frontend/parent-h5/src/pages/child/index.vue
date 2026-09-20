<template>
  <view class="page">
    <view v-if="list.length" class="list">
      <view v-for="item in list" :key="item.id" class="card child-card">
        <view class="child-head">
          <view class="child-name-row">
            <text class="child-name" :class="genderNameClass(item.gender)">{{ item.name || '未命名' }}</text>
            <text v-if="item.auditStatus !== 1" class="tag" :class="auditTag(item.auditStatus)">
              {{ auditLabel(item.auditStatus) }}
            </text>
          </view>
          <view class="remain">
            <text class="remain-label">养护</text>
            <text class="remain-num">{{ item.careCount ?? 0 }}</text>
            <text class="remain-slash">/</text>
            <text class="remain-label">可用</text>
            <text class="remain-num" :class="{ 'remain-zero': !item.remainingCount }">{{ item.remainingCount ?? 0 }}</text>
          </view>
        </view>

        <view class="child-meta">
          <text>{{ genderText(item.gender) }}</text>
          <text class="dot">·</text>
          <text>{{ ageLabel(item) }}</text>
          <text class="dot">·</text>
          <text>{{ item.phone || '—' }}</text>
        </view>

        <view class="child-store">
          <text class="child-store-key">建档医院</text>
          <text class="child-store-value">{{ item.storeName || '—' }}</text>
        </view>
        <view class="child-store">
          <text class="child-store-key">服务电话</text>
          <text class="child-store-value">{{ item.storeServicePhone || '—' }}</text>
        </view>

        <view v-if="item.auditStatus === 2 && item.auditRemark" class="reject-tip">
          驳回原因：{{ item.auditRemark }}
        </view>
        <view v-if="item.auditStatus === 0" class="pending-tip">
          档案待医院审核，审核通过后即可预约养护
        </view>
        <view v-else-if="item.auditStatus === 1 && !item.remainingCount" class="pending-tip pending-tip-warn">
          可用次数不足，请联系医院授权预约次数
        </view>

        <view class="child-actions">
          <view class="btn btn-sm btn-plain" @click="goDetail(item)">档案详情</view>
          <view
            class="btn btn-sm btn-primary"
            :class="{ 'is-disabled': !canReserve(item) }"
            @click="goReserve(item)"
          >
            立即预约
          </view>
        </view>
      </view>

      <view class="loading-tip">— 共 {{ list.length }} 个儿童 —</view>
    </view>

    <view v-else-if="!loaded && loading" class="loading-tip">加载中…</view>
    <view v-else class="empty">
      <text class="empty-icon">👶</text>
      <text class="empty-text">还没有添加任何档案</text>
      <view class="empty-btn" @click="goCreate">添加儿童档案</view>
    </view>

    <view v-if="list.length" class="fab" @click="goCreate">＋ 添加儿童档案</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { useUserStore } from '@/stores/user'
import { ageText } from '@/utils/format'
import { AUDIT_STATUS_MAP } from '@/utils/dict'
import { toast } from '@/utils/request'
import { setReserveChildId } from '@/utils/reserveIntent'
import type { Child } from '@/types'

const userStore = useUserStore()

const list = ref<Child[]>([])
const loading = ref(false)
const loaded = ref(false)

function genderText(gender: number) {
  return gender === 1 ? '男' : gender === 0 ? '女' : '未知'
}

/** 姓名按性别着色：男孩蓝 #2563eb / 女孩粉 #ec4899 */
function genderNameClass(gender: number) {
  return gender === 1 ? 'name-boy' : gender === 0 ? 'name-girl' : ''
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

/** 预约门控：已通过审核且剩余次数 > 0 */
function canReserve(item: Child) {
  return item.auditStatus === 1 && (item.remainingCount ?? 0) > 0
}

async function load() {
  if (!userStore.userId) return
  loading.value = true
  try {
    const res = await childApi.getChildList({ parentUserId: userStore.userId })
    list.value = res || []
    loaded.value = true
  } finally {
    loading.value = false
  }
}

function goDetail(item: Child) {
  uni.navigateTo({ url: `/pages/child/detail?id=${item.id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/child/form' })
}

function goReserve(item: Child) {
  if (item.auditStatus === 0) {
    toast('档案待医院审核，审核通过后即可预约')
    return
  }
  if (item.auditStatus === 2) {
    toast('档案已被驳回，请修改后重新提交')
    return
  }
  if (!item.remainingCount) {
    toast('可用次数不足，请联系医院授权预约次数')
    return
  }
  setReserveChildId(item.id)
  uni.switchTab({ url: '/pages/appointment/index' })
}

onShow(async () => {
  // 登录后自动认领同手机号档案，再拉取列表
  await userStore.claimMyChildren()
  load()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  padding-bottom: 180rpx;
}

.list {
  padding: 20rpx 24rpx;
}

.child-card {
  margin-bottom: 20rpx;
}

.child-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.child-name-row {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.child-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #0f172a;
  margin-right: 12rpx;
  max-width: 300rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name-boy {
  color: #2563eb;
}

.name-girl {
  color: #ec4899;
}

.remain {
  display: flex;
  align-items: baseline;
  flex: none;
  margin-left: 12rpx;
}

.remain-label {
  font-size: 22rpx;
  color: #94a3b8;
}

.remain-num {
  margin-left: 4rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: #14b8a6;
}

.remain-slash {
  margin: 0 8rpx;
  font-size: 26rpx;
  color: #cbd5e1;
}

.remain-zero {
  color: #cbd5e1;
}

.child-meta {
  display: flex;
  align-items: center;
  margin-top: 14rpx;
  font-size: 26rpx;
  color: #64748b;
}

.dot {
  margin: 0 10rpx;
  color: #cbd5e1;
}

.child-store {
  display: flex;
  align-items: center;
  margin-top: 10rpx;
  font-size: 26rpx;
}

.child-store-key {
  flex: none;
  color: #94a3b8;
  margin-right: 12rpx;
}

.child-store-value {
  flex: 1;
  min-width: 0;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reject-tip {
  margin-top: 14rpx;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 8rpx;
}

.pending-tip {
  margin-top: 14rpx;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  color: #b45309;
  background: #fffbeb;
  border-radius: 8rpx;
}

.pending-tip-warn {
  color: #b91c1c;
  background: #fef2f2;
}

.child-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f1f5f9;
}

.child-actions .btn {
  margin-left: 16rpx;
}

.empty-btn {
  margin-top: 32rpx;
  padding: 20rpx 64rpx;
  font-size: 28rpx;
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
  border-radius: 44rpx;
}

.fab {
  position: fixed;
  right: 32rpx;
  bottom: calc(env(safe-area-inset-bottom) + 140rpx);
  padding: 22rpx 40rpx;
  font-size: 28rpx;
  color: #fff;
  background: linear-gradient(135deg, #2dd4bf, #14b8a6);
  border-radius: 50rpx;
  box-shadow: 0 8rpx 24rpx rgba(20, 184, 166, 0.35);
  z-index: 20;
}
</style>
