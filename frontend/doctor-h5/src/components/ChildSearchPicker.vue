<template>
  <view class="search-wrap">
    <view class="search">
      <text class="search-icon">🔍</text>
      <input
        v-model="keyword"
        class="search-input"
        :placeholder="placeholder || '输入姓名或手机号查找儿童'"
        placeholder-class="ph"
        confirm-type="search"
        @input="onInput"
      />
      <text v-if="keyword" class="search-clear" @click="clear">✕</text>
    </view>

    <!-- 匹配结果下拉：仅在录入关键字后展示 -->
    <view v-if="visible" class="dropdown">
      <view v-if="loading" class="dd-tip">查询中…</view>
      <scroll-view v-else scroll-y class="dd-scroll">
        <view v-for="item in matches" :key="item.id" class="dd-item" @click="pick(item)">
          <view class="dd-main">
            <view class="cr-name-row">
              <text class="cr-name">{{ item.name }}</text>
              <text class="cr-meta">{{ genderText(item.gender) }} · {{ ageText(item.birthDate, item.age) }}</text>
            </view>
            <text class="cr-phone">{{ item.phone || '-' }}</text>
          </view>
          <text class="tag" :class="(item.remainingCount || 0) > 0 ? 'tag-primary' : 'tag-grey'">
            剩余 {{ item.remainingCount || 0 }} 次
          </text>
        </view>
        <view v-if="!matches.length" class="dd-tip">没有匹配的档案（仅显示已审核且启用的儿童）</view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { childApi } from '@/api/child'
import { useUserStore } from '@/stores/user'
import { ageText } from '@/utils/format'
import type { Child } from '@/types'

defineProps<{ placeholder?: string }>()

const emit = defineEmits<{
  (e: 'select', child: Child | null): void
}>()

const userStore = useUserStore()

const keyword = ref('')
const matches = ref<Child[]>([])
const loading = ref(false)
const visible = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

function genderText(gender?: number) {
  return gender === 1 ? '男' : gender === 0 ? '女' : '未填'
}

function onInput() {
  if (timer) clearTimeout(timer)
  const kw = keyword.value.trim()
  if (!kw) {
    matches.value = []
    visible.value = false
    loading.value = false
    return
  }
  loading.value = true
  visible.value = true
  timer = setTimeout(() => search(kw), 300)
}

async function search(kw: string) {
  try {
    const list = await childApi.pickOptions({ storeId: userStore.storeId, keyword: kw })
    if (keyword.value.trim() !== kw) return
    matches.value = list
  } catch {
    matches.value = []
  } finally {
    loading.value = false
  }
}

/** 选中后把姓名、手机号回填到查找框 */
function pick(child: Child) {
  keyword.value = `${child.name || ''} ${child.phone || ''}`.trim()
  matches.value = []
  visible.value = false
  emit('select', child)
}

function clear() {
  keyword.value = ''
  matches.value = []
  visible.value = false
  emit('select', null)
}
</script>

<style lang="scss" scoped>
.search-wrap {
  position: relative;
}

.search {
  display: flex;
  align-items: center;
  height: 72rpx;
  background: #f2f5f8;
  border-radius: 36rpx;
  padding: 0 24rpx;
}

.search-icon {
  font-size: 26rpx;
  margin-right: 12rpx;
}

.search-input {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  color: #1f2937;
  height: 100%;
}

.ph {
  color: #b7bfc9;
  font-size: 27rpx;
}

.search-clear {
  font-size: 26rpx;
  color: #b7bfc9;
  padding-left: 12rpx;
}

.dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: 84rpx;
  z-index: 30;
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 16rpx;
  box-shadow: 0 12rpx 32rpx rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.dd-scroll {
  max-height: 460rpx;
}

.dd-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 22rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.dd-item:last-child {
  border-bottom: none;
}

.dd-main {
  min-width: 0;
  flex: 1;
  margin-right: 16rpx;
}

.dd-tip {
  padding: 32rpx 0;
  text-align: center;
  font-size: 24rpx;
  color: #9ca3af;
}

.cr-name-row {
  display: flex;
  align-items: baseline;
}

.cr-name {
  font-size: 29rpx;
  font-weight: 600;
  color: #1f2937;
  margin-right: 12rpx;
}

.cr-meta {
  font-size: 23rpx;
  color: #9ca3af;
}

.cr-phone {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: #5b6572;
}
</style>
