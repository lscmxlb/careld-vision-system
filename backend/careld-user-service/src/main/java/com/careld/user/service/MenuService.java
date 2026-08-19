package com.careld.user.service;

import com.careld.user.entity.Menu;
import com.careld.user.dto.MenuRequest;

import java.util.List;

/**
 * 菜单服务
 */
public interface MenuService {

    /**
     * 获取全部菜单树
     */
    List<Menu> getMenuTree();

    /**
     * 新增菜单
     */
    Long createMenu(MenuRequest request);

    /**
     * 修改菜单
     */
    void updateMenu(Long id, MenuRequest request);

    /**
     * 删除菜单
     */
    void deleteMenu(Long id);
}
