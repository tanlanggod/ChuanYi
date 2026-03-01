package com.chuanyi.backend.modules.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanyi.backend.modules.order.entity.OrderLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLogEntity> {
}
