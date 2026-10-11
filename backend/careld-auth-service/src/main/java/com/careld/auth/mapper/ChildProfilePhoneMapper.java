package com.careld.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 儿童档案监护人手机号同步：家长账号改号后回写其名下档案（与 child-service 同库直连）
 */
@Mapper
public interface ChildProfilePhoneMapper {

    /**
     * 按家长账号批量更新名下档案的监护人手机号（密文 + 掩码）
     */
    @Update("UPDATE child_profile SET phone_mask = #{phoneMask}, phone_encrypted = #{phoneEncrypted}, updated_at = NOW() " +
            "WHERE parent_user_id = #{parentUserId} AND deleted_at IS NULL")
    int updatePhoneByParentUserId(@Param("parentUserId") Long parentUserId,
                                  @Param("phoneMask") String phoneMask,
                                  @Param("phoneEncrypted") String phoneEncrypted);
}
