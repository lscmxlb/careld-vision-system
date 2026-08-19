package com.careld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.user.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    @Select("SELECT * FROM sys_menu WHERE status = 1 ORDER BY sort_order ASC, id ASC")
    List<Menu> selectAllMenus();

    @Select("SELECT * FROM sys_menu WHERE status = 1 AND menu_type IN (1, 2) ORDER BY sort_order ASC, id ASC")
    List<Menu> selectVisibleMenus();

    @Select("SELECT * FROM sys_menu WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort_order ASC, id ASC")
    List<Menu> selectByParentId(Long parentId);
}
