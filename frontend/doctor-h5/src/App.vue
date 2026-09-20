<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 登录保护由 main.ts 的路由守卫统一处理
})
</script>

<style lang="scss">
@import 'uview-plus/index.scss';

page {
  background-color: $app-bg;
  color: $app-text;
  font-size: 28rpx;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

view,
text,
input,
textarea,
scroll-view {
  box-sizing: border-box;
}

/* ---------------- 布局 ---------------- */
.page {
  min-height: 100vh;
  padding: $app-gap;
}

.page-flat {
  min-height: 100vh;
}

.safe-bottom {
  height: calc(env(safe-area-inset-bottom) + 24rpx);
}

.row {
  display: flex;
  align-items: center;
}

.row-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.row-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.col {
  display: flex;
  flex-direction: column;
}

.flex-1 {
  flex: 1;
  min-width: 0;
}

/* ---------------- 卡片 ---------------- */
.card {
  background: $app-card;
  border-radius: $app-radius;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.04);
}

.card + .card {
  margin-top: $app-gap;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
  color: $app-text;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 30rpx;
  font-weight: 600;
  color: $app-text;
  margin: 8rpx 0 20rpx;

  .section-more {
    font-size: 24rpx;
    font-weight: 400;
    color: $app-text-light;
  }
}

/* ---------------- 文本 ---------------- */
.text-sub {
  color: $app-text-sub;
  font-size: 26rpx;
}

.text-light {
  color: $app-text-light;
  font-size: 24rpx;
}

.text-primary {
  color: $app-primary;
}

.text-danger {
  color: $app-danger;
}

.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* ---------------- 标签 ---------------- */
.tag {
  display: inline-flex;
  align-items: center;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  line-height: 1;
  white-space: nowrap;
}

.tag-warning {
  background: $app-warning-light;
  color: $app-warning;
}

.tag-primary {
  background: $app-info-light;
  color: $app-info;
}

.tag-success {
  background: $app-success-light;
  color: $app-success;
}

.tag-grey {
  background: $app-grey-light;
  color: $app-grey;
}

.tag-danger {
  background: $app-danger-light;
  color: $app-danger;
}

.tag-primary {
  background: $app-primary-light;
  color: $app-primary-dark;
}

/* ---------------- 按钮 ---------------- */
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 72rpx;
  padding: 0 28rpx;
  border-radius: 36rpx;
  font-size: 26rpx;
  line-height: 1;
  transition: opacity 0.15s;
}

.btn:active {
  opacity: 0.75;
}

.btn-sm {
  height: 58rpx;
  padding: 0 22rpx;
  font-size: 24rpx;
  border-radius: 29rpx;
}

.btn-primary {
  background: $app-primary;
  color: #fff;
}

.btn-outline {
  background: #fff;
  color: $app-primary;
  border: 2rpx solid $app-primary;
}

.btn-plain {
  background: #fff;
  color: $app-text-sub;
  border: 2rpx solid #e2e8f0;
}

.btn-danger-plain {
  background: #fff;
  color: $app-danger;
  border: 2rpx solid #fbc9c9;
}

.btn-danger {
  background: $app-danger;
  color: #fff;
}

.btn-success {
  background: $app-success;
  color: #fff;
}

.btn-block {
  width: 100%;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
}

.btn.is-disabled,
.btn[disabled] {
  background: #e8ecf1 !important;
  color: #a9b1bd !important;
  border-color: #e8ecf1 !important;
  pointer-events: none;
}

/* ---------------- 表单 ---------------- */
.field {
  display: flex;
  align-items: center;
  min-height: 92rpx;
  padding: 16rpx 0;
  border-bottom: 2rpx solid $app-border;
}

.field:last-child {
  border-bottom: none;
}

.field-label {
  width: 176rpx;
  flex-shrink: 0;
  color: $app-text-sub;
  font-size: 27rpx;
}

.field-label.required::before {
  content: '*';
  color: $app-danger;
  margin-right: 6rpx;
}

.field-value {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  color: $app-text;
}

.field-input {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  color: $app-text;
  height: 60rpx;
}

.field-placeholder {
  color: #c2c9d1;
  font-size: 28rpx;
}

