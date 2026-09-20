package com.careld.child.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * sys_user 中家长账号的最小映射（建档时按监护人手机号同步建号用）
 */
@Data
@TableName("sys_user")
public class ParentUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private Integer userType;
    private Long storeId;
    private Long centerId;
    private Long agentId;
    private Integer status;
}
