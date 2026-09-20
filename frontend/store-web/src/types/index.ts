/**
 * Careld Vision System - 医院Web端类型定义
 */

// ==================== 通用响应 ====================
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// ==================== 用户相关 ====================
export interface User {
  id: number
  username: string
  realName: string
  phone: string
  userType: number
  storeId?: number
  storeName?: string
  status: number
  roles: string[]
  permissions: string[]
}

export interface LoginRequest {
  username?: string
  phone?: string
  password: string
  loginType: number // 1=用户名 2=手机号
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  user: User
}

// ==================== 医院相关 ====================
export interface Store {
  id: number
  storeCode: string
  storeName: string
  address: string
  contactName: string
  contactPhone: string
  businessHours: string
}

// ==================== 儿童档案相关 ====================
export interface Child {
  id: number
  childCode: string
  name: string
  phone: string
  parentName?: string
  relation?: string
  birthDate: string
  age: number
  gender: number
  homeAddress?: string
  school?: string
  deliveryType?: string
  bedtime?: string
  wakeTime?: string
  eyeCondition: string
  nakedVisionBoth?: string
  nakedVisionLeft?: string
  nakedVisionRight?: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
  auditStatus: number // 0=待审核 1=已审核 2=已驳回
  auditRemark?: string
  storeId?: number
  /** 主治医生ID与姓名快照 */
  doctorId?: number
  doctorName?: string
  /** 来源：1=家长自建 2=医生/医院建档 */
  sourceType?: number
  /** 剩余可约次数 */
  remainingCount?: number
  /** 养护次数（含养护中，care_record status 1/2 计数） */
  careCount?: number
  /** 启用状态：1=启用, 0=禁用 */
  status: number
  createdAt: string
}

export interface ChildQuery {
  page?: number
  size?: number
  storeId?: number
  auditStatus?: number
  keyword?: string
  /** 是否包含已禁用档案（默认 false=仅启用） */
  includeDisabled?: boolean
  /** 档案状态：1=正常 0=已禁用 2=已隐藏；不传时按 includeDisabled 语义过滤 */
  status?: number
  /** 状态多值过滤（逗号分隔，如 "0,1"），优先级高于 status */
  statuses?: string
  /** 仅返回可用次数大于该值的档案 */
  remainingCountMin?: number
}

export interface CreateChildRequest {
  name: string
  phone: string
  storeId?: number
  parentName?: string
  relation?: string
  doctorId?: number
  doctorName?: string
  birthDate: string
  gender: number
  homeAddress?: string
  school?: string
  deliveryType?: string
  bedtime?: string
  wakeTime?: string
  eyeCondition: string
  nakedVisionBoth?: string
  nakedVisionLeft?: string
  nakedVisionRight?: string
  medicalHistory?: string
  allergyInfo?: string
  familyHistory?: string
}

// ==================== 排班预约相关 ====================
export interface Schedule {
  id: number
  scheduleDate: string
  timeSlotStart: string
  timeSlotEnd: string
  technicianId: number
  technicianName: string
  maxCapacity: number
  reservedCount: number
  availableCount: number
  status: number
}

export interface Reserve {
  id: number
  childId: number
  childName: string
  /** 儿童性别（1=男 0=女） */
  childGender?: number
  /** 儿童年龄（岁） */
  childAge?: number
  scheduleId?: number
  scheduleDate: string
  timeSlotStart: string
  timeSlotEnd: string
  technicianName?: string
  reserveType: number // 1=初次检测 2=复查 3=养护
  parentName?: string
  parentPhone: string
  remark?: string
  status: number // 1=已预约 2=养护中 3=已完成 4=已取消
  cancelReason?: string
  /** 取消原因类型：1家长原因 2医院原因 */
  cancelReasonType?: number
  /** 新链路：物化时段ID */
  slotId?: string
  /** 开始养护时间 */
  startTime?: string
  executorId?: number
  executorName?: string
  noShowFlag?: number
  refundFlag?: number
  /** 来源：1=家长预约，2=医生/门店预约 */
  source?: number
  /** 预约操作人姓名（门店医生/员工） */
  operatorName?: string
  /** 取消操作人姓名 */
  cancelOperatorName?: string
  /** 是否已调整（1=已调整） */
  adjustFlag?: number
  /** 调整操作人姓名 */
  adjustOperatorName?: string
}

