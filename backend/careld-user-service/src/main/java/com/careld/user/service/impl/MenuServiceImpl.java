package com.careld.user.service.impl;

import com.careld.common.exception.BusinessException;
import com.careld.user.dto.MenuRequest;
import com.careld.user.entity.Menu;
import com.careld.user.mapper.MenuMapper;
import com.careld.user.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单服务实现
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    @Override
    public List<Menu> getMenuTree() {
        List<Menu> allMenus = menuMapper.selectAllMenus();
        return buildTree(allMenus);
    }

    private List<Menu> buildTree(List<Menu> menus) {
        Map<Long, Menu> menuMap = new LinkedHashMap<>();
        for (Menu menu : menus) {
            menu.setChildren(new ArrayList<>());
            menuMap.put(menu.getId(), menu);
        }

        List<Menu> tree = new ArrayList<>();
        for (Menu menu : menus) {
            if (menu.getParentId() == null || menu.getParentId() == 0) {
                tree.add(menu);
            } else if (menuMap.containsKey(menu.getParentId())) {
                menuMap.get(menu.getParentId()).getChildren().add(menu);
            }
        }
        return tree;
    }

    @Override
    @Transactional
    public Long createMenu(MenuRequest request) {
        Menu menu = new Menu();
        menu.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        menu.setMenuName(request.getMenuName());
        menu.setMenuType(request.getMenuType() != null ? request.getMenuType() : 2);
        menu.setMenuPath(request.getMenuPath());
        menu.setMenuIcon(request.getMenuIcon());
        menu.setPermissionKey(request.getPermissionKey());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setVisible(request.getVisible() != null ? request.getVisible() : 1);
        menu.setStatus(1);
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Override
    @Transactional
    public void updateMenu(Long id, MenuRequest request) {
        Menu menu = menuMapper.selectById(id);
        if (menu == null) throw new BusinessException(404, "菜单不存在");
        if (request.getParentId() != null) menu.setParentId(request.getParentId());
        if (request.getMenuName() != null) menu.setMenuName(request.getMenuName());
        if (request.getMenuType() != null) menu.setMenuType(request.getMenuType());
        if (request.getMenuPath() != null) menu.setMenuPath(request.getMenuPath());
        if (request.getMenuIcon() != null) menu.setMenuIcon(request.getMenuIcon());
        if (request.getPermissionKey() != null) menu.setPermissionKey(request.getPermissionKey());
        if (request.getSortOrder() != null) menu.setSortOrder(request.getSortOrder());
        if (request.getVisible() != null) menu.setVisible(request.getVisible());
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional
    public void deleteMenu(Long id) {
        // 检查是否有子菜单
        List<Menu> children = menuMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException(400, "该菜单下还有子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }
}
