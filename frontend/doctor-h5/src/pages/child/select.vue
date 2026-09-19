<template>
  <view class="select-page">
    <view class="body">
      <view class="card">
        <view class="section-title">选择儿童</view>
        <ChildSearchPicker placeholder="输入姓名或手机号查找儿童" @select="onSelect" />
        <view class="form-tip">选择儿童后进入「预约授权」页面，为其录入授权次数与缴费信息</view>
      </view>
      <view class="safe-bottom" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import ChildSearchPicker from '@/components/ChildSearchPicker.vue'
import type { Child } from '@/types'

onLoad(() => {
  uni.setNavigationBarTitle({ title: '选择儿童' })
})

function onSelect(child: Child | null) {
  if (!child?.id) return
  uni.navigateTo({ url: `/pages/child/grant?id=${child.id}&from=home` })
}
</script>

<style lang="scss" scoped>
.select-page {
  min-height: 100vh;
  background: #f4f6f9;
}

.body {
  padding: 24rpx 28rpx 0;
}

.form-tip {
  margin-top: 20rpx;
  font-size: 24rpx;
  color: #94a3b8;
}
</style>
