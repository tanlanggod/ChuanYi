package com.chuanyi.backend.modules.cart.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.cart.entity.CartItemEntity;
import com.chuanyi.backend.modules.cart.mapper.CartItemMapper;
import com.chuanyi.backend.modules.goods.entity.GoodsSkuEntity;
import com.chuanyi.backend.modules.goods.entity.GoodsSpuEntity;
import com.chuanyi.backend.modules.goods.mapper.GoodsSkuMapper;
import com.chuanyi.backend.modules.goods.mapper.GoodsSpuMapper;
import com.chuanyi.backend.modules.snapshot.entity.DesignSnapshotEntity;
import com.chuanyi.backend.modules.snapshot.mapper.DesignSnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final GoodsSkuMapper goodsSkuMapper;
    private final GoodsSpuMapper goodsSpuMapper;
    private final DesignSnapshotMapper designSnapshotMapper;

    public CartService(CartItemMapper cartItemMapper,
                       GoodsSkuMapper goodsSkuMapper,
                       GoodsSpuMapper goodsSpuMapper,
                       DesignSnapshotMapper designSnapshotMapper) {
        this.cartItemMapper = cartItemMapper;
        this.goodsSkuMapper = goodsSkuMapper;
        this.goodsSpuMapper = goodsSpuMapper;
        this.designSnapshotMapper = designSnapshotMapper;
    }

    public Map<String, Object> detail() {
        Long userId = UserContext.currentUserId();
        List<CartItemEntity> entities = listUserItems(userId);
        refreshValidity(entities);
        return buildCartData(entities);
    }

    public Map<String, Object> addItem(Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        String itemType = parseText(body.get("itemType"));
        int qty = parseInt(body.get("qty"), 1);
        if (!StringUtils.hasText(itemType)) {
            throw new BizException(400, "itemType is required");
        }
        if (qty < 1) {
            qty = 1;
        }

        CartItemEntity entity;
        if ("NORMAL_GOODS".equals(itemType)) {
            Long skuId = parseLong(body.get("skuId"));
            if (skuId == null) {
                throw new BizException(400, "skuId is required");
            }
            GoodsSkuEntity sku = goodsSkuMapper.selectById(skuId);
            if (sku == null) {
                throw new BizException(404, "sku not found");
            }
            GoodsSpuEntity spu = goodsSpuMapper.selectById(sku.getSpuId());

            entity = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItemEntity>()
                    .eq(CartItemEntity::getUserId, userId)
                    .eq(CartItemEntity::getItemType, "NORMAL_GOODS")
                    .eq(CartItemEntity::getSkuId, skuId)
                    .last("LIMIT 1"));

            if (entity == null) {
                entity = new CartItemEntity();
                entity.setUserId(userId);
                entity.setItemType("NORMAL_GOODS");
                entity.setBizNo(String.valueOf(skuId));
                entity.setSpuId(sku.getSpuId());
                entity.setSkuId(skuId);
                entity.setSnapshotNo(null);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setQty(qty);
            } else {
                entity.setQty(entity.getQty() + qty);
            }
            entity.setTitle(spu == null ? "好物商品" : spu.getTitle());
            entity.setImageUrl(spu == null ? "" : spu.getCoverImageUrl());
            entity.setUnitPrice(sku.getPrice());
            entity.setSelected(1);
            entity.setValidityStatus(buildGoodsValidity(sku));
            entity.setInvalidReason(buildGoodsInvalidReason(sku));
            entity.setUpdatedAt(LocalDateTime.now());
        } else if ("DIY_SNAPSHOT".equals(itemType)) {
            String snapshotNo = parseText(body.get("snapshotNo"));
            if (!StringUtils.hasText(snapshotNo)) {
                throw new BizException(400, "snapshotNo is required");
            }
            DesignSnapshotEntity snapshot = designSnapshotMapper.selectOne(new LambdaQueryWrapper<DesignSnapshotEntity>()
                    .eq(DesignSnapshotEntity::getSnapshotNo, snapshotNo)
                    .eq(DesignSnapshotEntity::getUserId, userId)
                    .last("LIMIT 1"));
            if (snapshot == null) {
                throw new BizException(404, "snapshot not found");
            }
            entity = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItemEntity>()
                    .eq(CartItemEntity::getUserId, userId)
                    .eq(CartItemEntity::getItemType, "DIY_SNAPSHOT")
                    .eq(CartItemEntity::getSnapshotNo, snapshotNo)
                    .last("LIMIT 1"));
            if (entity == null) {
                entity = new CartItemEntity();
                entity.setUserId(userId);
                entity.setItemType("DIY_SNAPSHOT");
                entity.setBizNo(snapshotNo);
                entity.setSnapshotNo(snapshotNo);
                entity.setQty(1);
                entity.setCreatedAt(LocalDateTime.now());
            }
            entity.setTitle("定制手串");
            entity.setImageUrl(snapshot.getPreviewImageUrl());
            entity.setUnitPrice(snapshot.getPriceSnapshot() == null ? BigDecimal.ZERO : snapshot.getPriceSnapshot());
            entity.setSelected(1);
            entity.setValidityStatus("VALID");
            entity.setInvalidReason(null);
            entity.setUpdatedAt(LocalDateTime.now());
        } else {
            throw new BizException(400, "unsupported itemType");
        }

        if (entity.getId() == null) {
            cartItemMapper.insert(entity);
        } else {
            cartItemMapper.updateById(entity);
        }
        return buildSummaryData(listUserItems(userId));
    }

    public Map<String, Object> updateQty(Long cartItemId, Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        Integer qty = parseInt(body.get("qty"), 1);
        if (qty < 1) {
            qty = 1;
        }
        CartItemEntity entity = requireOwnedCartItem(userId, cartItemId);
        entity.setQty(qty);
        entity.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(entity);
        return buildSummaryData(listUserItems(userId));
    }

    public Map<String, Object> delete(Long cartItemId) {
        Long userId = UserContext.currentUserId();
        cartItemMapper.delete(new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getId, cartItemId)
                .eq(CartItemEntity::getUserId, userId));
        return buildSummaryData(listUserItems(userId));
    }

    public Map<String, Object> select(Long cartItemId, Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        boolean selected = Boolean.parseBoolean(String.valueOf(body.get("selected")));
        CartItemEntity entity = requireOwnedCartItem(userId, cartItemId);
        entity.setSelected(selected ? 1 : 0);
        entity.setUpdatedAt(LocalDateTime.now());
        cartItemMapper.updateById(entity);
        return buildSummaryData(listUserItems(userId));
    }

    public Map<String, Object> selectAll(Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        boolean selected = Boolean.parseBoolean(String.valueOf(body.get("selected")));
        cartItemMapper.update(null, new LambdaUpdateWrapper<CartItemEntity>()
                .eq(CartItemEntity::getUserId, userId)
                .set(CartItemEntity::getSelected, selected ? 1 : 0)
                .set(CartItemEntity::getUpdatedAt, LocalDateTime.now()));
        return buildSummaryData(listUserItems(userId));
    }

    public Map<String, Object> settlementPreview(Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        List<Long> cartItemIds = parseLongList(body == null ? null : body.get("cartItemIds"));
        List<CartItemEntity> selectedItems;
        if (cartItemIds.isEmpty()) {
            selectedItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItemEntity>()
                    .eq(CartItemEntity::getUserId, userId)
                    .eq(CartItemEntity::getSelected, 1)
                    .orderByAsc(CartItemEntity::getId));
        } else {
            selectedItems = cartItemMapper.selectList(new LambdaQueryWrapper<CartItemEntity>()
                    .eq(CartItemEntity::getUserId, userId)
                    .in(CartItemEntity::getId, cartItemIds)
                    .orderByAsc(CartItemEntity::getId));
        }

        refreshValidity(selectedItems);

        BigDecimal subtotal = BigDecimal.ZERO;
        boolean hasInvalid = false;
        for (CartItemEntity item : selectedItems) {
            BigDecimal amount = safe(item.getUnitPrice()).multiply(new BigDecimal(item.getQty() == null ? 1 : item.getQty()));
            subtotal = subtotal.add(amount);
            if (!"VALID".equals(item.getValidityStatus())) {
                hasInvalid = true;
            }
        }

        List<String> blockingReasons = new ArrayList<String>();
        if (selectedItems.isEmpty()) {
            blockingReasons.add("未选择结算商品");
        }
        if (hasInvalid) {
            blockingReasons.add("包含失效商品");
        }

        Map<String, Object> line1 = new HashMap<String, Object>();
        line1.put("title", "subtotal");
        line1.put("amount", subtotal);
        Map<String, Object> line2 = new HashMap<String, Object>();
        line2.put("title", "discount");
        line2.put("amount", BigDecimal.ZERO);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("summary", buildSummary(selectedItems));
        data.put("priceLines", java.util.Arrays.asList(line1, line2));
        data.put("canSubmit", blockingReasons.isEmpty());
        data.put("blockingReasons", blockingReasons);
        return data;
    }

    public List<CartItemEntity> listSelectedItems(Long userId, List<Long> cartItemIds) {
        LambdaQueryWrapper<CartItemEntity> wrapper = new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getUserId, userId)
                .eq(CartItemEntity::getSelected, 1);
        if (cartItemIds != null && !cartItemIds.isEmpty()) {
            wrapper.in(CartItemEntity::getId, cartItemIds);
        }
        List<CartItemEntity> items = cartItemMapper.selectList(wrapper.orderByAsc(CartItemEntity::getId));
        refreshValidity(items);
        return items;
    }

    public void removeItems(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        cartItemMapper.delete(new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getUserId, userId)
                .in(CartItemEntity::getId, ids));
    }

    private CartItemEntity requireOwnedCartItem(Long userId, Long cartItemId) {
        CartItemEntity entity = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getId, cartItemId)
                .eq(CartItemEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BizException(404, "cart item not found");
        }
        return entity;
    }

    private List<CartItemEntity> listUserItems(Long userId) {
        List<CartItemEntity> entities = cartItemMapper.selectList(new LambdaQueryWrapper<CartItemEntity>()
                .eq(CartItemEntity::getUserId, userId)
                .orderByDesc(CartItemEntity::getId));
        refreshValidity(entities);
        return entities;
    }

    private void refreshValidity(List<CartItemEntity> entities) {
        for (CartItemEntity item : entities) {
            if ("NORMAL_GOODS".equals(item.getItemType())) {
                GoodsSkuEntity sku = item.getSkuId() == null ? null : goodsSkuMapper.selectById(item.getSkuId());
                String status = buildGoodsValidity(sku);
                String reason = buildGoodsInvalidReason(sku);
                if (!status.equals(item.getValidityStatus())
                        || !equalsText(reason, item.getInvalidReason())) {
                    item.setValidityStatus(status);
                    item.setInvalidReason(reason);
                    item.setUpdatedAt(LocalDateTime.now());
                    cartItemMapper.updateById(item);
                }
                continue;
            }
            if ("DIY_SNAPSHOT".equals(item.getItemType())) {
                DesignSnapshotEntity snapshot = designSnapshotMapper.selectOne(new LambdaQueryWrapper<DesignSnapshotEntity>()
                        .eq(DesignSnapshotEntity::getSnapshotNo, item.getSnapshotNo())
                        .eq(DesignSnapshotEntity::getUserId, item.getUserId())
                        .last("LIMIT 1"));
                String status = snapshot == null ? "INVALID_NOT_FOUND" : "VALID";
                String reason = snapshot == null ? "定制快照不存在" : null;
                if (!status.equals(item.getValidityStatus())
                        || !equalsText(reason, item.getInvalidReason())) {
                    item.setValidityStatus(status);
                    item.setInvalidReason(reason);
                    item.setUpdatedAt(LocalDateTime.now());
                    cartItemMapper.updateById(item);
                }
            }
        }
    }

    private String buildGoodsValidity(GoodsSkuEntity sku) {
        if (sku == null) {
            return "INVALID_NOT_FOUND";
        }
        if (!"ON_SALE".equalsIgnoreCase(sku.getSalesStatus())) {
            return "INVALID_OFF_SALE";
        }
        if (sku.getStockQty() == null || sku.getStockQty() <= 0) {
            return "INVALID_OUT_OF_STOCK";
        }
        return "VALID";
    }

    private String buildGoodsInvalidReason(GoodsSkuEntity sku) {
        String status = buildGoodsValidity(sku);
        if ("INVALID_NOT_FOUND".equals(status)) {
            return "商品不存在";
        }
        if ("INVALID_OFF_SALE".equals(status)) {
            return "商品已下架";
        }
        if ("INVALID_OUT_OF_STOCK".equals(status)) {
            return "库存不足";
        }
        return null;
    }

    private boolean equalsText(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    private Map<String, Object> buildCartData(List<CartItemEntity> items) {
        List<Map<String, Object>> mapped = new ArrayList<Map<String, Object>>();
        int validCount = 0;
        int invalidCount = 0;
        for (CartItemEntity item : items) {
            if ("VALID".equals(item.getValidityStatus())) {
                validCount++;
            } else {
                invalidCount++;
            }
            mapped.add(mapItem(item));
        }

        String pageState;
        if (mapped.isEmpty()) {
            pageState = "EMPTY";
        } else if (invalidCount == 0) {
            pageState = "HAS_VALID";
        } else if (validCount == 0) {
            pageState = "HAS_INVALID";
        } else {
            pageState = "MIXED";
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("pageState", pageState);
        data.put("items", mapped);
        data.put("summary", buildSummary(items));
        return data;
    }

    private Map<String, Object> buildSummaryData(List<CartItemEntity> items) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("summary", buildSummary(items));
        return data;
    }

    private Map<String, Object> mapItem(CartItemEntity item) {
        Map<String, Object> mapped = new HashMap<String, Object>();
        mapped.put("cartItemId", item.getId());
        mapped.put("itemType", item.getItemType());
        mapped.put("bizNo", item.getBizNo());
        mapped.put("spuId", item.getSpuId());
        mapped.put("skuId", item.getSkuId());
        mapped.put("snapshotNo", item.getSnapshotNo());
        mapped.put("title", item.getTitle());
        mapped.put("imageUrl", item.getImageUrl());
        mapped.put("unitPrice", item.getUnitPrice());
        mapped.put("qty", item.getQty());
        mapped.put("selected", Integer.valueOf(1).equals(item.getSelected()));
        mapped.put("validityStatus", item.getValidityStatus());
        mapped.put("invalidReason", item.getInvalidReason());
        mapped.put("amount", safe(item.getUnitPrice()).multiply(new BigDecimal(item.getQty() == null ? 1 : item.getQty())));
        return mapped;
    }

    private Map<String, Object> buildSummary(List<CartItemEntity> items) {
        int itemCount = items.size();
        int selectedCount = 0;
        int invalidCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal selectedAmount = BigDecimal.ZERO;
        for (CartItemEntity item : items) {
            BigDecimal amount = safe(item.getUnitPrice()).multiply(new BigDecimal(item.getQty() == null ? 1 : item.getQty()));
            totalAmount = totalAmount.add(amount);
            if (Integer.valueOf(1).equals(item.getSelected())) {
                selectedCount++;
                selectedAmount = selectedAmount.add(amount);
            }
            if (!"VALID".equals(item.getValidityStatus())) {
                invalidCount++;
            }
        }

        String checkoutStatus = "READY";
        String checkoutBlockReason = null;
        if (itemCount == 0) {
            checkoutStatus = "BLOCKED_EMPTY";
            checkoutBlockReason = "购物车为空";
        } else if (selectedCount == 0) {
            checkoutStatus = "BLOCKED_NO_SELECTION";
            checkoutBlockReason = "未选择商品";
        } else if (invalidCount > 0) {
            checkoutStatus = "BLOCKED_INVALID_ITEMS";
            checkoutBlockReason = "存在失效商品";
        }

        Map<String, Object> summary = new HashMap<String, Object>();
        summary.put("itemCount", itemCount);
        summary.put("selectedCount", selectedCount);
        summary.put("invalidCount", invalidCount);
        summary.put("totalAmount", totalAmount);
        summary.put("selectedAmount", selectedAmount);
        summary.put("discountAmount", BigDecimal.ZERO);
        summary.put("payAmount", selectedAmount);
        summary.put("checkoutStatus", checkoutStatus);
        summary.put("checkoutBlockReason", checkoutBlockReason);
        return summary;
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

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private List<Long> parseLongList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        if (raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        for (Object item : raw) {
            Long id = parseLong(item);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
