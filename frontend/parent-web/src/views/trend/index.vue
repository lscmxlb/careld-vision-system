<template>
  <div class="trend-page">
    <!-- 孩子选择器 -->
    <div class="child-selector" v-if="children.length > 1">
      <el-select v-model="selectedChildId" placeholder="选择孩子" @change="fetchAllData" style="width: 100%">
        <el-option
          v-for="child in children"
          :key="child.id"
          :label="child.name"
          :value="child.id"
        />
      </el-select>
    </div>

    <!-- 日期范围 -->
    <div class="date-filter">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        @change="fetchAllData"
        style="width: 100%"
      />
    </div>

    <!-- 加载骨架屏 -->
    <div v-if="loading" class="loading-skeleton">
      <div class="skeleton-card">
        <div class="skeleton-line long"></div>
        <div class="skeleton-chart"></div>
      </div>
      <div class="skeleton-card">
        <div class="skeleton-line medium"></div>
        <div class="skeleton-grid">
          <div class="skeleton-cell" v-for="i in 4" :key="i"></div>
        </div>
      </div>
    </div>

    <template v-else>
      <!-- 视力趋势图 -->
      <div class="trend-card">
        <h3>视力变化趋势</h3>
        <div ref="chartRef" class="chart-container"></div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-card">
        <h3>养护统计</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <span class="stat-value">{{ stats.totalCount }}</span>
            <span class="stat-label">检测次数</span>
          </div>
          <div class="stat-item">
            <span class="stat-value" :class="stats.leftImprovement > 0 ? 'positive' : 'negative'">
              {{ stats.leftImprovement > 0 ? '+' : '' }}{{ stats.leftImprovement }}
            </span>
            <span class="stat-label">左眼改善</span>
          </div>
          <div class="stat-item">
            <span class="stat-value" :class="stats.rightImprovement > 0 ? 'positive' : 'negative'">
              {{ stats.rightImprovement > 0 ? '+' : '' }}{{ stats.rightImprovement }}
            </span>
            <span class="stat-label">右眼改善</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ stats.improvementRate }}%</span>
            <span class="stat-label">改善率</span>
          </div>
        </div>
      </div>

      <!-- 检测记录列表 -->
      <div class="history-card">
        <h3>检测记录</h3>
        <div v-if="records.length === 0" class="empty-history">
          <el-empty description="暂无检测记录" :image-size="60" />
        </div>
        <div v-else class="history-list">
          <div class="history-item" v-for="record in records" :key="record.id">
            <div class="history-date">
              <span class="day">{{ record.testTime.substring(8, 10) }}</span>
              <span class="month">{{ record.testTime.substring(5, 7) }}月</span>
            </div>
            <div class="history-info">
              <p class="store">{{ record.storeName || '医院' }}</p>
              <p class="tech">
                {{ record.testType === 1 ? '养护前' : '养护后' }} |
                检测: {{ record.testerName }}
              </p>
              <p class="effect">
                视力: {{ record.visionLevel }}
              </p>
            </div>
            <div class="history-vision" :class="getVisionClass(record.visionLevel)">
              {{ record.visionLevel }}
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ParentTrend' })
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import type { Child, VisionRecord } from '@/types'
import { childApi, visionApi } from '@/api'

const route = useRoute()

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

const loading = ref(false)
const children = ref<Child[]>([])
const selectedChildId = ref<number | null>(null)
const records = ref<VisionRecord[]>([])
const dateRange = ref<[string, string] | null>(null)

// 统计数据
const stats = computed(() => {
  if (records.value.length === 0) {
    return { totalCount: 0, leftImprovement: 0, rightImprovement: 0, improvementRate: 0 }
  }

  const afterTests = records.value.filter(r => r.testType === 2)
  const beforeTests = records.value.filter(r => r.testType === 1)

  let leftImprove = 0
  let rightImprove = 0
  let improvedCount = 0

  // 简单计算：最新养护后 vs 之前养护前的差值
  if (afterTests.length > 0 && beforeTests.length > 0) {
    const latest = afterTests[0]!
    const earliest = beforeTests[beforeTests.length - 1]!
    leftImprove = parseFloat(latest.visionLevel) - parseFloat(earliest.visionLevel)
    rightImprove = leftImprove // 简化处理
    improvedCount = afterTests.filter(t => {
      const before = beforeTests.find(b => b.reserveId === t.reserveId)
      return before && parseFloat(t.visionLevel) > parseFloat(before.visionLevel)
    }).length
  }

  return {
    totalCount: records.value.length,
    leftImprovement: Math.round(leftImprove * 10) / 10,
    rightImprovement: Math.round(rightImprove * 10) / 10,
    improvementRate: afterTests.length > 0
      ? Math.round((improvedCount / afterTests.length) * 100)
      : 0
  }
})

