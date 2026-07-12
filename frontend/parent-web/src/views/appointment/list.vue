<template>
  <div class="reserve-list-page">
    <div class="page-header">
      <h3>我的预约</h3>
    </div>

    <!-- 加载骨架屏 -->
    <div v-if="loading" class="loading-skeleton">
      <div class="skeleton-card" v-for="i in 3" :key="i">
        <div class="skeleton-line long"></div>
        <div class="skeleton-line medium"></div>
        <div class="skeleton-line short"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="reservations.length === 0" class="empty-state">
      <el-empty description="暂无预约记录">
        <el-button type="primary" @click="$router.push('/appointment')">去预约</el-button>
      </el-empty>
    </div>

    <!-- 预约列表 -->
    <div v-else class="reserve-list">
      <div
        class="reserve-card"
        v-for="reserve in reservations"
        :key="reserve.id"
      >
        <div class="reserve-header">
          <span class="order-no">{{ reserve.orderNo }}</span>
          <el-tag :type="getStatusType(reserve.status)" size="small">
            {{ getStatusText(reserve.status) }}
          </el-tag>
        </div>

        <div class="reserve-body">
          <div class="info-row">
            <el-icon><Clock /></el-icon>
            <span>{{ reserve.scheduleDate }} {{ reserve.timeSlotStart }}-{{ reserve.timeSlotEnd }}</span>
          </div>
          <div class="info-row">
            <el-icon><Location /></el-icon>
            <span>{{ reserve.storeName }}</span>
          </div>
          <div class="info-row">
            <el-icon><User /></el-icon>
            <span>{{ reserve.childName }}</span>
          </div>
          <div class="info-row" v-if="reserve.technicianName">
            <el-icon><Service /></el-icon>
            <span>技师: {{ reserve.technicianName }}</span>
          </div>
        </div>

        <div class="reserve-footer" v-if="reserve.status === 1">
          <el-button size="small" type="danger" plain @click="handleCancel(reserve)">
            取消预约
          </el-button>
        </div>

        <div class="cancel-reason" v-if="reserve.status === 5 && reserve.cancelReason">
          取消原因: {{ reserve.cancelReason }}
        </div>
      </div>
    </div>

    <!-- 取消原因弹窗 -->
    <el-dialog v-model="showCancel" title="取消预约" width="85%">
      <el-form>
        <el-form-item label="取消原因">
          <el-input
            v-model="cancelReason"
            type="textarea"
            :rows="3"
            placeholder="请输入取消原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCancel = false">返回</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="confirmCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ParentAppointmentList' })
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Clock, Location, User, Service } from '@element-plus/icons-vue'
import type { Reserve } from '@/types'
import { reserveApi } from '@/api'

const loading = ref(false)
const cancelLoading = ref(false)
const reservations = ref<Reserve[]>([])
const showCancel = ref(false)
const cancelReason = ref('')
const currentReserve = ref<Reserve | null>(null)

// 获取预约列表
const fetchReservations = async () => {
  loading.value = true
  try {
    reservations.value = await reserveApi.getMyReservations()
  } catch {
    ElMessage.error('获取预约列表失败')
  } finally {
    loading.value = false
  }
}

// 取消预约
const handleCancel = (reserve: Reserve) => {
  currentReserve.value = reserve
  cancelReason.value = ''
  showCancel.value = true
}

const confirmCancel = async () => {
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请输入取消原因')
    return
  }
  if (!currentReserve.value) return

  cancelLoading.value = true
  try {
    await reserveApi.cancelReservation(currentReserve.value.id, cancelReason.value)
    ElMessage.success('取消成功')
    showCancel.value = false
    await fetchReservations()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '取消失败')
  } finally {
    cancelLoading.value = false
  }
}

// 状态映射
const getStatusType = (status: number) => {
  const map: Record<number, string> = {
    1: 'warning',  // 待到店
    2: '',         // 已到店
    3: 'primary',  // 服务中
    4: 'success',  // 已完成
    5: 'info'      // 已取消
  }
  return map[status] || 'info'
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = {
    1: '待到店',
    2: '已到店',
    3: '服务中',
    4: '已完成',
    5: '已取消'
  }
  return map[status] || '未知'
}

onMounted(() => {
  fetchReservations()
})
</script>

<style scoped lang="scss">
.reserve-list-page {
  padding: 16px;

  .page-header {
    margin-bottom: 16px;

    h3 {
      font-size: 18px;
      color: #333;
    }
  }

  .loading-skeleton {
    .skeleton-card {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      animation: pulse 1.5s ease-in-out infinite;

      .skeleton-line {
        height: 14px;
        border-radius: 4px;
        background: #e8e8e8;
        margin-bottom: 10px;

        &.long { width: 70%; }
        &.medium { width: 50%; }
        &.short { width: 30%; }
      }
    }
  }

  .empty-state {
    background: #fff;
    border-radius: 12px;
    padding: 40px 20px;
  }

  .reserve-list {
    .reserve-card {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

      .reserve-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        padding-bottom: 12px;
        border-bottom: 1px solid #f5f5f5;

        .order-no {
          font-size: 14px;
          font-weight: bold;
          color: #333;
        }
      }

      .reserve-body {
        .info-row {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 0;
          font-size: 14px;
          color: #666;

          .el-icon {
            color: #999;
          }
        }
      }

      .reserve-footer {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px solid #f5f5f5;
        display: flex;
        justify-content: flex-end;
      }

      .cancel-reason {
        margin-top: 8px;
        padding: 8px 12px;
        background: #fff7e6;
        border-radius: 6px;
        font-size: 13px;
        color: #d48806;
      }
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
