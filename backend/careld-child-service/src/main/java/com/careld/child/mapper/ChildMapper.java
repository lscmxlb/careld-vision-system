package com.careld.child.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.child.entity.ChildProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
@Mapper
public interface ChildMapper extends BaseMapper<ChildProfile> {
    @Select("<script>SELECT * FROM child_profile WHERE deleted_at IS NULL " +
            "<if test='storeId != null'>AND store_id = #{storeId} </if>" +
            "<if test='auditStatus != null'>AND audit_status = #{auditStatus} </if>" +
            "<if test='parentUserId != null'>AND parent_user_id = #{parentUserId} </if>" +
            "<if test='keyword != null'>AND (name_mask LIKE CONCAT('%',#{keyword},'%') OR phone_mask LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "ORDER BY created_at DESC</script>")
    List<ChildProfile> selectByCondition(@Param("storeId") Long storeId,
                                          @Param("auditStatus") Integer auditStatus,
                                          @Param("parentUserId") Long parentUserId,
                                          @Param("keyword") String keyword);
    @Select("SELECT * FROM child_profile WHERE store_id = #{storeId} AND deleted_at IS NULL")
    List<ChildProfile> selectByStoreId(Long storeId);
}
