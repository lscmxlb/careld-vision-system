<template>
  <div class="schedule-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>全门店排班监控</span>
          <div class="header-actions">
            <el-select v-model="selectedStore" placeholder="选择门店" clearable style="width: 200px; margin-right: 10px;">
              <el-option
                v-for="item in storeOptions"
                :key="item.id"
                :label="item.storeName"
                :value="item.id"
              />
            </el-select>
            <el-date-picker
              v-model="selectedDate"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="margin-right: 10px;"
            />
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>

      <!-- 统计概览 -->
      <el-row :gutter="16" class="stat-row">
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalSchedules }}</div>
            <div class="stat-label">今日排班</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-value">{{ stats.totalCapacity }}</div>
            <div class="stat-label">总容量</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-value">{{ stats.reservedCount }}</div>
            <div class="stat-label">已预约</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-value">{{ stats.availableCount }}</div>
            <div class="stat-label">可预约</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-value">{{ stats.utilizationRate }}%</div>
            <div class="stat-label">预约率</div>
          </div>
        </el-col>
        <el-col :span="4">
          <div class="stat-item">
            <div class="stat-value">{{ stats.technicianCount }}</div>
            <div class="stat-label">在岗技师</div>
          </div>
        </el-col>
      </el-row>

      <!-- 排班表格 -->
      <el-table :data="scheduleData" v-loading="loading" stripe>
        <el-table-column prop="storeName" label="门店" min-width="150" />
        <el-table-column prop="technicianName" label="技师" width="100" />
        <el-table-column prop="timeSlot" label="时段" width="120">
          <template #default="{ row }">
            {{ row.timeSlotStart }} - {{ row.timeSlotEnd }}
          </template>
        </el-table-column>
        <el-table-column prop="maxCapacity" label="容量" width="80" />
        <el-table-column prop="reservedCount" label="已预约" width="80" />
        <el-table-column prop="availableCount" label="可预约" width="80" />
        <el-table-column label="预约率" width="120">
          <template #default="{ row }">
            <el-progress
              :percentage="Math.round((row.reservedCount / row.maxCapacity) * 100)"
              :status="row.reservedCount >= row.maxCapacity ? 'success' : ''"
            />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '开放' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="排班详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="门店">{{ currentSchedule?.storeName }}</el-descriptions-item>
        <el-descriptions-item label="技师">{{ currentSchedule?.technicianName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ currentSchedule?.scheduleDate }}</el-descriptions-item>
        <el-descriptions-item label="时段">{{ currentSchedule?.timeSlotStart }} - {{ currentSchedule?.timeSlotEnd }}</el-descriptions-item>
        <el-descriptions-item label="容量">{{ currentSchedule?.maxCapacity }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentSchedule?.status === 1 ? 'success' : 'info'">
            {{ currentSchedule?.status === 1 ? '开放' : '关闭' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h4>预约列表</h4>
      <el-table :data="reserveList" stripe size="small">
        <el-table-column prop="childName" label="儿童姓名" />
        <el-table-column prop="parentName" label="家长姓名" />
        <el-table-column prop="parentPhone" label="联系电话" />
        <el-table-column prop="reserveType" label="预约类型">
          <template #default="{ row }">
            {{ row.reserveType === 1 ? '首次' : '复诊' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'AdminSchedule' })
import { ref, reactive, onMounted } from 'vue'
import { storeApi, reserveApi } from '@/api'
import type { Store, Schedule, Reserve } from '@/types'

const loading = ref(false)
const selectedStore = ref<number | undefined>()
const selectedDate = ref(new Date().toISOString().split('T')[0])
const storeOptions = ref<Store[]>([])

const scheduleData = ref<Schedule[]>([])
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const stats = reactive({
  totalSchedules: 0,
  totalCapacity: 0,
  reservedCount: 0,
  availableCount: 0,
  utilizationRate: 0,
  technicianCount: 0
})

// 详情弹窗
const detailVisible = ref(false)
const currentSchedule = ref<Schedule | null>(null)
const reserveList = ref<Reserve[]>([])

const fetchStores = async () => {
  try {
    const res = await storeApi.getAllStores()
    storeOptions.value = res
  } catch (error) {
    console.error('获取门店列表失败', error)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    // 这里模拟数据，实际应该调用API
    const mockData: Schedule[] = []
    for (let i = 0; i < 20; i++) {
      mockData.push({
        id: i + 1,
        scheduleDate: selectedDate.value ?? '',
        timeSlotStart: `${9 + Math.floor(i / 3)}:00`,
        timeSlotEnd: `${9 + Math.floor(i / 3)}:50`,
        technicianId: i % 5 + 1,
        technicianName: `技师${(i % 5) + 1}`,
        maxCapacity: 3,
        reservedCount: Math.floor(Math.random() * 4),
        availableCount: 3 - Math.floor(Math.random() * 4),
        status: 1,
        storeName: `门店${(i % 10) + 1}`
      })
    }
    scheduleData.value = mockData
    pagination.total = 100
    
    // 更新统计
    stats.totalSchedules = mockData.length
    stats.totalCapacity = mockData.reduce((sum, item) => sum + item.maxCapacity, 0)
    stats.reservedCount = mockData.reduce((sum, item) => sum + item.reservedCount, 0)
    stats.availableCount = stats.totalCapacity - stats.reservedCount
    stats.utilizationRate = Math.round((stats.reservedCount / stats.totalCapacity) * 100)
    stats.technicianCount = 5
  } catch (error) {
    console.error('获取排班数据失败', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchData()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  fetchData()
}

const handleCurrentChange = (val: number) => {
  pagination.page = val
  fetchData()
}

const handleViewDetail = async (row: Schedule) => {
  currentSchedule.value = row
  detailVisible.value = true
  // 获取预约列表
  try {
    const res = await reserveApi.getReserveList({
      scheduleId: row.id,
      page: 1,
      size: 100
    })
    reserveList.value = res.data.list
  } catch {
    reserveList.value = []
  }
}

onMounted(() => {
  fetchStores()
  fetchData()
})
</script>

<style scoped lang="scss">
.schedule-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .stat-row {
    margin-bottom: 20px;

    .stat-item {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 8px;
      text-align: center;

      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #1890ff;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 12px;
        color: #666;
      }
    }
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
