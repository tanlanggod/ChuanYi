package com.chuanyi.backend.modules.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.common.util.MockUtil;
import com.chuanyi.backend.modules.address.entity.UserAddressEntity;
import com.chuanyi.backend.modules.address.mapper.UserAddressMapper;
import com.chuanyi.backend.modules.cart.entity.CartItemEntity;
import com.chuanyi.backend.modules.cart.service.CartService;
import com.chuanyi.backend.modules.goods.entity.GoodsSkuEntity;
import com.chuanyi.backend.modules.goods.entity.GoodsSpuEntity;
import com.chuanyi.backend.modules.goods.mapper.GoodsSkuMapper;
import com.chuanyi.backend.modules.goods.mapper.GoodsSpuMapper;
import com.chuanyi.backend.modules.order.entity.OrderItemEntity;
import com.chuanyi.backend.modules.order.entity.OrderLogEntity;
import com.chuanyi.backend.modules.order.entity.OrderMainEntity;
import com.chuanyi.backend.modules.order.mapper.OrderItemMapper;
import com.chuanyi.backend.modules.order.mapper.OrderLogMapper;
import com.chuanyi.backend.modules.order.mapper.OrderMainMapper;
import com.chuanyi.backend.modules.order.util.OrderLogRemarkUtil;
import com.chuanyi.backend.modules.snapshot.entity.DesignSnapshotEntity;
import com.chuanyi.backend.modules.snapshot.mapper.DesignSnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final String STATUS_PENDING_PAY = "PENDING_PAY";
    private static final String STATUS_PAID_WAIT_SHIP = "PAID_WAIT_SHIP";
    private static final String STATUS_SHIPPED_WAIT_RECEIVE = "SHIPPED_WAIT_RECEIVE";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String ACTION_CREATE = "CREATE_ORDER";
    private static final String ACTION_CANCEL = "CANCEL_ORDER";
    private static final String ACTION_SHIP = "SHIP_ORDER";
    private static final String ACTION_CONFIRM_RECEIPT = "CONFIRM_RECEIPT";

    private final OrderMainMapper orderMainMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderLogMapper orderLogMapper;
    private final DesignSnapshotMapper designSnapshotMapper;
    private final UserAddressMapper userAddressMapper;
    private final CartService cartService;
    private final GoodsSkuMapper goodsSkuMapper;
    private final GoodsSpuMapper goodsSpuMapper;

    public OrderService(OrderMainMapper orderMainMapper,
                        OrderItemMapper orderItemMapper,
                        OrderLogMapper orderLogMapper,
                        DesignSnapshotMapper designSnapshotMapper,
                        UserAddressMapper userAddressMapper,
                        CartService cartService,
                        GoodsSkuMapper goodsSkuMapper,
                        GoodsSpuMapper goodsSpuMapper) {
        this.orderMainMapper = orderMainMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderLogMapper = orderLogMapper;
        this.designSnapshotMapper = designSnapshotMapper;
        this.userAddressMapper = userAddressMapper;
        this.cartService = cartService;
        this.goodsSkuMapper = goodsSkuMapper;
        this.goodsSpuMapper = goodsSpuMapper;
    }

    public Map<String, Object> createFromSnapshot(Map<String, Object> body) {
        String snapshotNo = parseText(body.get("snapshotNo"));
        if (!StringUtils.hasText(snapshotNo)) {
            throw new BizException(400, "snapshotNo is required");
        }
        DesignSnapshotEntity snapshot = findSnapshot(snapshotNo);
        Long addressId = parseLong(body.get("addressId"));
        UserAddressEntity address = findAddress(addressId);
        String remark = parseText(body.get("remark"));

        return persistOrder(snapshot, address, remark);
    }

    public Map<String, Object> createFromCart(Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        Long addressId = parseLong(body.get("addressId"));
        UserAddressEntity address = findAddress(addressId);
        String remark = parseText(body.get("remark"));
        List<Long> cartItemIds = parseLongList(body.get("cartItemIds"));
        List<CartItemEntity> cartItems = cartService.listSelectedItems(userId, cartItemIds);
        if (cartItems.isEmpty()) {
            throw new BizException(400, "no selected cart items");
        }
        for (CartItemEntity cartItem : cartItems) {
            if (!"VALID".equals(cartItem.getValidityStatus())) {
                throw new BizException(400, "contains invalid cart items");
            }
        }

        BigDecimal payAmount = BigDecimal.ZERO;
        for (CartItemEntity cartItem : cartItems) {
            BigDecimal amount = safe(cartItem.getUnitPrice()).multiply(new BigDecimal(cartItem.getQty() == null ? 1 : cartItem.getQty()));
            payAmount = payAmount.add(amount);
        }

        OrderMainEntity order = new OrderMainEntity();
        order.setOrderNo(MockUtil.nextNo("O"));
        order.setUserId(userId);
        order.setTotalAmount(payAmount);
        order.setPayAmount(payAmount);
        order.setOrderStatus(STATUS_PENDING_PAY);
        order.setPayStatus("UNPAID");
        order.setPreoccupyStatus("LOCKED");
        order.setAddressSnapshotJson(buildAddressSnapshot(address));
        order.setRemark(remark);
        order.setCreatedAt(LocalDateTime.now());
        orderMainMapper.insert(order);
        insertOrderLog(order.getId(), null, STATUS_PENDING_PAY, ACTION_CREATE, "source=cart");

        List<Long> consumedIds = new ArrayList<Long>();
        for (CartItemEntity cartItem : cartItems) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setItemType(cartItem.getItemType());
            if ("DIY_SNAPSHOT".equals(cartItem.getItemType()) && StringUtils.hasText(cartItem.getSnapshotNo())) {
                DesignSnapshotEntity snapshot = findSnapshot(cartItem.getSnapshotNo());
                item.setSnapshotId(snapshot.getId());
                item.setItemSnapshotJson(snapshot.getSnapshotJson());
            } else {
                item.setSnapshotId(null);
                item.setItemSnapshotJson("{}");
            }
            item.setSkuId(cartItem.getSkuId());
            item.setItemName(cartItem.getTitle());
            item.setItemImageUrl(cartItem.getImageUrl());
            item.setUnitPrice(safe(cartItem.getUnitPrice()));
            item.setQty(cartItem.getQty() == null ? 1 : cartItem.getQty());
            item.setAmount(safe(cartItem.getUnitPrice()).multiply(new BigDecimal(item.getQty())));
            orderItemMapper.insert(item);
            consumedIds.add(cartItem.getId());
        }
        cartService.removeItems(userId, consumedIds);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        data.put("payAmount", order.getPayAmount());
        data.put("preoccupyStatus", order.getPreoccupyStatus());
        return data;
    }

    public Map<String, Object> createDirectGoods(Map<String, Object> body) {
        Long skuId = parseLong(body.get("skuId"));
        if (skuId == null) {
            throw new BizException(400, "skuId is required");
        }
        Integer qty = parseInt(body.get("qty"), 1);
        if (qty < 1) {
            qty = 1;
        }
        Long addressId = parseLong(body.get("addressId"));
        UserAddressEntity address = findAddress(addressId);
        String remark = parseText(body.get("remark"));

        GoodsSkuEntity sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(404, "sku not found");
        }
        if (!"ON_SALE".equalsIgnoreCase(sku.getSalesStatus())) {
            throw new BizException(400, "sku is off sale");
        }
        if (sku.getStockQty() == null || sku.getStockQty() < qty) {
            throw new BizException(400, "stock not enough");
        }
        GoodsSpuEntity spu = goodsSpuMapper.selectById(sku.getSpuId());

        BigDecimal unitPrice = safe(sku.getPrice());
        BigDecimal payAmount = unitPrice.multiply(new BigDecimal(qty));

        OrderMainEntity order = new OrderMainEntity();
        order.setOrderNo(MockUtil.nextNo("O"));
        order.setUserId(UserContext.currentUserId());
        order.setTotalAmount(payAmount);
        order.setPayAmount(payAmount);
        order.setOrderStatus(STATUS_PENDING_PAY);
        order.setPayStatus("UNPAID");
        order.setPreoccupyStatus("LOCKED");
        order.setAddressSnapshotJson(buildAddressSnapshot(address));
        order.setRemark(remark);
        order.setCreatedAt(LocalDateTime.now());
        orderMainMapper.insert(order);
        insertOrderLog(order.getId(), null, STATUS_PENDING_PAY, ACTION_CREATE, "source=direct_goods");

        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(order.getId());
        item.setItemType("NORMAL_GOODS");
        item.setSnapshotId(null);
        item.setSkuId(sku.getId());
        item.setItemName(spu == null ? "好物商品" : spu.getTitle());
        item.setItemImageUrl(StringUtils.hasText(sku.getSkuImageUrl())
                ? sku.getSkuImageUrl()
                : (spu == null ? "" : safe(spu.getCoverImageUrl())));
        item.setUnitPrice(unitPrice);
        item.setQty(qty);
        item.setAmount(payAmount);
        item.setItemSnapshotJson("{}");
        orderItemMapper.insert(item);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        data.put("payAmount", order.getPayAmount());
        data.put("preoccupyStatus", order.getPreoccupyStatus());
        return data;
    }

    public Map<String, Object> list(String status, Integer pageNo, Integer pageSize) {
        Long userId = UserContext.currentUserId();
        LambdaQueryWrapper<OrderMainEntity> wrapper = new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getUserId, userId)
                .orderByDesc(OrderMainEntity::getId);
        if (StringUtils.hasText(status)) {
            wrapper.eq(OrderMainEntity::getOrderStatus, status);
        }

        Page<OrderMainEntity> page = orderMainMapper.selectPage(new Page<OrderMainEntity>(pageNo, pageSize), wrapper);
        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", pageNo);
        pageData.put("pageSize", pageSize);
        pageData.put("total", page.getTotal());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", page.getRecords());
        data.put("page", pageData);
        return data;
    }

    public Map<String, Object> detail(String orderNo) {
        OrderMainEntity order = requireOwnedOrder(orderNo);

        List<OrderItemEntity> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, order.getId())
                .orderByAsc(OrderItemEntity::getId));
        List<OrderLogEntity> logs = listOrderLogs(order.getId());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        data.put("payStatus", order.getPayStatus());
        data.put("totalAmount", order.getTotalAmount());
        data.put("payAmount", order.getPayAmount());
        data.put("remark", order.getRemark());
        data.put("addressSnapshot", parseAddressSnapshot(order.getAddressSnapshotJson()));
        data.put("items", items);
        data.put("shipping", extractShipping(logs));
        data.put("logs", mapOrderLogs(logs));
        data.put("createdAt", order.getCreatedAt());
        data.put("paidAt", order.getPaidAt());
        return data;
    }

    public Map<String, Object> cancel(String orderNo, Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        OrderMainEntity order = requireOwnedOrder(orderNo);
        if (!STATUS_PENDING_PAY.equals(order.getOrderStatus())) {
            throw new BizException(400, "only pending pay order can be cancelled");
        }
        String oldStatus = order.getOrderStatus();
        String remark = parseText(request.get("remark"));

        order.setOrderStatus(STATUS_CANCELLED);
        order.setPreoccupyStatus("RELEASED");
        orderMainMapper.updateById(order);
        insertOrderLog(order.getId(), oldStatus, STATUS_CANCELLED, ACTION_CANCEL, remark);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        return data;
    }

    public Map<String, Object> ship(String orderNo, Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        OrderMainEntity order = requireOwnedOrder(orderNo);
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

        String shippingRemark = OrderLogRemarkUtil.buildShippingRemark(carrierCode, trackingNo, note);
        insertOrderLog(order.getId(), oldStatus, STATUS_SHIPPED_WAIT_RECEIVE, ACTION_SHIP, shippingRemark);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        data.put("carrierCode", carrierCode);
        data.put("trackingNo", trackingNo);
        return data;
    }

    public Map<String, Object> confirmReceipt(String orderNo, Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        OrderMainEntity order = requireOwnedOrder(orderNo);
        if (!STATUS_SHIPPED_WAIT_RECEIVE.equals(order.getOrderStatus())) {
            throw new BizException(400, "only shipped order can be confirmed");
        }
        String oldStatus = order.getOrderStatus();
        String remark = parseText(request.get("remark"));

        order.setOrderStatus(STATUS_COMPLETED);
        orderMainMapper.updateById(order);
        insertOrderLog(order.getId(), oldStatus, STATUS_COMPLETED, ACTION_CONFIRM_RECEIPT, remark);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        return data;
    }

    private Map<String, Object> persistOrder(DesignSnapshotEntity snapshot,
                                             UserAddressEntity address,
                                             String remark) {
        Long userId = UserContext.currentUserId();
        BigDecimal payAmount = snapshot.getPriceSnapshot() == null ? BigDecimal.ZERO : snapshot.getPriceSnapshot();

        OrderMainEntity order = new OrderMainEntity();
        order.setOrderNo(MockUtil.nextNo("O"));
        order.setUserId(userId);
        order.setTotalAmount(payAmount);
        order.setPayAmount(payAmount);
        order.setOrderStatus(STATUS_PENDING_PAY);
        order.setPayStatus("UNPAID");
        order.setPreoccupyStatus("LOCKED");
        order.setAddressSnapshotJson(buildAddressSnapshot(address));
        order.setRemark(remark);
        order.setCreatedAt(LocalDateTime.now());
        orderMainMapper.insert(order);
        insertOrderLog(order.getId(), null, STATUS_PENDING_PAY, ACTION_CREATE, "source=snapshot");

        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(order.getId());
        item.setItemType("DIY_SNAPSHOT");
        item.setSnapshotId(snapshot.getId());
        item.setItemName("定制手串");
        item.setItemImageUrl(snapshot.getPreviewImageUrl());
        item.setUnitPrice(payAmount);
        item.setQty(1);
        item.setAmount(payAmount);
        item.setItemSnapshotJson(snapshot.getSnapshotJson());
        orderItemMapper.insert(item);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("orderNo", order.getOrderNo());
        data.put("orderStatus", order.getOrderStatus());
        data.put("payAmount", order.getPayAmount());
        data.put("preoccupyStatus", order.getPreoccupyStatus());
        return data;
    }

    private OrderMainEntity requireOwnedOrder(String orderNo) {
        Long userId = UserContext.currentUserId();
        OrderMainEntity order = orderMainMapper.selectOne(new LambdaQueryWrapper<OrderMainEntity>()
                .eq(OrderMainEntity::getOrderNo, orderNo)
                .eq(OrderMainEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException(404, "order not found");
        }
        return order;
    }

    private List<OrderLogEntity> listOrderLogs(Long orderId) {
        return orderLogMapper.selectList(new LambdaQueryWrapper<OrderLogEntity>()
                .eq(OrderLogEntity::getOrderId, orderId)
                .orderByAsc(OrderLogEntity::getId));
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
        log.setOperatorType("USER");
        log.setOperatorId(UserContext.currentUserId());
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        orderLogMapper.insert(log);
    }

    private List<Map<String, Object>> mapOrderLogs(List<OrderLogEntity> logs) {
        List<Map<String, Object>> mapped = new ArrayList<Map<String, Object>>();
        for (OrderLogEntity log : logs) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("action", log.getAction());
            row.put("fromStatus", log.getFromStatus());
            row.put("toStatus", log.getToStatus());
            row.put("remark", log.getRemark());
            row.put("createdAt", log.getCreatedAt());
            mapped.add(row);
        }
        return mapped;
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
            if (ACTION_SHIP.equals(log.getAction())) {
                return log;
            }
        }
        return null;
    }

    private DesignSnapshotEntity findSnapshot(String snapshotNo) {
        Long userId = UserContext.currentUserId();
        DesignSnapshotEntity snapshot = designSnapshotMapper.selectOne(new LambdaQueryWrapper<DesignSnapshotEntity>()
                .eq(DesignSnapshotEntity::getSnapshotNo, snapshotNo)
                .eq(DesignSnapshotEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (snapshot == null) {
            throw new BizException(404, "snapshot not found");
        }
        return snapshot;
    }

    private UserAddressEntity findAddress(Long addressId) {
        if (addressId == null) {
            throw new BizException(400, "addressId is required");
        }
        Long userId = UserContext.currentUserId();
        UserAddressEntity address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddressEntity>()
                .eq(UserAddressEntity::getId, addressId)
                .eq(UserAddressEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (address == null) {
            throw new BizException(404, "address not found");
        }
        return address;
    }

    private String buildAddressSnapshot(UserAddressEntity address) {
        return "receiverName=" + safe(address.getReceiverName())
                + ";receiverPhone=" + safe(address.getReceiverPhone())
                + ";province=" + safe(address.getProvince())
                + ";city=" + safe(address.getCity())
                + ";district=" + safe(address.getDistrict())
                + ";detailAddress=" + safe(address.getDetailAddress());
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Long.valueOf(text);
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private List<Long> parseLongList(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<Long>();
        }
        List<?> raw = (List<?>) value;
        List<Long> result = new ArrayList<Long>();
        for (Object item : raw) {
            Long id = parseLong(item);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private Integer parseInt(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return defaultValue;
        }
        return Integer.valueOf(text);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
