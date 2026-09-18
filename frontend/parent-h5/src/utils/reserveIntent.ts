/**
 * 「立即预约」预选孩子传递
 * 预约页是 tabBar 页面，switchTab 不支持 query 参数，
 * 且 tab 页 onLoad 仅首次触发，故用一次性存储传递预选意图。
 */
const KEY = 'careld_parent_reserve_child_id'

export function setReserveChildId(childId: number | string) {
  uni.setStorageSync(KEY, String(childId))
}

/** 读取并清除预选意图（避免后续进入 tab 时重复应用） */
export function takeReserveChildId(): number {
  const raw = uni.getStorageSync(KEY)
  if (!raw) return 0
  uni.removeStorageSync(KEY)
  return Number(raw) || 0
}
