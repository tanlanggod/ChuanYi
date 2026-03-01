package com.chuanyi.backend.modules.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuanyi.backend.modules.payment.entity.PaymentRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecordEntity> {
}
