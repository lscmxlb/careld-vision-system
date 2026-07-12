package com.careld.vision.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.vision.entity.VisionTestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
@Mapper
public interface VisionMapper extends BaseMapper<VisionTestRecord> {
    @Select("SELECT * FROM vision_test_record WHERE child_id = #{childId} AND deleted_at IS NULL ORDER BY created_at DESC")
    List<VisionTestRecord> selectByChildId(Long childId);
    @Select("SELECT * FROM vision_test_record WHERE child_id = #{childId} AND reserve_id = #{reserveId} AND deleted_at IS NULL")
    List<VisionTestRecord> selectByChildAndReserve(@Param("childId") Long childId, @Param("reserveId") Long reserveId);
}