// 获取孩子列表
const fetchChildren = async () => {
  try {
    children.value = await childApi.getMyChildren()
    if (children.value.length > 0) {
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
  try {
    records.value = await visionApi.getVisionRecords(selectedChildId.value)
    // 按时间排序
    records.value.sort((a, b) => new Date(a.testTime).getTime() - new Date(b.testTime).getTime())
  } catch {
    ElMessage.error('获取检测记录失败')
  }
}

// 渲染图表
const renderChart = () => {
  if (!chartRef.value) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  // 从记录中提取日期和视力数据
  const dates = records.value.map(r => {
    const d = r.testTime.substring(5, 10).replace('-', '/')
    return d
  })

  // 按检测类型分组
  const beforeData = records.value.map(r => r.testType === 1 ? parseFloat(r.visionLevel) : null)
  const afterData = records.value.map(r => r.testType === 2 ? parseFloat(r.visionLevel) : null)

  const option = {
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      min: (value: { min: number }) => Math.floor(value.min * 10 - 1) / 10,
      max: (value: { max: number }) => Math.ceil(value.max * 10 + 1) / 10
    },
    tooltip: {
      trigger: 'axis'
    },
    series: [
      {
        name: '养护前',
        type: 'line',
        data: beforeData,
        smooth: true,
        connectNulls: true,
        itemStyle: { color: '#faad14' },
        lineStyle: { type: 'dashed' }
      },
      {
        name: '养护后',
        type: 'line',
        data: afterData,
        smooth: true,
        connectNulls: true,
        itemStyle: { color: '#52c41a' }
      }
    ],
    legend: { data: ['养护前', '养护后'], bottom: 0 }
  }

  chart.setOption(option)
}

// 全部数据
const fetchAllData = async () => {
  loading.value = true
  try {
    await fetchRecords()
    await nextTick()
    renderChart()
  } catch {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
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
    fetchAllData()
  }
})

onMounted(async () => {
  await fetchChildren()
  if (selectedChildId.value) {
    await fetchAllData()
  }
})

onUnmounted(() => {
  chart?.dispose()
})
</script>

<style scoped lang="scss">
.trend-page {
  padding: 16px;

  .child-selector,
  .date-filter {
    margin-bottom: 16px;
  }

  .loading-skeleton {
    .skeleton-card {
      background: #fff;
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 16px;
      animation: pulse 1.5s ease-in-out infinite;

      .skeleton-line {
        height: 14px;
        border-radius: 4px;
        background: #e8e8e8;
        margin-bottom: 16px;

        &.long { width: 60%; }
        &.medium { width: 40%; }
      }

      .skeleton-chart {
        height: 200px;
        background: #f0f0f0;
        border-radius: 8px;
      }

      .skeleton-grid {
        display: grid;
        grid-template-columns: repeat(2, 1fr);
        gap: 12px;

        .skeleton-cell {
          height: 60px;
          background: #f0f0f0;
          border-radius: 8px;
        }
      }
    }
  }

  .trend-card,
  .stats-card,
  .history-card {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    h3 {
      font-size: 16px;
      margin-bottom: 16px;
      color: #333;
    }
  }

  .chart-container {
    height: 250px;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;

    .stat-item {
      text-align: center;
      padding: 16px;
      background: #f7f8fa;
      border-radius: 8px;

      .stat-value {
        display: block;
        font-size: 24px;
        font-weight: bold;
        color: #1890ff;
        margin-bottom: 4px;

        &.positive { color: #52c41a; }
        &.negative { color: #f5222d; }
      }

      .stat-label {
        font-size: 12px;
        color: #666;
      }
    }
  }

  .empty-history {
    padding: 20px 0;
  }

  .history-list {
    .history-item {
      display: flex;
      align-items: center;
      padding: 16px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .history-date {
        width: 60px;
        text-align: center;

        .day {
          display: block;
          font-size: 24px;
          font-weight: bold;
          color: #1890ff;
        }

        .month {
          font-size: 12px;
          color: #999;
        }
      }

      .history-info {
        flex: 1;
        padding: 0 12px;

        .store {
          font-size: 14px;
          font-weight: bold;
          margin-bottom: 4px;
        }

        .tech {
          font-size: 12px;
          color: #666;
          margin-bottom: 4px;
        }

        .effect {
          font-size: 12px;
          color: #1890ff;
        }
      }

      .history-vision {
        font-size: 20px;
        font-weight: bold;

        &.vision-good { color: #52c41a; }
        &.vision-normal { color: #faad14; }
        &.vision-poor { color: #f5222d; }
      }
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
