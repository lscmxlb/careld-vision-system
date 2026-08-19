package com.careld.user.service.impl;

import com.alibaba.fastjson2.JSON;
import com.careld.common.exception.BusinessException;
import com.careld.user.entity.Menu;
import com.careld.user.entity.RoleMenu;
import com.careld.user.mapper.MenuMapper;
import com.careld.user.mapper.RoleMenuMapper;
import com.careld.user.mapper.UserRoleMapper;
import com.careld.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "careld:perm:user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public List<String> getPermissionKeysByUserId(Long userId) {
        if (userId == null) return Collections.emptyList();

        // 1. 查缓存
        String cacheKey = CACHE_PREFIX + userId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSON.parseArray(cached, String.class);
            }
        } catch (Exception e) {
            log.warn("Redis缓存查询失败，回源数据库: {}", e.getMessage());
        }

        // 2. 查用户角色
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return Collections.emptyList();

        // 3. 查角色-菜单权限
        String roleIdStr = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<RoleMenu> roleMenus = roleMenuMapper.selectByRoleIds(roleIdStr);
        if (roleMenus.isEmpty()) return Collections.emptyList();

        // 4. 构建权限标识列表
        Set<String> permissions = new HashSet<>();
        for (RoleMenu rm : roleMenus) {
            Menu menu = rm.getMenu();
            if (menu == null || menu.getPermissionKey() == null) continue;

            // 添加权限标识本身
            permissions.add(menu.getPermissionKey());

            // 按钮级(type=3)权限的permission_key本身就是完整权限（如 organization:center:create），
            // 不需要从actions派生，直接跳过
            if (menu.getMenuType() != null && menu.getMenuType() == 3) continue;

            // 菜单级(type=2)权限从actions派生操作权限
            // 菜单的permission_key以:view结尾，如 organization:center:view
            if (rm.getActions() != null && !rm.getActions().isBlank()) {
                try {
                    List<String> actions = JSON.parseArray(rm.getActions(), String.class);
                    String baseKey = menu.getPermissionKey();
                    // 将 :view 替换为 :action
                    for (String action : actions) {
                        if ("view".equals(action)) {
                            permissions.add(baseKey);
                        } else {
                            // 如 organization:center:view -> organization:center:create
                            String actionKey = baseKey.replaceAll(":view$", ":" + action);
                            if (!actionKey.equals(baseKey)) {
                                permissions.add(actionKey);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析actions失败: {}", rm.getActions());
                }
            }
        }

        List<String> result = new ArrayList<>(permissions);

        // 5. 写缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(result), CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis缓存写入失败: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public List<Menu> getUserMenuTree(Long userId) {
        if (userId == null) return Collections.emptyList();

        // 查用户角色
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return Collections.emptyList();

        // 查角色-菜单权限
        String roleIdStr = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<RoleMenu> roleMenus = roleMenuMapper.selectByRoleIds(roleIdStr);

        // 收集用户拥有的菜单ID（仅目录和菜单类型）
        Set<Long> userMenuIds = new HashSet<>();
        for (RoleMenu rm : roleMenus) {
            Menu menu = rm.getMenu();
            if (menu != null && menu.getMenuType() != null && menu.getMenuType() <= 2) {
                userMenuIds.add(menu.getId());
            }
        }
        if (userMenuIds.isEmpty()) return Collections.emptyList();

        // 查全部菜单（目录和菜单），用于补全父级目录
        List<Menu> allMenus = menuMapper.selectVisibleMenus();
        Map<Long, Menu> allMenuMap = new LinkedHashMap<>();
        for (Menu m : allMenus) {
            allMenuMap.put(m.getId(), m);
        }

        // 自动补全父级目录：如果子菜单已分配但父目录未分配，自动添加父目录
        Set<Long> expandedIds = new LinkedHashSet<>(userMenuIds);
        for (Long menuId : userMenuIds) {
            Menu m = allMenuMap.get(menuId);
            Long parentId = (m != null) ? m.getParentId() : null;
            while (parentId != null && parentId > 0 && !expandedIds.contains(parentId)) {
                expandedIds.add(parentId);
                Menu parent = allMenuMap.get(parentId);
                parentId = (parent != null) ? parent.getParentId() : null;
            }
        }

        // 构建菜单Map（按 sort_order 顺序，仅包含用户有权限的+父级目录）
        Map<Long, Menu> menuMap = new LinkedHashMap<>();
        for (Menu m : allMenus) {
            if (expandedIds.contains(m.getId()) && m.getVisible() != null && m.getVisible() == 1) {
                Menu copy = new Menu();
                copy.setId(m.getId());
                copy.setParentId(m.getParentId());
                copy.setMenuName(m.getMenuName());
                copy.setMenuType(m.getMenuType());
                copy.setMenuPath(m.getMenuPath());
                copy.setMenuIcon(m.getMenuIcon());
                copy.setPermissionKey(m.getPermissionKey());
                copy.setSortOrder(m.getSortOrder());
                copy.setChildren(new ArrayList<>());
                menuMap.put(m.getId(), copy);
            }
        }

        // 构建树
        List<Menu> tree = new ArrayList<>();
        for (Menu menu : menuMap.values()) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                tree.add(menu);
            } else if (menuMap.containsKey(menu.getParentId())) {
                menuMap.get(menu.getParentId()).getChildren().add(menu);
            }
        }

        return tree;
    }

    @Override
    public void clearPermissionCache(Long userId) {
        try {
            redisTemplate.delete(CACHE_PREFIX + userId);
        } catch (Exception e) {
            log.warn("清除权限缓存失败: {}", e.getMessage());
        }
    }
}
