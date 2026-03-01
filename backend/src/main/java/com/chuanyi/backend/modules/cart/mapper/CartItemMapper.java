package com.chuanyi.backend.modules.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanyi.backend.modules.cart.entity.CartItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItemEntity> {
}
