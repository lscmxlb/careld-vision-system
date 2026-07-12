<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  -webkit-tap-highlight-color: transparent;
}

html {
  font-size: 16px;
}

body {
  background: #f5f5f5;
  color: #333;
  overflow-x: hidden;
}

/* 移动端全局优化 */
a {
  text-decoration: none;
  color: inherit;
}

img {
  max-width: 100%;
}

/* Element Plus 移动端微调 */
.el-dialog {
  border-radius: 16px !important;
}

.el-message-box {
  border-radius: 12px !important;
}

/* 滚动条隐藏 */
::-webkit-scrollbar {
  display: none;
}

* {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
