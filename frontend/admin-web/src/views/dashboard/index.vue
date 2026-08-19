<template>
  <div class="dashboard-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #e6f7ff;">
            <el-icon :size="32" color="#1890ff"><Shop /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.storeCount }}</div>
            <div class="stat-label">医院总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #f6ffed;">
            <el-icon :size="32" color="#52c41a"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.childCount }}</div>
            <div class="stat-label">建档儿童</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #fff7e6;">
            <el-icon :size="32" color="#faad14"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.monthlyVisits }}</div>
            <div class="stat-label">本月养护</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-icon" style="background: #f9f0ff;">
            <el-icon :size="32" color="#722ed1"><TrendCharts /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.avgImprovement }}</div>
            <div class="stat-label">平均改善</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>全国医院客流趋势</span>
              <el-radio-group v-model="trendTimeRange" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="year">全年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>医院业绩排行</span>
              <el-link type="primary" @click="$router.push('/store/list')">查看全部</el-link>
            </div>
          </template>
          <div ref="rankChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-row :gutter="16" class="table-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待审核档案</span>
              <el-link type="primary" @click="$router.push('/store/audit')">去审核</el-link>
            </div>
          </template>
          <el-table :data="pendingAudits" v-loading="loading" stripe>
            <el-table-column prop="name" label="儿童姓名" width="100" />
            <el-table-column prop="storeName" label="所属医院" />
            <el-table-column prop="age" label="年龄" width="80" />
            <el-table-column prop="createdAt" label="提交时间" width="160">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="goToAudit(row.id)">审核</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>设备状态监控</span>
              <el-link type="primary" @click="$router.push('/device')">查看全部</el-link>
            </div>
          </template>
          <el-table :data="deviceStatus" v-loading="loading" stripe>
            <el-table-column prop="storeName" label="医院" />
            <el-table-column prop="deviceName" label="设备名称" />
            <el-table-column prop="calibrationStatus" label="校准状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.calibrationStatus === 1 ? 'success' : 'warning'">
                  {{ row.calibrationStatus === 1 ? '已校准' : '未校准' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastSyncTime" label="最后同步" width="160">
              <template #default="{ row }">
                {{ row.lastSyncTime ? formatDate(row.lastSyncTime) : '从未' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminDashboard' })
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Shop, User, Calendar, TrendCharts } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { statisticsApi, childApi, deviceApi } from '@/api'
import type { Child, Device } from '@/types'

const router = useRouter()

const loading = ref(false)
const trendTimeRange = ref('week')
const trendChartRef = ref<HTMLDivElement>()
const rankChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let rankChart: echarts.ECharts | null = null

const stats = reactive({
  storeCount: 0,
  childCount: 0,
  monthlyVisits: 0,
  avgImprovement: '0.0'
})

const pendingAudits = ref<Child[]>([])
const deviceStatus = ref<Device[]>([])

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '养护人次',
        type: 'line',
        smooth: true,
        data: [120, 132, 101, 134, 90, 230, 210],
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24, 144, 255, 0.3)' },
            { offset: 1, color: 'rgba(24, 144, 255, 0.05)' }
          ])
        },
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '新建档',
        type: 'line',
        smooth: true,
        data: [20, 32, 21, 34, 10, 40, 30],
        itemStyle: { color: '#52c41a' }
      }
    ]
  }
  trendChart.setOption(option)
}

// 初始化排行图表
const initRankChart = () => {
  if (!rankChartRef.value) return
  rankChart = echarts.init(rankChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: ['医院5', '医院4', '医院3', '医院2', '医院1']
    },
    series: [
      {
        name: '养护人次',
        type: 'bar',
        data: [150, 230, 320, 450, 520],
        itemStyle: { color: '#1890ff' }
      }
    ]
  }
  rankChart.setOption(option)
}

// 获取统计数据
const fetchData = async () => {
  loading.value = true
  try {
    // 获取全国汇总数据
    const endDate = new Date().toISOString().split('T')[0] ?? ''
    const startDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0] ?? ''
    const summary = await statisticsApi.getNationalSummary({ startDate, endDate })
    stats.storeCount = summary.storeCount
    stats.childCount = summary.totalChildren
    stats.monthlyVisits = summary.monthlyVisits
    stats.avgImprovement = summary.avgImprovement

    // 获取待审核档案
    const pendingRes = await childApi.getChildList({ auditStatus: 0, size: 5 })
    pendingAudits.value = pendingRes.data.list

    // 获取设备状态
    const deviceRes = await deviceApi.getDeviceList({ size: 5 })
    deviceStatus.value = deviceRes.data.list
  } catch (error) {
    console.error('获取数据失败', error)
  } finally {
    loading.value = false
  }
}

const goToAudit = (id: number) => {
  router.push(`/store/audit?id=${id}`)
}

const formatDate = (date: string) => {
  return new Date(date).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 监听时间范围变化
watch(trendTimeRange, () => {
  if (trendChart) {
    // 这里可以根据时间范围重新加载数据
    trendChart.resize()
  }
})

onMounted(() => {
  nextTick(() => {
    initTrendChart()
    initRankChart()
  })
  fetchData()
})

onUnmounted(() => {
  trendChart?.dispose()
  rankChart?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard-page {
  .stat-cards {
    margin-bottom: 16px;

    .stat-card {
      display: flex;
      align-items: center;
      padding: 20px;

      .stat-icon {
        width: 64px;
        height: 64px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 16px;
      }

      .stat-info {
        .stat-value {
          font-size: 28px;
          font-weight: bold;
          color: #262626;
          line-height: 1;
          margin-bottom: 8px;
        }

        .stat-label {
          font-size: 14px;
          color: #8c8c8c;
        }
      }
    }
  }

  .chart-row {
    margin-bottom: 16px;

    .chart-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .chart-container {
        height: 300px;
      }
    }
  }

  .table-row {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
}
</style>
