package com.careld.common.security;

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

    public static Long getCurrentDeptId() {
        CurrentUser user = HOLDER.get();
        return user == null ? null : user.getDeptId();
    }

    /**
     * 当前登录用户信息
     */
    public static class CurrentUser {
        private final Long userId;
        private final Integer userType;
        private final Long storeId;
        private final Long deptId;
        private final String username;

        public CurrentUser(Long userId, Integer userType, Long storeId, Long deptId, String username) {
            this.userId = userId;
            this.userType = userType;
            this.storeId = storeId;
            this.deptId = deptId;
            this.username = username;
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

        public Long getDeptId() {
            return deptId;
        }

        public String getUsername() {
            return username;
        }
    }
}
