<template>
  <view class="page">
    <view class="search-bar">
      <view class="search-box">
        <text class="search-icon">🔍</text>
        <input
          v-model="keyword"
          class="search-input"
          type="text"
          placeholder="搜索姓名 / 家长手机号"
          placeholder-class="ph"
          confirm-type="search"
          @confirm="reload"
        />
        <text v-if="keyword" class="search-clear" @click="clearKeyword">✕</text>
      </view>
    </view>

    <view class="filter-row">
      <scroll-view class="chip-scroll" scroll-x :show-scrollbar="false">
        <view class="chip-list">
          <view
            v-for="opt in AUDIT_CHIPS"
            :key="String(opt.value)"
            class="chip"
            :class="{ 'chip-active': activeFilters.includes(opt.value) }"
            @click="toggleFilter(opt.value)"
          >
            {{ opt.label }}
          </view>
        </view>
      </scroll-view>
    </view>

    <view v-if="filteredList.length" class="list">
      <view v-for="item in visibleList" :key="item.id" class="card child-card" @click="goDetail(item)">
        <view class="child-head">
          <view class="child-name-row">
            <text class="child-name" :class="genderNameClass(item.gender)">{{ item.name || '未命名' }}</text>
            <text class="tag" :class="auditTag(item.auditStatus)">{{ auditLabel(item.auditStatus) }}</text>
            <text v-if="item.status === 0" class="tag tag-danger">已禁用</text>
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
          <text>{{ createdLabel(item.createdAt) }}</text>
        </view>

        <view class="child-meta">
          <text>{{ parentLabel(item) }}</text>
          <text class="dot">·</text>
          <text>{{ item.phone || '—' }}</text>
        </view>

        <view v-if="item.auditStatus === 2 && item.auditRemark" class="reject-tip">
          驳回原因：{{ item.auditRemark }}
        </view>
      </view>

      <view v-if="visibleCount < filteredList.length" class="loading-tip" @click="showMore">上拉加载更多（{{ filteredList.length - visibleCount }}）</view>
      <view v-else class="loading-tip">— 共 {{ filteredList.length }} 条 —</view>
    </view>

    <view v-else-if="loading" class="loading-tip">加载中…</view>
    <view v-else class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无儿童档案</text>
    </view>

    <view class="fab" @click="goCreate">＋ 建档</view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { childApi } from '@/api/child'
import { useUserStore } from '@/stores/user'
import { ageText } from '@/utils/format'
import { AUDIT_STATUS_MAP, AUDIT_STATUS_OPTIONS } from '@/utils/dict'
import type { Child } from '@/types'

const PAGE_SIZE = 20

/** 筛选 chip：待审核/已通过/已驳回（审核状态）+ 已禁用（启用状态） */
type AuditChipValue = number | 'disabled'

const AUDIT_CHIPS: { value: AuditChipValue; label: string }[] = [
  ...AUDIT_STATUS_OPTIONS.map((o) => ({ value: o.value as AuditChipValue, label: o.label })),
  { value: 'disabled', label: '已禁用' },
]

/** 进入页面默认显示：待审核 + 已通过 */
const DEFAULT_CHIPS: AuditChipValue[] = [0, 1]

const userStore = useUserStore()

const keyword = ref('')
const activeFilters = ref<AuditChipValue[]>([...DEFAULT_CHIPS])

const list = ref<Child[]>([])
const loading = ref(false)
const visibleCount = ref(PAGE_SIZE)

/** 本地筛选：审核状态 chip 之间为并集，已禁用单独放行 status=0 的记录 */
const filteredList = computed(() => {
  const auditSelected = activeFilters.value.filter((v): v is number => typeof v === 'number')
  const showDisabled = activeFilters.value.includes('disabled')
  return list.value.filter((item) => {
    if (item.status === 0) return showDisabled
    return auditSelected.includes(item.auditStatus)
  })
})

