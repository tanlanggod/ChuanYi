package com.chuanyi.backend.modules.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.common.util.MockUtil;
import com.chuanyi.backend.modules.order.entity.OrderLogEntity;
import com.chuanyi.backend.modules.order.entity.OrderMainEntity;
import com.chuanyi.backend.modules.order.mapper.OrderLogMapper;
import com.chuanyi.backend.modules.order.mapper.OrderMainMapper;
import com.chuanyi.backend.modules.payment.entity.PaymentRecordEntity;
import com.chuanyi.backend.modules.payment.mapper.PaymentRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentMockService {

    private final OrderMainMapper orderMainMapper;
    private final OrderLogMapper orderLogMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public PaymentMockService(OrderMainMapper orderMainMapper,
                              OrderLogMapper orderLogMapper,
                              PaymentRecordMapper paymentRecordMapper) {
        this.orderMainMapper = orderMainMapper;
        this.orderLogMapper = orderLogMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    public Map<String, Object> pay(Map<String, Object> body) {
        String orderNo = parseText(body.get("orderNo"));
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException(400, "orderNo is required");
        }

        Long userId = UserContext.currentUserId();
        OrderMainEntity order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getOrderNo, orderNo)
                .eq(OrderMainEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(404, "order not found");
        }

        PaymentRecordEntity payment = new PaymentRecordEntity();
        payment.setPayNo(MockUtil.nextNo("P"));
        payment.setOrderId(order.getId());
        payment.setChannel("MOCK");
        payment.setPayStatus("PROCESSING");
        payment.setReqJson(body.toString());
        payment.setRespJson("{}");
        payment.setCreatedAt(LocalDateTime.now());
        paymentRecordMapper.insert(payment);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("payNo", payment.getPayNo());
        data.put("payStatus", payment.getPayStatus());
        return data;
    }

    public Boolean callback(Map<String, Object> body) {
        String payNo = parseText(body.get("payNo"));
        String orderNo = parseText(body.get("orderNo"));
        String payStatus = parseText(body.get("payStatus"));
        if (!StringUtils.hasText(payNo) || !StringUtils.hasText(orderNo) || !StringUtils.hasText(payStatus)) {
            throw new BizException(400, "payNo/orderNo/payStatus are required");
        }

        OrderMainEntity order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(404, "order not found");
        }

        PaymentRecordEntity payment = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecordEntity>()
                .eq(PaymentRecordEntity::getPayNo, payNo)
                .eq(PaymentRecordEntity::getOrderId, order.getId())
                .last("LIMIT 1"));
        if (payment == null) {
            throw new BizException(404, "payment record not found");
        }

        if ("SUCCESS".equals(payment.getPayStatus()) && "SUCCESS".equalsIgnoreCase(payStatus)) {
            return Boolean.TRUE;
        }

        payment.setPayStatus(payStatus.toUpperCase());
        payment.setRespJson(body.toString());
        if ("SUCCESS".equalsIgnoreCase(payStatus)) {
            payment.setPaidAt(LocalDateTime.now());
        }
        paymentRecordMapper.updateById(payment);

        String oldStatus = order.getOrderStatus();
        String newStatus = oldStatus;
        String action = null;
        if ("SUCCESS".equalsIgnoreCase(payStatus)) {
            order.setPayStatus("PAID");
            order.setOrderStatus("PAID_WAIT_SHIP");
            order.setPaidAt(LocalDateTime.now());
            newStatus = order.getOrderStatus();
            action = "PAY_SUCCESS";
        } else if ("CANCELLED".equalsIgnoreCase(payStatus)) {
            order.setPayStatus("UNPAID");
            order.setOrderStatus("CANCELLED");
            newStatus = order.getOrderStatus();
            action = "PAY_CANCEL";
        } else {
            order.setPayStatus("UNPAID");
        }
        orderMainMapper.updateById(order);
        if (action != null) {
            insertOrderLog(order.getId(), oldStatus, newStatus, action, "payNo=" + payment.getPayNo());
        }
        return Boolean.TRUE;
    }

    private void insertOrderLog(Long orderId,
                                String fromStatus,
                                String toStatus,
                                String action,
                                String remark) {
        OrderLogEntity log = new OrderLogEntity();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorType("SYSTEM");
        log.setOperatorId(0L);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        orderLogMapper.insert(log);
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
