package com.careld.notify.mapper;

import lombok.Data;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 通知服务跨域只读数据源
 *
 * <p>沿用本项目「服务间不互相 HTTP 调用、按需直读共享库表」的约定：
 * 发送时需要儿童联系方式（child-service 域）、家长账号与微信绑定（user-service 域）、
 * 医院名称（store-service 域）、通道凭据（auth-service 维护的 sys_config）。</p>
 */
@Mapper
public interface NotifySourceMapper {

    @Data
    class ChildContact {
        private Long id;
        private Long storeId;
        private String nameMask;
        private String nameEncrypted;
        private String phoneMask;
        private String phoneEncrypted;
        private Long parentUserId;
    }

    @Data
    class ChildNameCandidate {
        private Long id;
        private String nameMask;
        private String nameEncrypted;
    }

    @Select("SELECT id, store_id, name_mask, name_encrypted, phone_mask, phone_encrypted, parent_user_id "
            + "FROM child_profile WHERE id = #{childId} AND deleted_at IS NULL")
    ChildContact selectChildContact(@Param("childId") Long childId);

    @Select("SELECT id, name_mask, name_encrypted FROM child_profile WHERE store_id = #{storeId} "
            + "AND deleted_at IS NULL ORDER BY id DESC LIMIT 1000")
    List<ChildNameCandidate> selectChildNameCandidates(@Param("storeId") Long storeId);

    @Select("SELECT id, username, real_name AS realName, phone, wechat_openid AS wechatOpenid, "
            + "wechat_bound_at AS wechatBoundAt FROM sys_user WHERE id = #{userId} AND deleted_at IS NULL")
    Map<String, Object> selectUserById(@Param("userId") Long userId);

    @Select("SELECT store_name FROM store_info WHERE id = #{storeId} AND deleted_at IS NULL")
    String selectStoreName(@Param("storeId") Long storeId);

    @Select("SELECT config_value FROM sys_config WHERE config_key = #{key} LIMIT 1")
    String selectConfigValue(@Param("key") String key);

    @Insert("INSERT INTO sys_config (config_key, config_value, config_group, remark) "
            + "VALUES (#{key}, #{value}, #{group}, #{remark}) "
            + "ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), "
            + "config_group = VALUES(config_group), remark = VALUES(remark)")
    int upsertConfig(@Param("key") String key, @Param("value") String value,
                     @Param("group") String group, @Param("remark") String remark);

    @Update("UPDATE sys_user SET wechat_openid = #{openid}, wechat_bound_at = NOW(), updated_at = NOW() "
            + "WHERE id = #{userId}")
    int bindWechat(@Param("userId") Long userId, @Param("openid") String openid);

    @Update("UPDATE sys_user SET wechat_openid = NULL, wechat_bound_at = NULL, updated_at = NOW() "
            + "WHERE id = #{userId}")
    int unbindWechat(@Param("userId") Long userId);

    /** 取关时按 openid 反查并清除绑定，避免继续向已失效的 openid 发送 */
    @Update("UPDATE sys_user SET wechat_openid = NULL, wechat_bound_at = NULL, updated_at = NOW() "
            + "WHERE wechat_openid = #{openid} AND deleted_at IS NULL")
    int unbindByOpenid(@Param("openid") String openid);

    @Select("SELECT id FROM sys_user WHERE wechat_openid = #{openid} AND user_type = 3 AND deleted_at IS NULL LIMIT 1")
    Long selectUserIdByOpenid(@Param("openid") String openid);
}
