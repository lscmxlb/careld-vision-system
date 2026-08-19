package com.careld.user.service;

import com.careld.user.entity.Menu;

import java.util.List;

/**
 * 权限服务
 */
public interface PermissionService {

    /**
     * 获取用户权限标识列表
     */
    List<String> getPermissionKeysByUserId(Long userId);

    /**
     * 获取用户可见菜单树（仅目录和菜单，不含按钮）
     */
    List<Menu> getUserMenuTree(Long userId);

    /**
     * 清除用户权限缓存
     */
    void clearPermissionCache(Long userId);
}
