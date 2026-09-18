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
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 统一搜索表单控件宽度，避免 inline 模式下缩成过窄 */
.search-form .el-select {
  width: 180px;
}

.search-form .el-input {
  width: 200px;
}
</style>
