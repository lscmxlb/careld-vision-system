package com.careld.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.careld.auth.entity.UserConsent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户同意记录Mapper
 */
@Mapper
public interface UserConsentMapper extends BaseMapper<UserConsent> {
}