export interface BatchScheduleRequest {
  startDate: string
  endDate: string
  technicianId: number
  timeSlots: Array<{
    start: string
    end: string
    capacity: number
  }>
  weekDays: number[]
}

// ==================== 视力检测相关 ====================
export interface VisionRecord {
  id: number
  childId: number
  childName: string
  testType: number // 1=裸眼 2=矫正
  eyeType: number // 0=双眼 1=左眼 2=右眼
  leftEye: string
  rightEye: string
  visionLevel: string
  testTime: string
  testerName: string
  remark?: string
  beforeAfter: string // before=养护前 after=养护后
  improvement?: string
  createdAt: string
}

export interface VisionCompare {
  childId: number
  childName: string
  beforeTest: {
    leftEye: string
    rightEye: string
  }
  afterTest: {
    leftEye: string
    rightEye: string
  }
  improvement: {
    leftEye: string
    rightEye: string
  }
}

// ==================== TV设备相关 ====================
export interface Device {
  id: number
  deviceCode: string
  deviceName: string
  status: number // 1=在线 0=离线
  calibrationStatus: number // 1=已校准 0=未校准
  lastSyncTime?: string
}

export interface DeviceBindRequest {
  storeId: number
  deviceCode: string
  deviceName: string
}

// ==================== 科室相关 ====================
export interface Department {
  id: number
  storeId: number
  storeName?: string
  deptCode: string
  deptName: string
  deptType: number // 1=儿童保健科 2=妇幼保健科 3=中医科 4=眼科 5=其它科室
  /** 服务电话 */
  servicePhone?: string | null
  /** 收费标准(元)：预约授权自动计费用；null 表示未配置 */
  chargeStandard?: number | null
  sortOrder: number
  status: number // 1=启用 0=禁用
  remark?: string
  createdAt: string
}

// ==================== 数据统计相关 ====================
export interface DashboardStats {
  todayReserves: number
  pendingChildren: number
  activeDevices: number
  todayTests: number
  /** 本月预约（按预约日期，含已取消） */
  monthReserveCount: number
  /** 总预约数量（含已取消） */
  totalReserveCount: number
  /** 本月新增档案（按建档时间） */
  monthChildCount: number
  /** 档案总数（未删除且非已隐藏） */
  totalChildCount: number
  /** 本月养护（按养护日期） */
  monthCareCount: number
  /** 总养护数量 */
  totalCareCount: number
}

export interface WeeklyTrend {
  dates: string[]
  reserveCounts: number[]
  testCounts: number[]
}

export interface VisionStatistics {
  labels: string[]
  beforeValues: number[]
  afterValues: number[]
}

// ==================== 通用分页 ====================
export interface Pagination {
  page: number
  size: number
  total: number
  pages: number
}

export interface PageResult<T> {
  list: T[]
  pagination: Pagination
}

// ==================== 儿童养护服务扩展 ====================
/** 排班规则时段：每条规则在周一~五/周六日下可配置多个不重复时段 */
export interface ScheduleRulePeriod {
  id?: number
  ruleId?: number
  dayType: number // 1=周一~五 2=周六日
  startTime: string
  endTime: string
  capacity: number
}

/** 排班规则例外日（带备注） */
export interface ScheduleRuleException {
  id?: number
  ruleId?: number
  exceptionDate: string
  remark?: string
}