const visibleList = computed(() => filteredList.value.slice(0, visibleCount.value))

function genderText(gender: number) {
  return gender === 1 ? '男孩' : gender === 0 ? '女孩' : '未知'
}

function genderNameClass(gender: number) {
  return gender === 1 ? 'name-boy' : gender === 0 ? 'name-girl' : ''
}

function ageLabel(item: Child) {
  return ageText(item.birthDate, item.age) || '年龄未知'
}

function createdLabel(createdAt?: string) {
  const date = (createdAt || '').slice(0, 10)
  return date ? `${date}建档` : '—'
}

function parentLabel(item: Child) {
  if (!item.parentName) return '—'
  return item.relation ? `${item.parentName}(${item.relation})` : item.parentName
}

function auditTag(status: number) {
  return AUDIT_STATUS_MAP[status]?.tag || 'tag-grey'
}

function auditLabel(status: number) {
  return AUDIT_STATUS_MAP[status]?.label || '未知'
}

async function load() {
  loading.value = true
  try {
    // 拉取含禁用全量，前端按筛选 chip 本地过滤
    const res = await childApi.getChildList({
      storeId: userStore.storeId,
      keyword: keyword.value || undefined,
      includeDisabled: true,
    })
    list.value = res || []
    visibleCount.value = PAGE_SIZE
  } finally {
    loading.value = false
  }
}

function reload() {
  visibleCount.value = PAGE_SIZE
  load()
}

function clearKeyword() {
  keyword.value = ''
  reload()
}

function toggleFilter(value: AuditChipValue) {
  const idx = activeFilters.value.indexOf(value)
  if (idx >= 0) {
    if (activeFilters.value.length === 1) {
      uni.showToast({ title: '至少保留一个筛选状态', icon: 'none' })
      return
    }
    activeFilters.value.splice(idx, 1)
  } else {
    activeFilters.value.push(value)
  }
  visibleCount.value = PAGE_SIZE
}

function showMore() {
  visibleCount.value = Math.min(visibleCount.value + PAGE_SIZE, filteredList.value.length)
}

function goDetail(item: Child) {
  uni.navigateTo({ url: `/pages/child/detail?id=${item.id}` })
}

function goCreate() {
  uni.navigateTo({ url: '/pages/child/form' })
}

onShow(() => {
  reload()
})

onPullDownRefresh(async () => {
  await load()
  uni.stopPullDownRefresh()
})

onReachBottom(showMore)
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 160rpx;
}

.search-bar {
  padding: 20rpx 24rpx 0;
  background: #fff;
}

.search-box {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f4f6f9;
  border-radius: 36rpx;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #1e293b;
}

.ph {
  color: #94a3b8;
}

.search-clear {
  width: 40rpx;
  text-align: center;
  color: #94a3b8;
  font-size: 26rpx;
}

.filter-row {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.chip-scroll {
  flex: 1;
  white-space: nowrap;
}

.chip-list {
  display: inline-flex;
}

.chip {
  display: inline-block;
  padding: 10rpx 26rpx;
  margin-right: 12rpx;
  font-size: 26rpx;
  color: #475569;
  background: #f1f5f9;
  border-radius: 30rpx;
}

.chip-active {
  color: #fff;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
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
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
  margin-right: 12rpx;
  max-width: 260rpx;
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
  color: #2563eb;
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
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #64748b;
}

.dot {
  margin: 0 10rpx;
  color: #cbd5e1;
}

.reject-tip {
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  font-size: 24rpx;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 8rpx;
}

.fab {
  position: fixed;
  right: 32rpx;
  bottom: 60rpx;
  padding: 22rpx 40rpx;
  font-size: 28rpx;
  color: #fff;
  background: linear-gradient(135deg, #60a5fa, #2563eb);
  border-radius: 50rpx;
  box-shadow: 0 8rpx 24rpx rgba(37, 99, 235, 0.35);
  z-index: 20;
}
</style>
