package com.chuanyi.backend.modules.logistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.order.entity.OrderLogEntity;
import com.chuanyi.backend.modules.order.entity.OrderMainEntity;
import com.chuanyi.backend.modules.order.mapper.OrderLogMapper;
import com.chuanyi.backend.modules.order.mapper.OrderMainMapper;
import com.chuanyi.backend.modules.order.util.OrderLogRemarkUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogisticsService {

    private final OrderMainMapper orderMainMapper;
    private final OrderLogMapper orderLogMapper;

    public LogisticsService(OrderMainMapper orderMainMapper,
                           OrderLogMapper orderLogMapper) {
        this.orderMainMapper = orderMainMapper;
        this.orderLogMapper = orderLogMapper;
    }

    public Map<String, Object> track(String carrierCode, String trackingNo) {
        if (!StringUtils.hasText(carrierCode) || !StringUtils.hasText(trackingNo)) {
            throw new BizException(400, "carrierCode and trackingNo are required");
        }
        Long userId = UserContext.currentUserId();
        OrderMainEntity order = findOrderByOrderNo(userId, trackingNo);
        String resolvedCarrierCode = carrierCode;
        String resolvedTrackingNo = trackingNo;

        if (order == null) {
            OrderLogEntity shipLog = findShipLogByTrackingNo(userId, carrierCode, trackingNo);
            if (shipLog != null) {
                order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMainEntity>()
                        .eq(OrderMainEntity::getId, shipLog.getOrderId())
                        .eq(OrderMainEntity::getUserId, userId)
                        .last("LIMIT 1"));
                Map<String, String> kv = OrderLogRemarkUtil.parseKeyValueRemark(shipLog.getRemark());
                if (StringUtils.hasText(kv.get("carrierCode"))) {
                    resolvedCarrierCode = kv.get("carrierCode");
                }
                if (StringUtils.hasText(kv.get("trackingNo"))) {
                    resolvedTrackingNo = kv.get("trackingNo");
                }
            }
        }

        if (order == null) {
            return buildFallbackData(resolvedCarrierCode, resolvedTrackingNo);
        }

        List<OrderLogEntity> logs = orderLogMapper.selectList(new LambdaQueryWrapper<OrderLogEntity>()
                .eq(OrderLogEntity::getOrderId, order.getId())
                .orderByAsc(OrderLogEntity::getId));
        return buildOrderTrackData(resolvedCarrierCode, resolvedTrackingNo, order, logs);
    }

    private OrderMainEntity findOrderByOrderNo(Long userId, String orderNo) {
        return orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getOrderNo, orderNo)
                .eq(OrderMainEntity::getUserId, userId)
                .last("LIMIT 1"));
    }

    private OrderLogEntity findShipLogByTrackingNo(Long userId,
                                                   String carrierCode,
                                                   String trackingNo) {
        List<OrderLogEntity> logs = orderLogMapper.selectList(new LambdaQueryWrapper<OrderLogEntity>()
                .eq(OrderLogEntity::getAction, "SHIP_ORDER")
                .like(OrderLogEntity::getRemark, "trackingNo=" + trackingNo)
                .orderByDesc(OrderLogEntity::getId)
                .last("LIMIT 50"));
        for (OrderLogEntity log : logs) {
            Map<String, String> kv = OrderLogRemarkUtil.parseKeyValueRemark(log.getRemark());
            if (!trackingNo.equals(kv.get("trackingNo"))) {
                continue;
            }
            if (StringUtils.hasText(carrierCode) && StringUtils.hasText(kv.get("carrierCode"))
                    && !carrierCode.equalsIgnoreCase(kv.get("carrierCode"))) {
                continue;
            }
            OrderMainEntity order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMainEntity>()
                    .eq(OrderMainEntity::getId, log.getOrderId())
                    .eq(OrderMainEntity::getUserId, userId)
                    .last("LIMIT 1"));
            if (order != null) {
                return log;
            }
        }
        return null;
    }

    private Map<String, Object> buildOrderTrackData(String carrierCode,
                                                    String trackingNo,
                                                    OrderMainEntity order,
                                                    List<OrderLogEntity> logs) {
        List<Map<String, Object>> nodes = buildNodesFromLogs(order, logs);
        if (nodes.isEmpty()) {
            LocalDateTime createdAt = order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt();
            nodes.add(node(createdAt, "ORDER_CREATED", "System", "Order created"));
        }

        String latestStatus = mapLatestStatus(order.getOrderStatus());
        if ("IN_TRANSIT".equals(latestStatus) && !containsStatus(nodes, "IN_TRANSIT")) {
            nodes.add(node(basedTime(order), "IN_TRANSIT", "Transit Hub", "Shipment is in transit"));
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("carrierCode", carrierCode);
        data.put("trackingNo", trackingNo);
        data.put("latestStatus", latestStatus);
        data.put("nodes", nodes);
        return data;
    }

    private List<Map<String, Object>> buildNodesFromLogs(OrderMainEntity order, List<OrderLogEntity> logs) {
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        for (OrderLogEntity log : logs) {
            String action = log.getAction();
            if ("CREATE_ORDER".equals(action)) {
                nodes.add(node(log.getCreatedAt(), "ORDER_CREATED", "System", "Order created"));
                continue;
            }
            if ("PAY_SUCCESS".equals(action)) {
                nodes.add(node(log.getCreatedAt(), "PAY_SUCCESS", "Payment Center", "Payment confirmed"));
                continue;
            }
            if ("SHIP_ORDER".equals(action)) {
                Map<String, String> kv = OrderLogRemarkUtil.parseKeyValueRemark(log.getRemark());
                String carrier = kv.get("carrierCode");
                String tracking = kv.get("trackingNo");
                String desc = "Shipment sent";
                if (StringUtils.hasText(carrier) || StringUtils.hasText(tracking)) {
                    desc = "Shipment sent " + safe(carrier) + " " + safe(tracking);
                }
                nodes.add(node(log.getCreatedAt(), "IN_TRANSIT", "Carrier", desc.trim()));
                continue;
            }
            if ("CONFIRM_RECEIPT".equals(action)) {
                nodes.add(node(log.getCreatedAt(), "DELIVERED", "Destination", "Shipment delivered"));
                continue;
            }
            if ("CANCEL_ORDER".equals(action) || "PAY_CANCEL".equals(action)) {
                nodes.add(node(log.getCreatedAt(), "CANCELLED", "System", "Order cancelled"));
            }
        }
        if (nodes.isEmpty()) {
            LocalDateTime createdAt = order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt();
            nodes.add(node(createdAt, "ORDER_CREATED", "System", "Order created"));
        }
        return nodes;
    }

    private boolean containsStatus(List<Map<String, Object>> nodes, String status) {
        for (Map<String, Object> node : nodes) {
            if (status.equals(node.get("status"))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> buildFallbackData(String carrierCode, String trackingNo) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("carrierCode", carrierCode);
        data.put("trackingNo", trackingNo);
        data.put("latestStatus", "IN_TRANSIT");
        List<Map<String, Object>> nodes = new ArrayList<Map<String, Object>>();
        nodes.add(node(LocalDateTime.now(), "IN_TRANSIT", "Transit Hub", "Shipment is in transit"));
        data.put("nodes", nodes);
        return data;
    }

    private Map<String, Object> node(LocalDateTime time,
                                     String status,
                                     String location,
                                     String description) {
        Map<String, Object> node = new HashMap<String, Object>();
        node.put("time", time);
        node.put("status", status);
        node.put("location", location);
        node.put("description", description);
        return node;
    }

    private LocalDateTime basedTime(OrderMainEntity order) {
        if (order.getPaidAt() != null) {
            return order.getPaidAt().plusHours(1);
        }
        if (order.getCreatedAt() != null) {
            return order.getCreatedAt().plusHours(1);
        }
        return LocalDateTime.now();
    }

    private String mapLatestStatus(String orderStatus) {
        if (!StringUtils.hasText(orderStatus)) {
            return "PENDING_PAY";
        }
        if ("PENDING_PAY".equals(orderStatus)) {
            return "PENDING_PAY";
        }
        if ("PAID_WAIT_SHIP".equals(orderStatus)) {
            return "WAITING_FOR_SHIPMENT";
        }
        if ("SHIPPED_WAIT_RECEIVE".equals(orderStatus) || "SHIPPED".equals(orderStatus)) {
            return "IN_TRANSIT";
        }
        if ("COMPLETED".equals(orderStatus)) {
            return "DELIVERED";
        }
        if ("CANCELLED".equals(orderStatus)) {
            return "CANCELLED";
        }
        return orderStatus;
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }
}