/** 排班规则（自然日区间 + 多时段 + 例外日） */
export interface ScheduleRule {
  id?: number
  storeId?: number
  startDate: string
  endDate: string
  /** 以下旧单时段字段为兼容保留（取每个日类型第一个时段） */
  weekdayStartTime?: string
  weekdayEndTime?: string
  weekdayCapacity?: number
  weekendStartTime?: string
  weekendEndTime?: string
  weekendCapacity?: number
  status?: number
  periods?: ScheduleRulePeriod[]
  exceptions?: ScheduleRuleException[]
  exceptionDates?: string[]
  createdAt?: string
}

/** 每日可约时段（由排班规则物化生成） */
export interface ScheduleSlot {
  id: string
  storeId: number
  slotDate: string
  slotStartTime: string
  slotEndTime: string
  maxCapacity: number
  bookedCount: number
  status: number // 1=开放 0=关闭
}

/** 日历按天聚合名额：booked=Σ已约 B=total=Σ开放时段容量（固定值，不随已约变化） */
export interface SlotDailySummary {
  date: string
  booked: number
  total: number
}

/** 预约规则配置 */
export interface AppointmentConfig {
  id?: number
  storeId?: number
  parentCancelHours: number
  doctorCancelHours: number
  noShowBufferMinutes: number
  autoNoShowHours: number
  autoCompleteHours: number
  /** 预约记录默认显示的状态，逗号分隔（1已预约2养护中3已完成4已取消5已爽约） */
  defaultShowStatuses?: string
}

/** 医务人员 */
export interface MedicalStaff {
  id?: number
  storeId?: number
  name: string
  phone: string
  gender: number // 0未知 1男 2女
  staffRole: number // 1=医生 2=医生助理
  loginPassword?: string
  status: number // 1=启用 0=禁用
  createdAt?: string
}

/** 养护记录 */
export interface CareRecord {
  id: number
  appointmentId?: number
  childId: number
  childName?: string
  parentName?: string
  parentPhone?: string
  storeId?: number
  careDate: string
  timeSlot?: string
  visionBeforeLeft?: string
  visionBeforeRight?: string
  visionBeforeBoth?: string
  visionAfterLeft?: string
  visionAfterRight?: string
  visionAfterBoth?: string
  executorId?: number
  executorName?: string
  status: number // 1=养护中 2=已完成
  /** 儿童性别（1=男 0=女） */
  childGender?: number
  /** 儿童手机号（脱敏） */
  childPhone?: string
  /** 该儿童累计已完成养护次数 */
  careCount?: number
  /** 档案剩余可约次数 */
  remainingCount?: number
  /** 建档时裸眼双眼视力 */
  nakedVisionBoth?: string
  /** 本次养护对应预约单备注 */
  remark?: string
  createdAt?: string
}

/** 服务次数变更流水 */
export interface ChildServiceRecord {
  id: number
  childId: number
  changeType: number // 1=预约授权 2=预约扣减 3=取消退还 4=爽约退还 5=爽约不退还
  changeCount: number
  remainingAfter?: number
  paymentAmount?: number
  paymentMethod?: string // 自费/医保/其他
  doctorId?: number
  doctorName?: string
  remark?: string
  createdAt: string
}

/** 预约每日统计 */
export interface ReserveDailyStatistics {
  statDate: string
  total: number
  completed: number
  cancelled: number
  /** 待养护数量（状态为已预约 1） */
  pending: number
}

// ==================== 日志记录 ====================
export interface OperationLog {
  id: number
  logType: number // 1=操作日志 2=登录日志 3=异常日志
  userId?: number
  userName?: string
  storeId?: number
  storeName?: string
  module: string
  action: string
  description?: string
  requestMethod: string
  requestUrl: string
  requestParams?: string
  responseData?: string
  ipAddress?: string
  userAgent?: string
  deviceType?: string
  executeTime?: number
  status: number // 1=成功 0=失败
  errorMsg?: string
  createdAt: string
}

export interface OperationLogQuery {
  logType?: number
  module?: string
  action?: string
  userName?: string
  keyword?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}
