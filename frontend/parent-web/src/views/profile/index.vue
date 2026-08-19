<template>
  <div class="profile-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-skeleton">
      <div class="skeleton-card" v-for="i in 2" :key="i">
        <div class="skeleton-circle"></div>
        <div class="skeleton-content">
          <div class="skeleton-line long"></div>
          <div class="skeleton-line medium"></div>
          <div class="skeleton-line short"></div>
        </div>
      </div>
    </div>

    <!-- 孩子列表 -->
    <template v-else>
      <div class="child-card" v-for="child in children" :key="child.id" @click="showChildDetail(child)">
        <div class="child-header">
          <div class="child-avatar">
            <el-avatar :size="60" :icon="UserFilled" />
          </div>
          <div class="child-info">
            <h3>
              {{ child.name }}
              <el-tag size="small" :type="child.gender === 1 ? '' : 'danger'">
                {{ child.gender === 1 ? '男' : '女' }}
              </el-tag>
            </h3>
            <p>{{ child.age }}岁 | {{ child.storeName }}</p>
            <p class="eye-condition">视力状况: {{ child.eyeCondition }}</p>
          </div>
        </div>

        <el-divider />

        <div class="vision-summary">
          <div class="vision-item">
            <span class="label">左眼</span>
            <span class="value" :class="getVisionClass(child.lastVisionTest?.leftEye)">
              {{ child.lastVisionTest?.leftEye || '--' }}
            </span>
          </div>
          <div class="vision-item">
            <span class="label">右眼</span>
            <span class="value" :class="getVisionClass(child.lastVisionTest?.rightEye)">
              {{ child.lastVisionTest?.rightEye || '--' }}
            </span>
          </div>
        </div>

        <div class="action-btns">
          <el-button type="primary" @click.stop="viewReports(child)">查看报告</el-button>
          <el-button @click.stop="viewTrend(child)">视力趋势</el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-card" v-if="children.length === 0">
        <el-empty description="暂无绑定档案">
          <el-button type="primary" @click="showAddChild = true">添加孩子</el-button>
        </el-empty>
      </div>

      <!-- 添加孩子 FAB -->
      <div class="fab-btn" v-if="children.length > 0" @click="showAddChild = true">
        <el-icon :size="28"><Plus /></el-icon>
      </div>
    </template>

    <!-- 添加孩子弹窗 -->
    <el-dialog v-model="showAddChild" title="添加孩子" width="90%" top="5vh">
      <el-form :model="childForm" label-width="80px">
        <el-form-item label="姓名" required>
          <el-input v-model="childForm.name" placeholder="请输入孩子姓名" />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="childForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="所属医院" required>
          <el-select
            v-model="childForm.storeId"
            placeholder="请选择医院"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="store in storeList"
              :key="store.id"
              :label="store.storeName"
              :value="store.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期" required>
          <el-date-picker
            v-model="childForm.birthDate"
            type="date"
            placeholder="选择出生日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="性别" required>
          <el-radio-group v-model="childForm.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="视力状况" required>
          <el-select v-model="childForm.eyeCondition" placeholder="请选择" style="width: 100%">
            <el-option label="正常" value="正常" />
            <el-option label="轻度近视" value="轻度近视" />
            <el-option label="中度近视" value="中度近视" />
            <el-option label="高度近视" value="高度近视" />
            <el-option label="散光" value="散光" />
            <el-option label="弱视" value="弱视" />
          </el-select>
        </el-form-item>
        <el-form-item label="既往病史">
          <el-input v-model="childForm.medicalHistory" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="过敏信息">
          <el-input v-model="childForm.allergyInfo" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddChild = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAddChild">确定</el-button>
      </template>
    </el-dialog>

    <!-- 孩子详情弹窗 -->
    <el-dialog v-model="showDetail" :title="detailChild?.name" width="90%" top="5vh">
      <div v-if="detailChild" class="child-detail">
        <div class="detail-row">
          <span class="detail-label">档案编号</span>
          <span class="detail-value">{{ detailChild.childCode }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">姓名</span>
          <span class="detail-value">{{ detailChild.name }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">性别</span>
          <span class="detail-value">{{ detailChild.gender === 1 ? '男' : '女' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">年龄</span>
          <span class="detail-value">{{ detailChild.age }}岁</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">出生日期</span>
          <span class="detail-value">{{ detailChild.birthDate }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">手机号</span>
          <span class="detail-value">{{ detailChild.phone }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">所属医院</span>
          <span class="detail-value">{{ detailChild.storeName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">视力状况</span>
          <span class="detail-value">{{ detailChild.eyeCondition }}</span>
        </div>
        <div class="detail-row" v-if="detailChild.medicalHistory">
          <span class="detail-label">既往病史</span>
          <span class="detail-value">{{ detailChild.medicalHistory }}</span>
        </div>
        <div class="detail-row" v-if="detailChild.allergyInfo">
          <span class="detail-label">过敏信息</span>
          <span class="detail-value">{{ detailChild.allergyInfo }}</span>
        </div>
        <div class="detail-row" v-if="detailChild.lastVisionTest">
          <span class="detail-label">最近检测</span>
          <span class="detail-value">
            左眼 {{ detailChild.lastVisionTest.leftEye }} |
            右眼 {{ detailChild.lastVisionTest.rightEye }}
          </span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'ParentProfile' })
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserFilled, Plus } from '@element-plus/icons-vue'
import type { Child, CreateChildRequest, Store } from '@/types'
import { childApi, storeApi } from '@/api'

const router = useRouter()

// 状态
const loading = ref(false)
const addLoading = ref(false)
const children = ref<Child[]>([])
const storeList = ref<Store[]>([])
const showAddChild = ref(false)
const showDetail = ref(false)
const detailChild = ref<Child | null>(null)

// 添加孩子表单
const childForm = reactive<CreateChildRequest>({
  name: '',
  phone: '',
  storeId: undefined,
  birthDate: '',
  gender: 1,
  eyeCondition: '',
  medicalHistory: '',
  allergyInfo: ''
})

// 获取孩子列表
const fetchChildren = async () => {
  loading.value = true
  try {
    children.value = await childApi.getMyChildren()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '获取档案列表失败')
  } finally {
    loading.value = false
  }
}

// 视力等级样式
const getVisionClass = (vision?: string) => {
  if (!vision) return ''
  const val = parseFloat(vision)
  if (val >= 5.0) return 'vision-good'
  if (val >= 4.8) return 'vision-normal'
  return 'vision-poor'
}

// 查看孩子详情
const showChildDetail = async (child: Child) => {
  try {
    const detail = await childApi.getChildDetail(child.id)
    detailChild.value = detail
    showDetail.value = true
  } catch {
    detailChild.value = child
    showDetail.value = true
  }
}

// 添加孩子
const handleAddChild = async () => {
  if (!childForm.name) {
    ElMessage.warning('请输入孩子姓名')
    return
  }
  if (!childForm.phone || childForm.phone.length !== 11) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!childForm.storeId) {
    ElMessage.warning('请选择所属医院')
    return
  }
  if (!childForm.birthDate) {
    ElMessage.warning('请选择出生日期')
    return
  }
  if (!childForm.eyeCondition) {
    ElMessage.warning('请选择视力状况')
    return
  }

  addLoading.value = true
  try {
    await childApi.addChild(childForm)
    ElMessage.success('添加成功')
    showAddChild.value = false
    resetChildForm()
    await fetchChildren()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '添加失败')
  } finally {
    addLoading.value = false
  }
}

const resetChildForm = () => {
  childForm.name = ''
  childForm.phone = ''
  childForm.storeId = undefined
  childForm.birthDate = ''
  childForm.gender = 1
  childForm.eyeCondition = ''
  childForm.medicalHistory = ''
  childForm.allergyInfo = ''
}

const viewReports = (child: Child) => {
  router.push(`/report?childId=${child.id}`)
}

const viewTrend = (child: Child) => {
  router.push(`/trend?childId=${child.id}`)
}

// 获取医院列表（添加孩子时选择）
const fetchStoreList = async () => {
  try {
    storeList.value = await storeApi.getStoreList()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '获取医院列表失败')
  }
}

onMounted(() => {
  fetchChildren()
  fetchStoreList()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 16px;

  .loading-skeleton {
    .skeleton-card {
      background: #fff;
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 16px;
      display: flex;
      gap: 16px;
      animation: pulse 1.5s ease-in-out infinite;

      .skeleton-circle {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: #e8e8e8;
      }

      .skeleton-content {
        flex: 1;

        .skeleton-line {
          height: 14px;
          border-radius: 4px;
          background: #e8e8e8;
          margin-bottom: 10px;

          &.long { width: 80%; }
          &.medium { width: 60%; }
          &.short { width: 40%; }
        }
      }
    }
  }

  .child-card {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    cursor: pointer;
    transition: transform 0.2s;

    &:active {
      transform: scale(0.98);
    }

    .child-header {
      display: flex;
      gap: 16px;

      .child-info {
        h3 {
          font-size: 18px;
          margin-bottom: 8px;
          display: flex;
          align-items: center;
          gap: 8px;
        }

        p {
          color: #666;
          font-size: 14px;
          margin-bottom: 4px;
        }

        .eye-condition {
          color: #1890ff;
        }
      }
    }

    .vision-summary {
      display: flex;
      justify-content: space-around;
      margin: 16px 0;

      .vision-item {
        text-align: center;

        .label {
          display: block;
          color: #999;
          font-size: 12px;
          margin-bottom: 4px;
        }

        .value {
          font-size: 28px;
          font-weight: bold;

          &.vision-good { color: #52c41a; }
          &.vision-normal { color: #faad14; }
          &.vision-poor { color: #f5222d; }
        }
      }
    }

    .action-btns {
      display: flex;
      gap: 12px;

      .el-button {
        flex: 1;
      }
    }
  }

  .empty-card {
    background: #fff;
    border-radius: 12px;
    padding: 40px 20px;
  }

  .fab-btn {
    position: fixed;
    right: 20px;
    bottom: 80px;
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: #1890ff;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
    cursor: pointer;
    transition: transform 0.2s;
    z-index: 10;

    &:active {
      transform: scale(0.9);
    }
  }

  .child-detail {
    .detail-row {
      display: flex;
      padding: 12px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
      }

      .detail-label {
        width: 80px;
        color: #999;
        font-size: 14px;
        flex-shrink: 0;
      }

      .detail-value {
        flex: 1;
        color: #333;
        font-size: 14px;
      }
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
