package com.chuanyi.backend.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanyi.backend.modules.auth.entity.UserAccountEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {
}
