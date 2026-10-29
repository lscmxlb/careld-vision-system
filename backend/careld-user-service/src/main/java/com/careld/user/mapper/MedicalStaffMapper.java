package com.careld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.careld.user.entity.MedicalStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MedicalStaffMapper extends BaseMapper<MedicalStaff> {

    @Select("SELECT * FROM medical_staff WHERE store_id = #{storeId} AND phone = #{phone} AND deleted_at IS NULL LIMIT 1")
    MedicalStaff selectByStoreAndPhone(@Param("storeId") Long storeId, @Param("phone") String phone);

    /**
     * 删除前改名并释放登录手机号：姓名追加「[已经删除]」，phone 改为 DEL-{id} 让出唯一键，同时禁用。
     * 名字列 varchar(64)，后缀 6 字符，最多保留 58 字符。
     */
    @Update("UPDATE medical_staff SET name = CONCAT(LEFT(name, 58), '[已经删除]'), "
            + "phone = CONCAT('DEL-', id), status = 0, updated_at = NOW() "
            + "WHERE id = #{id} AND deleted_at IS NULL AND name NOT LIKE '%[已经删除]'")
    int markDeleted(@Param("id") Long id);

    /** 手机号码查询：按手机号模糊匹配全部医务人员记录 */
    @Select("SELECT * FROM medical_staff WHERE phone LIKE CONCAT('%', #{phone}, '%') AND deleted_at IS NULL ORDER BY id")
    java.util.List<MedicalStaff> selectListByPhoneLike(@Param("phone") String phone);

    /** 已删除医务人员分页（自定义 SQL 绕过 @TableLogic 的 deleted_at IS NULL 过滤） */
    @Select("<script>SELECT * FROM medical_staff WHERE deleted_at IS NOT NULL "
            + "<if test='storeId != null'>AND store_id = #{storeId} </if>"
            + "<if test='staffRole != null'>AND staff_role = #{staffRole} </if>"
            + "<if test='keyword != null and keyword != \"\"'>AND (name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR phone LIKE CONCAT('%', #{keyword}, '%')) </if>"
            + "ORDER BY deleted_at DESC, id DESC</script>")
    Page<MedicalStaff> selectDeletedPage(Page<MedicalStaff> page,
                                         @Param("storeId") Long storeId,
                                         @Param("keyword") String keyword,
                                         @Param("staffRole") Integer staffRole);
}
