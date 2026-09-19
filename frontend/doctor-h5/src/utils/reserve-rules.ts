/**
 * 预约操作可用性规则（与 PC 端 hospital 端完全一致）
 */
import type { Reserve } from '@/types'
import { todayStr } from './format'

/** 今天（YYYY-MM-DD）：开始养护仅预约当天可操作 */
export function isReserveToday(row: Reserve): boolean {
  return row.scheduleDate === todayStr()
}

/** 已逾期：当前时间超过预约时段结束时间（逾期后不可调整/取消，只能标记爽约） */
export function isOverdue(row: Reserve): boolean {
  if (!row.scheduleDate || !row.timeSlotEnd) return false
  const end = new Date(`${row.scheduleDate}T${row.timeSlotEnd.slice(0, 8)}`)
  if (Number.isNaN(end.getTime())) return false
  return end.getTime() < Date.now()
}

/** 开始/完成养护按钮：已预约当天可开始；当天已爽约（客户当天到店）同样可开始；养护中可完成 */
export function canOperateCare(row: Reserve): boolean {
  if (row.status === 2) return true
  if (!isReserveToday(row)) return false
  return row.status === 1 || (row.status === 4 && row.noShowFlag === 1)
}

/** 预约调整：仅已预约且未逾期 */
export function canAdjust(row: Reserve): boolean {
  return row.status === 1 && !isOverdue(row)
}

/** 标记爽约：仅已预约，且当天或已逾期 */
export function canMarkNoShow(row: Reserve): boolean {
  return row.status === 1 && (isReserveToday(row) || isOverdue(row))
}

/** 取消预约：仅已预约且未逾期 */
export function canCancel(row: Reserve): boolean {
  return row.status === 1 && !isOverdue(row)
}

/** 取消/调整被禁用时的原因文案 */
export function overdueTip(row: Reserve): string {
  if (row.status === 1 && isOverdue(row)) return '已超过预约时段，不可取消/调整，仅可标记爽约'
  return ''
}
