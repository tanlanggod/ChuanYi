package com.chuanyi.backend.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanyi.backend.modules.order.entity.OrderMainEntity;
import com.chuanyi.backend.modules.order.mapper.OrderMainMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final OrderMainMapper orderMainMapper;

    public AdminDashboardService(OrderMainMapper orderMainMapper) {
        this.orderMainMapper = orderMainMapper;
    }

    public Map<String, Object> overview() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);

        Long totalOrders = orderMainMapper.selectCount(new LambdaQueryWrapper<OrderMainEntity>());
        Long pendingPay = countByStatus("PENDING_PAY");
        Long paidWaitShip = countByStatus("PAID_WAIT_SHIP");
        Long shippedWaitReceive = countByStatus("SHIPPED_WAIT_RECEIVE");
        Long completed = countByStatus("COMPLETED");
        Long cancelled = countByStatus("CANCELLED");
        Long todayOrders = orderMainMapper.selectCount(new LambdaQueryWrapper<OrderMainEntity>()
                .ge(OrderMainEntity::getCreatedAt, dayStart)
                .lt(OrderMainEntity::getCreatedAt, dayEnd));

        List<OrderMainEntity> todayPaidOrders = orderMainMapper.selectList(new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getPayStatus, "PAID")
                .ge(OrderMainEntity::getPaidAt, dayStart)
                .lt(OrderMainEntity::getPaidAt, dayEnd));
        BigDecimal todayRevenue = BigDecimal.ZERO;
        for (OrderMainEntity order : todayPaidOrders) {
            if (order.getPayAmount() != null) {
                todayRevenue = todayRevenue.add(order.getPayAmount());
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("totalOrders", safe(totalOrders));
        data.put("pendingPayCount", safe(pendingPay));
        data.put("paidWaitShipCount", safe(paidWaitShip));
        data.put("shippedWaitReceiveCount", safe(shippedWaitReceive));
        data.put("completedCount", safe(completed));
        data.put("cancelledCount", safe(cancelled));
        data.put("todayOrderCount", safe(todayOrders));
        data.put("todayRevenue", todayRevenue);
        return data;
    }

    private Long countByStatus(String status) {
        return orderMainMapper.selectCount(new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getOrderStatus, status));
    }

    private long safe(Long value) {
        return value == null ? 0L : value.longValue();
    }
}
