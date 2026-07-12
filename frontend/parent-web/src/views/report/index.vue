<template>
  <div class="report-page">
    <!-- 孩子选择器 -->
    <div class="child-selector" v-if="children.length > 1">
      <el-select v-model="selectedChildId" placeholder="选择孩子" @change="fetchRecords" style="width: 100%">
        <el-option
          v-for="child in children"
          :key="child.id"
          :label="child.name"
          :value="child.id"
        />
      </el-select>
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
    <div v-else-if="records.length === 0" class="empty-state">
      <el-empty description="暂无检测记录">
        <el-button type="primary" @click="$router.push('/appointment')">去预约</el-button>
      </el-empty>
    </div>

    <!-- 报告列表 -->
    <div v-else class="report-list">
      <div
        class="report-card"
        v-for="record in records"
        :key="record.id"
        @click="showRecordDetail(record)"
      >
        <div class="report-header">
          <span class="report-date">{{ formatDate(record.testTime) }}</span>
          <el-tag :type="record.testType === 1 ? 'info' : 'success'" size="small">
            {{ record.testType === 1 ? '养护前' : '养护后' }}
          </el-tag>
        </div>
        <div class="report-body">
          <div class="vision-row">
            <div class="vision-item">
              <span class="label">左眼</span>
              <span class="value" :class="getVisionClass(record.visionLevel)">
                {{ record.visionLevel }}
              </span>
            </div>
          </div>
          <div class="report-info">
            <p>检测门店: {{ record.storeName || '--' }}</p>
            <p>检测人员: {{ record.testerName }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 记录详情弹窗 -->
    <el-dialog v-model="showDetail" title="检测详情" width="90%" top="5vh">
      <div v-if="detailRecord" class="record-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <div class="detail-row">
            <span class="label">检测时间</span>
            <span class="value">{{ formatDateTime(detailRecord.testTime) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">检测类型</span>
            <span class="value">{{ detailRecord.testType === 1 ? '养护前检测' : '养护后检测' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">检测门店</span>
            <span class="value">{{ detailRecord.storeName || '--' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">检测人员</span>
            <span class="value">{{ detailRecord.testerName }}</span>
          </div>
        </div>
        <div class="detail-section">
          <h4>检测结果</h4>
          <div class="vision-result">
            <div class="vision-block">
              <span class="vision-label">视力</span>
              <span class="vision-val" :class="getVisionClass(detailRecord.visionLevel)">
                {{ detailRecord.visionLevel }}
              </span>
            </div>
          </div>
        </div>
        <div class="detail-section" v-if="detailRecord.remark">
          <h4>备注</h4>
          <p class="remark-text">{{ detailRecord.remark }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ParentReport' })
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { Child, VisionRecord } from '@/types'
import { childApi, visionApi } from '@/api'

const route = useRoute()

const loading = ref(false)
const children = ref<Child[]>([])
const selectedChildId = ref<number | null>(null)
const records = ref<VisionRecord[]>([])
const showDetail = ref(false)
const detailRecord = ref<VisionRecord | null>(null)

// 获取孩子列表
const fetchChildren = async () => {
  try {
    children.value = await childApi.getMyChildren()
    if (children.value.length > 0) {
      // 优先使用 URL 参数中的 childId
      const urlChildId = route.query.childId ? Number(route.query.childId) : null
      if (urlChildId && children.value.some(c => c.id === urlChildId)) {
        selectedChildId.value = urlChildId
      } else {
        selectedChildId.value = children.value[0]!.id
      }
    }
  } catch {
    ElMessage.error('获取孩子列表失败')
  }
}

// 获取检测记录
const fetchRecords = async () => {
  if (!selectedChildId.value) return
  loading.value = true
  try {
    records.value = await visionApi.getVisionRecords(selectedChildId.value)
  } catch {
    ElMessage.error('获取检测记录失败')
  } finally {
    loading.value = false
  }
}

// 显示记录详情
const showRecordDetail = (record: VisionRecord) => {
  detailRecord.value = record
  showDetail.value = true
}

// 日期格式化
const formatDate = (dateStr: string) => {
  if (!dateStr) return '--'
  return dateStr.substring(0, 10)
}

const formatDateTime = (dateStr: string) => {
  if (!dateStr) return '--'
  return dateStr.replace('T', ' ').substring(0, 16)
}

// 视力等级样式
const getVisionClass = (vision: string) => {
  if (!vision) return ''
  const val = parseFloat(vision)
  if (val >= 5.0) return 'vision-good'
  if (val >= 4.8) return 'vision-normal'
  return 'vision-poor'
}

// 监听路由变化
watch(() => route.query.childId, (newId) => {
  if (newId) {
    selectedChildId.value = Number(newId)
    fetchRecords()
  }
})

onMounted(async () => {
  await fetchChildren()
  if (selectedChildId.value) {
    fetchRecords()
  }
})
</script>

<style scoped lang="scss">
.report-page {
  padding: 16px;

  .child-selector {
    margin-bottom: 16px;
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

  .report-list {
    .report-card {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      cursor: pointer;
      transition: transform 0.2s;

      &:active {
        transform: scale(0.98);
      }

      .report-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .report-date {
          font-size: 16px;
          font-weight: bold;
        }
      }

      .report-body {
        .vision-row {
          display: flex;
          gap: 40px;
          margin-bottom: 12px;

          .vision-item {
            .label {
              display: block;
              color: #999;
              font-size: 12px;
              margin-bottom: 4px;
            }

            .value {
              font-size: 24px;
              font-weight: bold;

              &.vision-good { color: #52c41a; }
              &.vision-normal { color: #faad14; }
              &.vision-poor { color: #f5222d; }
            }
          }
        }

        .report-info {
          p {
            color: #666;
            font-size: 13px;
            margin-bottom: 4px;
          }
        }
      }
    }
  }

  .record-detail {
    .detail-section {
      margin-bottom: 20px;

      h4 {
        font-size: 15px;
        color: #333;
        margin-bottom: 12px;
        padding-left: 8px;
        border-left: 3px solid #1890ff;
      }
    }

    .detail-row {
      display: flex;
      padding: 8px 0;

      .label {
        width: 80px;
        color: #999;
        font-size: 14px;
        flex-shrink: 0;
      }

      .value {
        flex: 1;
        color: #333;
        font-size: 14px;
      }
    }

    .vision-result {
      display: flex;
      justify-content: center;

      .vision-block {
        text-align: center;

        .vision-label {
          display: block;
          color: #999;
          font-size: 12px;
          margin-bottom: 8px;
        }

        .vision-val {
          font-size: 36px;
          font-weight: bold;

          &.vision-good { color: #52c41a; }
          &.vision-normal { color: #faad14; }
          &.vision-poor { color: #f5222d; }
        }
      }
    }

    .remark-text {
      color: #666;
      font-size: 14px;
      line-height: 1.6;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
