package com.careld.common.security;

import java.util.List;

/**
 * 数据权限工具类
 *
 * <p>数据权限沿组织绑定链（运营中心 center → 代理商 agent → 医院 store）严格向下过滤：
 * <ul>
 *   <li>admin（super_admin，拥有 "*" 通配权限）：全局可见，不过滤</li>
 *   <li>总部其他用户(type=1)：下级全可见，但看不到平级总部用户</li>
 *   <li>运营中心(type=4)：强制 centerId = 当前用户 centerId</li>
 *   <li>代理商(type=5)：强制 agentId = 当前用户 agentId</li>
 *   <li>门店维护(type=2)：强制 storeId = 当前用户 storeId</li>
 *   <li>家长(type=3)：强制 userId = 当前用户 userId</li>
 * </ul>
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
    }

    /**
     * 判断当前用户是否为超级管理员（admin账号或拥有 "*" 通配权限）
     */
    public static boolean isSuperAdmin() {
        UserContext.CurrentUser user = UserContext.get();
        if (user == null) {
            return false;
        }
        if ("admin".equals(user.getUsername())) {
            return true;
        }
        List<String> permissions = user.getPermissions();
        return permissions != null && permissions.contains("*");
    }

    /**
     * 当前用户是否为家长（type=3）
     */
    public static boolean isParent() {
        Integer userType = UserContext.getCurrentUserType();
        return userType != null && userType == 3;
    }

    /**
     * 解析门店过滤值（家长按所选医院，不做本店强制）
     *
     * <p>家长可为孩子选择任意医院建档，数据范围应以「本人孩子」为准（parentUserId/childId 维度），
     * 门店只是家长所选医院而非所属范围，不能强制回其注册门店；其他角色沿用 {@link #resolveStoreId}。
     */
    public static Long resolveStoreIdWithParentChoice(Long paramStoreId) {
        if (isParent()) {
            return paramStoreId;
        }
        return resolveStoreId(paramStoreId);
    }

    /**
     * 当前用户是否需要数据过滤（超级管理员和总部用户除外）
     */
    public static boolean isRestricted() {
        if (isSuperAdmin()) {
            return false;
        }
        Integer userType = UserContext.getCurrentUserType();
        return userType == null || userType != 1;
    }

    /**
     * 解析有效的 storeId 过滤值
     * - 超级管理员/总部(type=1)：返回前端传入的 paramStoreId（可 null）
     * - 门店/医院(type=2)：强制返回当前用户的 storeId
     * - 其他类型：如果用户有 storeId 则使用，否则返回 param
     */
    public static Long resolveStoreId(Long paramStoreId) {
        if (isSuperAdmin()) {
            return paramStoreId;
        }
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
     * - 超级管理员/总部(type=1)：返回前端传入的 paramCenterId（可 null）
     * - 其他类型（运营中心/代理商/门店）：强制使用当前用户上下文中的 centerId
     */
    public static Long resolveCenterId(Long paramCenterId) {
        if (isSuperAdmin()) {
            return paramCenterId;
        }
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType == 1) {
            return paramCenterId;
        }
        Long currentCenterId = UserContext.getCurrentCenterId();
        if (currentCenterId != null) {
            return currentCenterId;
        }
        return paramCenterId;
    }

    /**
     * 解析有效的 agentId 过滤值
     * - 超级管理员/总部(type=1)：返回前端传入的 paramAgentId（可 null）
     * - 其他类型（代理商/门店）：强制使用当前用户上下文中的 agentId
     */
    public static Long resolveAgentId(Long paramAgentId) {
        if (isSuperAdmin()) {
            return paramAgentId;
        }
        Integer userType = UserContext.getCurrentUserType();
        if (userType == null || userType == 1) {
            return paramAgentId;
        }
        Long currentAgentId = UserContext.getCurrentAgentId();
        if (currentAgentId != null) {
            return currentAgentId;
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
