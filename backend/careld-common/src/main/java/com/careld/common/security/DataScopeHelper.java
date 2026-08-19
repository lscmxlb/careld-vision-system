package com.careld.common.security;

/**
 * 数据权限工具类
 *
 * <p>根据当前登录用户的 userType 自动注入组织过滤条件：
 * <ul>
 *   <li>type=1 总部：不限制，使用前端传入的参数</li>
 *   <li>type=4 运营中心：强制 centerId = 当前用户 centerId</li>
 *   <li>type=5 代理商：强制 agentId = 当前用户 agentId</li>
 *   <li>type=2 门店维护：强制 storeId = 当前用户 storeId</li>
 *   <li>type=3 家长：强制 userId = 当前用户 userId</li>
 * </ul>
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
    }

    /**
     * 当前用户是否需要数据过滤（非总部用户需要）
     */
    public static boolean isRestricted() {
        Integer userType = UserContext.getCurrentUserType();
        return userType == null || userType != 1;
    }

    /**
     * 解析有效的 storeId 过滤值
     * - 总部(type=1)：返回前端传入的 paramStoreId（可 null）
     * - 门店/医院(type=2)：强制返回当前用户的 storeId
     * - 其他类型：如果用户有 storeId 则使用，否则返回 param
     */
    public static Long resolveStoreId(Long paramStoreId) {
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType == 1) {
            return paramStoreId;
        }
        Long currentUserStoreId = UserContext.getCurrentStoreId();
        if (currentUserStoreId != null) {
            return currentUserStoreId;
        }
        return paramStoreId;
    }

    /**
     * 解析有效的 centerId 过滤值
     * - 总部(type=1)：返回前端传入的 paramCenterId（可 null）
     * - 运营中心(type=4)：强制返回当前用户的 centerId
     * - 其他类型：如果用户有 centerId 则使用，否则返回 param
     */
    public static Long resolveCenterId(Long paramCenterId) {
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType == 1) {
            return paramCenterId;
        }
        if (userType == 4) {
            Long currentCenterId = UserContext.getCurrentCenterId();
            if (currentCenterId != null) {
                return currentCenterId;
            }
        }
        return paramCenterId;
    }

    /**
     * 解析有效的 agentId 过滤值
     * - 总部(type=1)：返回前端传入的 paramAgentId（可 null）
     * - 代理商(type=5)：强制返回当前用户的 agentId
     * - 其他类型：如果用户有 agentId 则使用，否则返回 param
     */
    public static Long resolveAgentId(Long paramAgentId) {
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType == 1) {
            return paramAgentId;
        }
        if (userType == 5) {
            Long currentAgentId = UserContext.getCurrentAgentId();
            if (currentAgentId != null) {
                return currentAgentId;
            }
        }
        return paramAgentId;
    }

    /**
     * 解析有效的 userId 过滤值（用于个人数据范围）
     * - 总部(type=1)：返回前端传入的 paramUserId（可 null）
     * - 家长(type=3)：强制返回当前用户 userId
     * - 其他类型：返回 param
     */
    public static Long resolveUserId(Long paramUserId) {
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType == 1) {
            return paramUserId;
        }
        if (userType == 3) {
            return UserContext.getCurrentUserId();
        }
        return paramUserId;
    }
}
