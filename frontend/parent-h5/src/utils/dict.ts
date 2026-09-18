/**
 * 业务字典（与 PC 端口径保持一致）
 */
import type { Reserve } from '@/types'

/* ---------------- 预约状态 ---------------- */
/** 筛选用状态：1 已预约 / 2 养护中 / 3 已完成 / 4 已取消 / 5 已爽约 */
export const RESERVE_STATUS_OPTIONS = [
  { value: 1, label: '已预约' },
  { value: 2, label: '养护中' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已爽约' },
]

const RESERVE_STATUS_MAP: Record<number, { label: string; tag: string }> = {
  1: { label: '已预约', tag: 'tag-warning' },
  2: { label: '养护中', tag: 'tag-primary' },
  3: { label: '已完成', tag: 'tag-success' },
  4: { label: '已取消', tag: 'tag-grey' },
  5: { label: '已爽约', tag: 'tag-danger' },
}

/** 记录真实状态值：4 且 noShowFlag=1 视为 5（已爽约） */
export function reserveStatusValue(row: Reserve): number {
  return row.status === 4 && row.noShowFlag === 1 ? 5 : row.status
}

export function reserveStatusLabel(row: Reserve): string {
  return RESERVE_STATUS_MAP[reserveStatusValue(row)]?.label || '未知'
}

export function reserveStatusTag(row: Reserve): string {
  return RESERVE_STATUS_MAP[reserveStatusValue(row)]?.tag || 'tag-grey'
}

/* ---------------- 档案审核状态 ---------------- */
export const AUDIT_STATUS_MAP: Record<number, { label: string; tag: string }> = {
  0: { label: '待审核', tag: 'tag-warning' },
  1: { label: '已通过', tag: 'tag-success' },
  2: { label: '已驳回', tag: 'tag-danger' },
}

export const AUDIT_STATUS_OPTIONS = [
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 2, label: '已驳回' },
]

/* ---------------- 服务次数变更类型 ---------------- */
export const CHANGE_TYPE_MAP: Record<number, { label: string; sign: string }> = {
  1: { label: '预约授权', sign: '+' },
  2: { label: '预约扣减', sign: '-' },
  3: { label: '取消退还', sign: '+' },
  4: { label: '爽约退还', sign: '+' },
  5: { label: '爽约不退还', sign: '-' },
}

/* ---------------- 养护记录状态 ---------------- */
export const CARE_STATUS_MAP: Record<number, { label: string; tag: string }> = {
  1: { label: '养护中', tag: 'tag-primary' },
  2: { label: '已完成', tag: 'tag-success' },
}

/* ---------------- 缴费方式 ---------------- */
export const PAYMENT_METHODS = ['自费支付', '医保-个人余额', '医保-统筹支付', '免费体验', '其它']
/** 需按科室收费标准计费的方式 */
export const CHARGED_METHODS = ['自费支付', '医保-个人余额', '医保-统筹支付']

/* ---------------- 视力状况（多选，互斥规则见 handleEyeCondition） ---------------- */
export const EYE_CONDITION_OPTIONS = ['正常', '轻度近视', '中度近视', '高度近视', '斜视', '弱视', '散光', '远视']
const MYOPIA_OPTIONS = ['轻度近视', '中度近视', '高度近视']

/**
 * 视力状况多选互斥处理（对齐 PC）：
 * - 勾选「正常」→ 仅保留正常
 * - 勾选近视三档之一 → 取消「正常」并保证三档互斥
 */
export function resolveEyeConditions(selected: string[], changed?: string): string[] {
  const last = changed || selected[selected.length - 1]
  if (last === '正常') return ['正常']
  const withoutNormal = selected.filter((v) => v !== '正常')
  if (MYOPIA_OPTIONS.includes(last)) {
    return withoutNormal.filter((v) => v === last || !MYOPIA_OPTIONS.includes(v))
  }
  return withoutNormal
}

/* ---------------- 关系 / 分娩方式 ---------------- */
export const RELATION_OPTIONS = ['妈妈', '爸爸', '爷爷', '奶奶', '外公', '外婆', '其他']
export const DELIVERY_TYPE_OPTIONS = ['顺产', '剖宫产']

/* ---------------- 医务人员角色 ---------------- */
export const STAFF_ROLE_MAP: Record<number, string> = { 1: '医生', 2: '医生助理' }

/* ---------------- 用户类型（登录身份展示） ---------------- */
export const USER_TYPE_MAP: Record<number, string> = {
  1: '超级管理员',
  2: '店长',
  3: '家长',
  4: '总部用户',
  5: '未分配',
  6: '医务人员',
}
