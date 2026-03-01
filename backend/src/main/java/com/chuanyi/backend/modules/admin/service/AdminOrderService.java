package com.chuanyi.backend.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.order.entity.OrderItemEntity;
import com.chuanyi.backend.modules.order.entity.OrderLogEntity;
import com.chuanyi.backend.modules.order.entity.OrderMainEntity;
import com.chuanyi.backend.modules.order.mapper.OrderItemMapper;
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
public class AdminOrderService {

    private static final String STATUS_PENDING_PAY = "PENDING_PAY";
    private static final String STATUS_PAID_WAIT_SHIP = "PAID_WAIT_SHIP";
    private static final String STATUS_SHIPPED_WAIT_RECEIVE = "SHIPPED_WAIT_RECEIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;

    public AdminOrderService(OrderMainMapper orderMainMapper,
                             OrderItemMapper orderItemMapper,
                             OrderLogMapper orderLogMapper) {
        this.orderMainMapper = orderMainMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderLogMapper = orderLogMapper;
    }

    public Map<String, Object> list(String status, String keyword, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<OrderMainEntity>()
                .orderByDesc(OrderMainEntity::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrderMainEntity::getOrderStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OrderMainEntity::getOrderNo, keyword)
                    .or().like(OrderMainEntity::getRemark, keyword));
        }
        Page<OrderMainEntity> page = orderMainMapper.selectPage(new Page<OrderMainEntity>(pageNo, pageSize), wrapper);

        List<Map<String, Object>> mapped = new ArrayList<Map<String, Object>>();
        for (OrderMainEntity order : page.getRecords()) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("orderNo", order.getOrderNo());
            row.put("userId", order.getUserId());
            row.put("orderStatus", order.getOrderStatus());
            row.put("payStatus", order.getPayStatus());
            row.put("totalAmount", order.getTotalAmount());
            row.put("payAmount", order.getPayAmount());
            row.put("createdAt", order.getCreatedAt());
            row.put("paidAt", order.getPaidAt());
            row.put("shipping", extractShipping(listLogs(order.getId())));
            row.put("itemPreview", fetchItemPreview(order.getId()));
            mapped.add(row);
        }

        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", pageNo);
        pageData.put("pageSize", pageSize);
        pageData.put("total", page.getTotal());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", mapped);
        data.put("page", pageData);
        return data;
    }

    public Map<String, Object> detail(String orderNo) {
        OrderMainEntity order = requireOrder(orderNo);
        List<OrderItemEntity> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, order.getId())
                .orderByAsc(OrderItemEntity::getId));
        List<OrderLogEntity> logs = listLogs(order.getId());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("userId", order.getUserId());
        data.put("orderStatus", order.getOrderStatus());
        data.put("payStatus", order.getPayStatus());
        data.put("totalAmount", order.getTotalAmount());
        data.put("payAmount", order.getPayAmount());
        data.put("remark", order.getRemark());
        data.put("addressSnapshot", parseAddressSnapshot(order.getAddressSnapshotJson()));
        data.put("items", items);
        data.put("shipping", extractShipping(logs));
        data.put("logs", mapLogs(logs));
        data.put("createdAt", order.getCreatedAt());
        data.put("paidAt", order.getPaidAt());
        return data;
    }

    public Map<String, Object> cancel(String orderNo, Map<String, Object> body, Long adminId) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        OrderMainEntity order = requireOrder(orderNo);
        if (!STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            throw new BizException(400, "only pending pay order can be cancelled");
        }
        String oldStatus = order.getOrderStatus();
        String remark = parseText(request.get("remark"));

        order.setOrderStatus(STATUS_CANCELLED);
        order.setPreoccupyStatus("RELEASED");
        orderMainMapper.updateById(order);
        insertOrderLog(order.getId(), oldStatus, STATUS_CANCELLED, "CANCEL_ORDER", remark, adminId);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        return data;
    }

    public Map<String, Object> ship(String orderNo, Map<String, Object> body, Long adminId) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        OrderMainEntity order = requireOrder(orderNo);
        if (!STATUS_PAID_WAIT_SHIP.equals(order.getOrderStatus())) {
            throw new BizException(400, "only paid waiting ship order can be shipped");
        }
        String carrierCode = parseText(request.get("carrierCode"));
        String trackingNo = parseText(request.get("trackingNo"));
        String note = parseText(request.get("remark"));
        if (!StringUtils.hasText(carrierCode) || !StringUtils.hasText(trackingNo)) {
            throw new BizException(400, "carrierCode and trackingNo are required");
        }

        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(STATUS_SHIPPED_WAIT_RECEIVE);
        orderMainMapper.updateById(order);
        insertOrderLog(order.getId(), oldStatus, STATUS_SHIPPED_WAIT_RECEIVE,
                "SHIP_ORDER", OrderLogRemarkUtil.buildShippingRemark(carrierCode, trackingNo, note), adminId);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        data.put("carrierCode", carrierCode);
        data.put("trackingNo", trackingNo);
        return data;
    }

    public Map<String, Object> markCompleted(String orderNo, Map<String, Object> body, Long adminId) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        OrderMainEntity order = requireOrder(orderNo);
        if (!STATUS_SHIPPED_WAIT_RECEIVE.equals(order.getOrderStatus())) {
            throw new BizException(400, "only shipped order can be completed");
        }
        String oldStatus = order.getOrderStatus();
        String remark = parseText(request.get("remark"));

        order.setOrderStatus(STATUS_COMPLETED);
        orderMainMapper.updateById(order);
        insertOrderLog(order.getId(), oldStatus, STATUS_COMPLETED, "ADMIN_COMPLETE", remark, adminId);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        return data;
    }

    private OrderMainEntity requireOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException(400, "orderNo is required");
        }
        OrderMainEntity order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(404, "order not found");
        }
        return order;
    }

    private List<OrderLogEntity> listLogs(Long orderId) {
        return orderLogMapper.selectList(new LambdaQueryWrapper<OrderLogEntity>()
                .eq(OrderLogEntity::getOrderId, orderId)
                .orderByAsc(OrderLogEntity::getId));
    }

    private Map<String, Object> fetchItemPreview(Long orderId) {
        OrderItemEntity item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId)
                .orderByAsc(OrderItemEntity::getId)
                .last("LIMIT 1"));
        Map<String, Object> preview = new HashMap<String, Object>();
        if (item == null) {
            return preview;
        }
        preview.put("title", item.getItemName());
        preview.put("imageUrl", item.getItemImageUrl());
        return preview;
    }

    private void insertOrderLog(Long orderId,
                                String fromStatus,
                                String toStatus,
                                String action,
                                String remark,
                                Long adminId) {
        OrderLogEntity log = new OrderLogEntity();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setAction(action);
        log.setOperatorType("ADMIN");
        log.setOperatorId(adminId);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        orderLogMapper.insert(log);
    }

    private Map<String, Object> extractShipping(List<OrderLogEntity> logs) {
        Map<String, Object> shipping = new HashMap<String, Object>();
        OrderLogEntity shipLog = findLatestShipLog(logs);
        if (shipLog == null) {
            return shipping;
        }
        Map<String, String> kv = OrderLogRemarkUtil.parseKeyValueRemark(shipLog.getRemark());
        shipping.put("carrierCode", kv.get("carrierCode"));
        shipping.put("trackingNo", kv.get("trackingNo"));
        shipping.put("remark", kv.get("note"));
        shipping.put("shippedAt", shipLog.getCreatedAt());
        return shipping;
    }

    private OrderLogEntity findLatestShipLog(List<OrderLogEntity> logs) {
        for (int i = logs.size() - 1; i >= 0; i--) {
            OrderLogEntity log = logs.get(i);
            if ("SHIP_ORDER".equals(log.getAction())) {
                return log;
            }
        }
        return null;
    }

    private List<Map<String, Object>> mapLogs(List<OrderLogEntity> logs) {
        List<Map<String, Object>> mapped = new ArrayList<Map<String, Object>>();
        for (OrderLogEntity log : logs) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("action", log.getAction());
            row.put("fromStatus", log.getFromStatus());
            row.put("toStatus", log.getToStatus());
            row.put("operatorType", log.getOperatorType());
            row.put("operatorId", log.getOperatorId());
            row.put("remark", log.getRemark());
            row.put("createdAt", log.getCreatedAt());
            mapped.add(row);
        }
        return mapped;
    }

    private Map<String, Object> parseAddressSnapshot(String text) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (!StringUtils.hasText(text)) {
            return map;
        }
        String normalized = text.trim();
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        String delimiter = normalized.contains(";") ? ";" : ",";
        String[] pairs = normalized.split(delimiter);
        for (String pair : pairs) {
            String line = pair.trim();
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = line.substring(0, idx).trim();
            String value = line.substring(idx + 1).trim();
            map.put(key, value);
        }
        return map;
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
