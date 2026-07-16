package com.careld.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.careld.user.dto.OperationLogResponse;
import com.careld.user.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志（含门店名称）
     */
    @Select("<script>"
            + "SELECT l.id, l.log_type, l.user_id, l.user_name, l.store_id, "
            + "       s.store_name, l.module, l.action, l.description, "
            + "       l.request_method, l.request_url, l.request_params, l.response_data, "
            + "       l.ip_address, l.user_agent, l.device_type, l.execute_time, l.status, "
            + "       l.error_msg, l.created_at "
            + "FROM sys_operation_log l "
            + "LEFT JOIN store_info s ON s.id = l.store_id AND s.deleted_at IS NULL "
            + "WHERE 1=1 "
            + "<if test='logType != null'>AND l.log_type = #{logType} </if>"
            + "<if test='module != null and module.length() > 0'>AND l.module = #{module} </if>"
            + "<if test='keyword != null and keyword.length() > 0'>AND (l.user_name LIKE CONCAT('%',#{keyword},'%') OR l.action LIKE CONCAT('%',#{keyword},'%') OR l.request_url LIKE CONCAT('%',#{keyword},'%')) </if>"
            + "<if test='startTime != null'>AND l.created_at &gt;= #{startTime} </if>"
            + "<if test='endTime != null'>AND l.created_at &lt;= #{endTime} </if>"
            + "ORDER BY l.created_at DESC"
            + "</script>")
    IPage<OperationLogResponse> selectPageWithStore(IPage<OperationLogResponse> page,
                                                     @Param("logType") Integer logType,
                                                     @Param("module") String module,
                                                     @Param("keyword") String keyword,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 单条详情（含门店名称）
     */
    @Select("SELECT l.id, l.log_type, l.user_id, l.user_name, l.store_id, s.store_name, "
            + "       l.module, l.action, l.description, l.request_method, l.request_url, "
            + "       l.request_params, l.response_data, l.ip_address, l.user_agent, "
            + "       l.device_type, l.execute_time, l.status, l.error_msg, l.created_at "
            + "FROM sys_operation_log l "
            + "LEFT JOIN store_info s ON s.id = l.store_id AND s.deleted_at IS NULL "
            + "WHERE l.id = #{id}")
    OperationLogResponse selectDetailById(@Param("id") Long id);
}
