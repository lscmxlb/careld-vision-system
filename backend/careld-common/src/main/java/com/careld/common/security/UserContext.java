package com.careld.common.security;

import java.util.List;

/**
 * 当前登录用户上下文（基于 ThreadLocal）
 *
 * <p>由 {@link JwtAuthFilter} 在请求进入时从 JWT 解析并填充，
 * 供 {@code CareldMetaObjectHandler} 自动填充 createdBy/updatedBy、
 * 以及业务代码通过 {@link #getCurrentUserId()} 获取当前操作人。
 * 请求结束后由过滤器清理，避免线程复用导致的上下文泄露。</p>
 */
public final class UserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 获取当前登录用户 ID（未登录时返回 null）
     */
    public static Long getCurrentUserId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static Integer getCurrentUserType() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getUserType();
    }

    public static Long getCurrentStoreId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getStoreId();
    }

    public static Long getCurrentCenterId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getCenterId();
    }

    public static Long getCurrentAgentId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getAgentId();
    }

    public static Long getCurrentDeptId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getDeptId();
    }

    /**
     * 获取当前用户权限列表（未登录时返回 null）
     */
    public static List<String> getPermissions() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getPermissions();
    }

    /**
     * 当前登录用户信息
     */
    public static class CurrentUser {
        private final Long userId;
        private final Integer userType;
        private final Long storeId;
        private final Long centerId;
        private final Long agentId;
        private final Long deptId;
        private final String username;
        private final List<String> permissions;

        public CurrentUser(Long userId, Integer userType, Long storeId, Long deptId, String username) {
            this(userId, userType, storeId, null, null, deptId, username, null);
        }

        public CurrentUser(Long userId, Integer userType, Long storeId, Long deptId, String username, List<String> permissions) {
            this(userId, userType, storeId, null, null, deptId, username, permissions);
        }

        public CurrentUser(Long userId, Integer userType, Long storeId, Long centerId, Long agentId, Long deptId, String username, List<String> permissions) {
            this.userId = userId;
            this.userType = userType;
            this.storeId = storeId;
            this.centerId = centerId;
            this.agentId = agentId;
            this.deptId = deptId;
            this.username = username;
            this.permissions = permissions;
        }

        public Long getUserId() {
            return userId;
        }

        public Integer getUserType() {
            return userType;
        }

        public Long getStoreId() {
            return storeId;
        }

        public Long getCenterId() {
            return centerId;
        }

        public Long getAgentId() {
            return agentId;
        }

        public Long getDeptId() {
            return deptId;
        }

        public String getUsername() {
            return username;
        }

        public List<String> getPermissions() {
            return permissions;
        }
    }
}
