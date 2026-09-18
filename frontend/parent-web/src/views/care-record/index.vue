<template>
  <div class="care-record-page">
    <!-- 孩子选择 -->
    <div class="child-select-bar" v-if="children.length > 0">
      <div
        v-for="child in children"
        :key="child.id"
        class="child-chip"
        :class="{ active: selectedChildId === child.id }"
        @click="selectChild(child.id)"
      >
        {{ child.nameMask || child.name }}
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && records.length === 0" class="empty-state">
      <el-empty description="暂无养护记录">
        <el-button type="primary" @click="$router.push('/appointment')">去预约</el-button>
      </el-empty>
    </div>

    <!-- 瀑布流卡片（双列交错） -->
    <div class="waterfall" v-else-if="records.length > 0">
      <div
        class="record-card"
        v-for="record in records"
        :key="record.id"
        :class="{ 'in-progress': record.status === 1 }"
        @click="showDetail(record)"
      >
        <div class="card-head">
          <span class="care-date">{{ record.careDate }}</span>
          <el-tag :type="record.status === 2 ? 'success' : 'warning'" size="small">
            {{ record.status === 2 ? '已完成' : '养护中' }}
          </el-tag>
        </div>
        <div class="card-slot" v-if="record.timeSlot">{{ record.timeSlot }}</div>
        <div class="vision-row">
          <div class="vision-cell">
            <span class="v-label">养护前</span>
            <span class="v-value">{{ record.visionBeforeLeft || '-' }}</span>
            <span class="v-value">{{ record.visionBeforeRight || '-' }}</span>
          </div>
          <div class="vision-cell">
            <span class="v-label">养护后</span>
            <span class="v-value" :class="{ improve: isImproved(record) }">
              {{ record.visionAfterLeft || '-' }}
            </span>
            <span class="v-value" :class="{ improve: isImproved(record) }">
              {{ record.visionAfterRight || '-' }}
            </span>
          </div>
        </div>
        <div class="card-footer">
          <span>执行人：{{ record.executorName || '-' }}</span>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="养护记录详情" width="88%" top="8vh">
      <div v-if="detailRecord" class="record-detail">
        <div class="detail-row">
          <span class="label">养护日期</span>
          <span class="value">{{ detailRecord.careDate }} {{ detailRecord.timeSlot || '' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">执行人</span>
          <span class="value">{{ detailRecord.executorName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">养护前视力</span>
          <span class="value">左 {{ detailRecord.visionBeforeLeft || '-' }} | 右 {{ detailRecord.visionBeforeRight || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">养护后视力</span>
          <span class="value">左 {{ detailRecord.visionAfterLeft || '-' }} | 右 {{ detailRecord.visionAfterRight || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="label">状态</span>
          <span class="value">{{ detailRecord.status === 2 ? '已完成' : '养护中' }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ParentCareRecord' })
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { CareRecord, Child } from '@/types'
import { childApi, careRecordApi } from '@/api'

const route = useRoute()

const loading = ref(false)
const children = ref<Child[]>([])
const selectedChildId = ref<number | null>(null)
const records = ref<CareRecord[]>([])
const detailVisible = ref(false)
const detailRecord = ref<CareRecord | null>(null)

const selectChild = async (childId: number) => {
  selectedChildId.value = childId
  await fetchRecords()
}

const fetchChildren = async () => {
  try {
    children.value = await childApi.getMyChildren()
    // 默认选中URL指定的孩子，否则选第一个
    const queryChildId = Number(route.query.childId)
    const target = children.value.find(c => c.id === queryChildId) || children.value[0]
    if (target) {
      await selectChild(target.id)
    }
  } catch {
    ElMessage.error('获取孩子列表失败')
  }
}

const fetchRecords = async () => {
  if (!selectedChildId.value) return
  loading.value = true
  try {
    records.value = await careRecordApi.getRecordsByChild(selectedChildId.value)
  } catch {
    ElMessage.error('获取养护记录失败')
  } finally {
    loading.value = false
  }
}

const showDetail = (record: CareRecord) => {
  detailRecord.value = record
  detailVisible.value = true
}

// 养护后视力是否有提升（左眼对比）
const isImproved = (record: CareRecord) => {
  if (!record.visionAfterLeft || !record.visionBeforeLeft) return false
  return parseFloat(record.visionAfterLeft) > parseFloat(record.visionBeforeLeft)
}

onMounted(fetchChildren)
</script>

<style scoped lang="scss">
.care-record-page {
  padding: 16px;

  .child-select-bar {
    display: flex;
    gap: 8px;
    overflow-x: auto;
    margin-bottom: 16px;
    padding-bottom: 4px;

    .child-chip {
      flex-shrink: 0;
      padding: 6px 16px;
      border-radius: 20px;
      background: #fff;
      color: #666;
      font-size: 14px;
      border: 1px solid #e8e8e8;
      cursor: pointer;
      transition: all 0.2s;

      &.active {
        background: #667eea;
        color: #fff;
        border-color: #667eea;
      }
    }
  }

  .empty-state {
    background: #fff;
    border-radius: 12px;
    padding: 40px 20px;
  }

  .waterfall {
    column-count: 2;
    column-gap: 12px;

    .record-card {
      background: #fff;
      border-radius: 12px;
      padding: 14px;
      margin-bottom: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      break-inside: avoid;
      cursor: pointer;

      &.in-progress {
        border: 1px solid #faad14;
      }

      .card-head {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;

        .care-date {
          font-size: 14px;
          font-weight: bold;
          color: #333;
        }
      }

      .card-slot {
        font-size: 12px;
        color: #999;
        margin-bottom: 8px;
      }

      .vision-row {
        display: flex;
        gap: 8px;
        margin-bottom: 8px;

        .vision-cell {
          flex: 1;
          background: #f7f8fa;
          border-radius: 8px;
          padding: 8px;
          text-align: center;

          .v-label {
            display: block;
            font-size: 11px;
            color: #999;
            margin-bottom: 4px;
          }

          .v-value {
            display: block;
            font-size: 16px;
            font-weight: bold;
            color: #333;

            &.improve {
              color: #52c41a;
            }
          }
        }
      }

      .card-footer {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

.record-detail {
  .detail-row {
    display: flex;
    padding: 10px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .label {
      width: 90px;
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
}
</style>