.field-arrow::after {
  content: '';
  width: 14rpx;
  height: 14rpx;
  border-top: 3rpx solid #cbd5e1;
  border-right: 3rpx solid #cbd5e1;
  transform: rotate(45deg);
  margin-left: 12rpx;
  flex-shrink: 0;
}

/* ---------------- 选择框（统一样式） ---------------- */
.field-control {
  flex: 1;
  min-width: 0;
}

.select-box {
  display: flex;
  align-items: center;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f8fafc;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  box-sizing: border-box;
}

.select-text {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-arrow {
  width: 14rpx;
  height: 14rpx;
  flex: none;
  margin-left: 12rpx;
  border-top: 3rpx solid #94a3b8;
  border-right: 3rpx solid #94a3b8;
  transform: rotate(135deg);
}

/* ---------------- 底部操作条 ---------------- */
.sticky-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  padding: 16rpx 24rpx calc(env(safe-area-inset-bottom) + 16rpx);
  box-shadow: 0 -4rpx 20rpx rgba(15, 23, 42, 0.06);
}

.sticky-bar .btn {
  flex: 1;
}

/* ---------------- 空态 ---------------- */
.empty {
  padding: 120rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: $app-text-light;
  font-size: 26rpx;
}

.empty-icon {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  background: #eef2f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  margin-bottom: 24rpx;
  color: #cbd5e1;
}

/* ---------------- 加载态 ---------------- */
.loading-tip {
  text-align: center;
  padding: 32rpx 0;
  color: $app-text-light;
  font-size: 24rpx;
}

/* ---------------- 分隔线 ---------------- */
.divider {
  height: 2rpx;
  background: $app-border;
  margin: 20rpx 0;
}

.divider-fold {
  display: flex;
  align-items: center;
  font-size: 24rpx;
  color: $app-text-light;
  margin: 28rpx 0 8rpx;
}

.divider-fold::after {
  content: '';
  flex: 1;
  height: 2rpx;
  background: $app-border;
  margin-left: 16rpx;
}

/* ---------------- 遮罩弹层 ---------------- */
.mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 1000;
}

/* uni-app H5 的 picker 弹层自带 z-index:999，低于本端贴底弹层(1000/1001)，
   在取消预约等弹层里打开会被遮罩盖住而无法选择，这里统一抬到弹层之上 */
.uni-picker-container {
  z-index: 1200 !important;
}

.sheet {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
  background: #fff;
  border-radius: 28rpx 28rpx 0 0;
  max-height: 86vh;
  display: flex;
  flex-direction: column;
}

/* 居中弹层：在 .sheet 基础上改为页面正中（上下居中），左右留边、四角圆角 */
.sheet-center {
  left: 48rpx;
  right: 48rpx;
  top: 50%;
  bottom: auto;
  transform: translateY(-50%);
  border-radius: 24rpx;
  max-height: 80vh;
  overflow: hidden;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx 16rpx;
  flex-shrink: 0;
}

.sheet-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $app-text;
}

.sheet-close {
  font-size: 40rpx;
  color: $app-text-light;
  line-height: 1;
  padding: 0 8rpx;
}

.sheet-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 24rpx;
}

.sheet-footer {
  flex-shrink: 0;
  display: flex;
  gap: 20rpx;
  padding: 16rpx 24rpx calc(env(safe-area-inset-bottom) + 16rpx);
  background: #fff;
}

.sheet-footer .btn {
  flex: 1;
}

/* ---------------- 居中弹窗 ---------------- */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 56rpx;
}

.modal-card {
  width: 100%;
  max-height: 70vh;
  background: #fff;
  border-radius: 24rpx;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 32rpx 32rpx 12rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: 600;
  color: $app-text;
}

.modal-close {
  font-size: 40rpx;
  color: $app-text-light;
  line-height: 1;
  padding: 0 8rpx;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 12rpx 32rpx 8rpx;
}

.modal-footer {
  flex-shrink: 0;
  display: flex;
  gap: 20rpx;
  padding: 24rpx 32rpx 32rpx;
}

.modal-footer .btn {
  flex: 1;
}
</style>
