<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <p class="error-message">页面不存在</p>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminNotFound' })
</script>

<style scoped lang="scss">
.error-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;

  .error-content {
    text-align: center;

    .error-code {
      font-size: 120px;
      font-weight: bold;
      color: #1890ff;
      margin-bottom: 20px;
      line-height: 1;
    }

    .error-message {
      font-size: 24px;
      color: #333;
      margin-bottom: 10px;
    }

    .error-desc {
      font-size: 14px;
      color: #999;
      margin-bottom: 30px;
    }
  }
}
</style>
