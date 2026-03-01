package com.chuanyi.backend.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.goods.entity.GoodsCategoryEntity;
import com.chuanyi.backend.modules.goods.entity.GoodsSkuEntity;
import com.chuanyi.backend.modules.goods.entity.GoodsSpuEntity;
import com.chuanyi.backend.modules.goods.mapper.GoodsCategoryMapper;
import com.chuanyi.backend.modules.goods.mapper.GoodsSkuMapper;
import com.chuanyi.backend.modules.goods.mapper.GoodsSpuMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminGoodsService {

    private final GoodsSpuMapper goodsSpuMapper;
    private final GoodsSkuMapper goodsSkuMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;

    public AdminGoodsService(GoodsSpuMapper goodsSpuMapper,
                             GoodsSkuMapper goodsSkuMapper,
                             GoodsCategoryMapper goodsCategoryMapper) {
        this.goodsSpuMapper = goodsSpuMapper;
        this.goodsSkuMapper = goodsSkuMapper;
        this.goodsCategoryMapper = goodsCategoryMapper;
    }

    public Map<String, Object> list(Long categoryId,
                                    String keyword,
                                    String salesStatus,
                                    Integer pageNo,
                                    Integer pageSize) {
        LambdaQueryWrapper<GoodsSpuEntity> wrapper = new LambdaQueryWrapper<GoodsSpuEntity>();
        if (categoryId != null) {
            wrapper.eq(GoodsSpuEntity::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(GoodsSpuEntity::getTitle, keyword)
                    .or().like(GoodsSpuEntity::getSubtitle, keyword));
        }
        if (StringUtils.hasText(salesStatus)) {
            wrapper.eq(GoodsSpuEntity::getSalesStatus, salesStatus);
        }
        wrapper.orderByAsc(GoodsSpuEntity::getSortNo).orderByDesc(GoodsSpuEntity::getId);

        Page<GoodsSpuEntity> page = goodsSpuMapper.selectPage(new Page<GoodsSpuEntity>(pageNo, pageSize), wrapper);
        Map<Long, String> categoryNames = loadCategoryNames();

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (GoodsSpuEntity spu : page.getRecords()) {
            rows.add(mapSpuRow(spu, categoryNames));
        }

        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", pageNo);
        pageData.put("pageSize", pageSize);
        pageData.put("total", page.getTotal());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", rows);
        data.put("page", pageData);
        return data;
    }

    public Map<String, Object> updateSalesStatus(Long spuId, Map<String, Object> body) {
        GoodsSpuEntity spu = goodsSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(404, "goods not found");
        }
        String salesStatus = parseText(body == null ? null : body.get("salesStatus"));
        if (!"ON_SALE".equalsIgnoreCase(salesStatus) && !"OFF_SALE".equalsIgnoreCase(salesStatus)) {
            throw new BizException(400, "salesStatus must be ON_SALE or OFF_SALE");
        }
        String normalized = salesStatus.toUpperCase();
        spu.setSalesStatus(normalized);
        goodsSpuMapper.updateById(spu);

        List<GoodsSkuEntity> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSkuEntity>()
                .eq(GoodsSkuEntity::getSpuId, spuId));
        for (GoodsSkuEntity sku : skus) {
            sku.setSalesStatus(normalized);
            goodsSkuMapper.updateById(sku);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("spuId", spu.getId());
        data.put("salesStatus", spu.getSalesStatus());
        return data;
    }

    public Map<String, Object> detail(Long spuId) {
        GoodsSpuEntity spu = goodsSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(404, "goods not found");
        }
        Map<Long, String> categoryNames = loadCategoryNames();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("spu", mapSpuRow(spu, categoryNames));
        data.put("skus", listSkus(spuId));
        return data;
    }

    public Map<String, Object> save(Long spuId, Map<String, Object> body) {
        GoodsSpuEntity spu = goodsSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(404, "goods not found");
        }
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;

        String title = parseText(request.get("title"));
        String subtitle = parseText(request.get("subtitle"));
        String coverImageUrl = parseText(request.get("coverImageUrl"));
        Long categoryId = parseLong(request.get("categoryId"));
        Integer sortNo = parseInt(request.get("sortNo"));
        String stockStatus = parseText(request.get("stockStatus"));
        String salesStatus = parseText(request.get("salesStatus"));
        BigDecimal minPrice = parseDecimal(request.get("minPrice"));
        BigDecimal maxPrice = parseDecimal(request.get("maxPrice"));
        String tags = parseText(request.get("tags"));

        if (StringUtils.hasText(title)) {
            spu.setTitle(title);
        }
        if (subtitle != null) {
            spu.setSubtitle(subtitle);
        }
        if (coverImageUrl != null) {
            spu.setCoverImageUrl(coverImageUrl);
        }
        if (categoryId != null) {
            GoodsCategoryEntity category = goodsCategoryMapper.selectById(categoryId);
            if (category == null) {
                throw new BizException(400, "category not found");
            }
            spu.setCategoryId(categoryId);
        }
        if (sortNo != null) {
            spu.setSortNo(sortNo);
        }
        if (StringUtils.hasText(stockStatus)) {
            spu.setStockStatus(stockStatus.toUpperCase());
        }
        if (minPrice != null) {
            spu.setMinPrice(minPrice);
        }
        if (maxPrice != null) {
            spu.setMaxPrice(maxPrice);
        }
        if (tags != null) {
            spu.setTagsJson(toTagsJson(tags));
        }
        if (StringUtils.hasText(salesStatus)) {
            String normalized = salesStatus.toUpperCase();
            if (!"ON_SALE".equals(normalized) && !"OFF_SALE".equals(normalized) && !"SOLD_OUT".equals(normalized)) {
                throw new BizException(400, "invalid salesStatus");
            }
            spu.setSalesStatus(normalized);
            if ("ON_SALE".equals(normalized) || "OFF_SALE".equals(normalized)) {
                syncSkuSalesStatus(spuId, normalized);
            }
        }
        goodsSpuMapper.updateById(spu);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("spuId", spu.getId());
        data.put("title", spu.getTitle());
        data.put("salesStatus", spu.getSalesStatus());
        return data;
    }

    public Map<String, Object> createSku(Long spuId, Map<String, Object> body) {
        GoodsSpuEntity spu = goodsSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(404, "goods not found");
        }
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;

        String specText = parseText(request.get("specText"));
        BigDecimal price = parseDecimal(request.get("price"));
        if (!StringUtils.hasText(specText)) {
            throw new BizException(400, "specText is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(400, "price is required");
        }

        GoodsSkuEntity sku = new GoodsSkuEntity();
        sku.setSpuId(spuId);
        sku.setSpecText(specText);
        sku.setSkuImageUrl(parseText(request.get("skuImageUrl")));
        sku.setPrice(price);
        sku.setOriginPrice(parseDecimal(request.get("originPrice")));
        Integer stockQty = parseInt(request.get("stockQty"));
        sku.setStockQty(stockQty == null ? 0 : Math.max(stockQty, 0));
        String salesStatus = parseText(request.get("salesStatus"));
        sku.setSalesStatus(StringUtils.hasText(salesStatus) ? salesStatus.toUpperCase() : "ON_SALE");

        goodsSkuMapper.insert(sku);
        refreshSpuPricingAndStock(spuId);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("skuId", sku.getId());
        data.put("spuId", sku.getSpuId());
        return data;
    }

    public Map<String, Object> saveSku(Long skuId, Map<String, Object> body) {
        GoodsSkuEntity sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(404, "sku not found");
        }
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;

        String specText = parseText(request.get("specText"));
        if (StringUtils.hasText(specText)) {
            sku.setSpecText(specText);
        }
        if (request.containsKey("skuImageUrl")) {
            sku.setSkuImageUrl(parseText(request.get("skuImageUrl")));
        }
        BigDecimal price = parseDecimal(request.get("price"));
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(400, "price must be >= 0");
            }
            sku.setPrice(price);
        }
        BigDecimal originPrice = parseDecimal(request.get("originPrice"));
        if (originPrice != null) {
            sku.setOriginPrice(originPrice);
        }
        Integer stockQty = parseInt(request.get("stockQty"));
        if (stockQty != null) {
            sku.setStockQty(Math.max(stockQty, 0));
        }
        String salesStatus = parseText(request.get("salesStatus"));
        if (StringUtils.hasText(salesStatus)) {
            String normalized = salesStatus.toUpperCase();
            if (!"ON_SALE".equals(normalized) && !"OFF_SALE".equals(normalized)) {
                throw new BizException(400, "invalid salesStatus");
            }
            sku.setSalesStatus(normalized);
        }
        goodsSkuMapper.updateById(sku);
        refreshSpuPricingAndStock(sku.getSpuId());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("skuId", sku.getId());
        data.put("spuId", sku.getSpuId());
        return data;
    }

    public Map<String, Object> updateSkuStatus(Long skuId, Map<String, Object> body) {
        GoodsSkuEntity sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(404, "sku not found");
        }
        String salesStatus = parseText(body == null ? null : body.get("salesStatus"));
        if (!"ON_SALE".equalsIgnoreCase(salesStatus) && !"OFF_SALE".equalsIgnoreCase(salesStatus)) {
            throw new BizException(400, "salesStatus must be ON_SALE or OFF_SALE");
        }
        sku.setSalesStatus(salesStatus.toUpperCase());
        goodsSkuMapper.updateById(sku);
        refreshSpuPricingAndStock(sku.getSpuId());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("skuId", sku.getId());
        data.put("salesStatus", sku.getSalesStatus());
        return data;
    }

    public Boolean deleteSku(Long skuId) {
        GoodsSkuEntity sku = goodsSkuMapper.selectById(skuId);
        if (sku == null) {
            return Boolean.TRUE;
        }
        Long spuId = sku.getSpuId();
        goodsSkuMapper.deleteById(skuId);
        refreshSpuPricingAndStock(spuId);
        return Boolean.TRUE;
    }

    private Map<Long, String> loadCategoryNames() {
        List<GoodsCategoryEntity> categories = goodsCategoryMapper.selectList(new LambdaQueryWrapper<GoodsCategoryEntity>()
                .orderByAsc(GoodsCategoryEntity::getSortNo, GoodsCategoryEntity::getId));
        Map<Long, String> map = new HashMap<Long, String>();
        for (GoodsCategoryEntity category : categories) {
            map.put(category.getId(), category.getName());
        }
        return map;
    }

    private List<String> parseTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return new ArrayList<String>();
        }
        String normalized = tagsJson.trim()
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "");
        if (!StringUtils.hasText(normalized)) {
            return new ArrayList<String>();
        }
        String[] parts = normalized.split(",");
        List<String> tags = new ArrayList<String>();
        for (String part : parts) {
            String tag = part.trim();
            if (StringUtils.hasText(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private List<Map<String, Object>> listSkus(Long spuId) {
        List<GoodsSkuEntity> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSkuEntity>()
                .eq(GoodsSkuEntity::getSpuId, spuId)
                .orderByAsc(GoodsSkuEntity::getId));
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (GoodsSkuEntity sku : skus) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("skuId", sku.getId());
            row.put("spuId", sku.getSpuId());
            row.put("specText", sku.getSpecText());
            row.put("skuImageUrl", sku.getSkuImageUrl());
            row.put("price", sku.getPrice());
            row.put("originPrice", sku.getOriginPrice());
            row.put("stockQty", sku.getStockQty());
            row.put("salesStatus", sku.getSalesStatus());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> mapSpuRow(GoodsSpuEntity spu, Map<Long, String> categoryNames) {
        List<GoodsSkuEntity> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSkuEntity>()
                .eq(GoodsSkuEntity::getSpuId, spu.getId())
                .orderByAsc(GoodsSkuEntity::getId));
        int skuCount = skus.size();
        int onSaleSkuCount = 0;
        int totalStock = 0;
        for (GoodsSkuEntity sku : skus) {
            if ("ON_SALE".equalsIgnoreCase(sku.getSalesStatus())) {
                onSaleSkuCount++;
            }
            if (sku.getStockQty() != null) {
                totalStock += sku.getStockQty();
            }
        }

        Map<String, Object> row = new HashMap<String, Object>();
        row.put("spuId", spu.getId());
        row.put("categoryId", spu.getCategoryId());
        row.put("categoryName", categoryNames.get(spu.getCategoryId()));
        row.put("title", spu.getTitle());
        row.put("subtitle", spu.getSubtitle());
        row.put("coverImageUrl", spu.getCoverImageUrl());
        row.put("minPrice", spu.getMinPrice());
        row.put("maxPrice", spu.getMaxPrice());
        row.put("salesStatus", spu.getSalesStatus());
        row.put("stockStatus", spu.getStockStatus());
        row.put("sortNo", spu.getSortNo());
        row.put("tags", parseTags(spu.getTagsJson()));
        row.put("skuCount", skuCount);
        row.put("onSaleSkuCount", onSaleSkuCount);
        row.put("totalStock", totalStock);
        return row;
    }

    private void syncSkuSalesStatus(Long spuId, String salesStatus) {
        List<GoodsSkuEntity> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSkuEntity>()
                .eq(GoodsSkuEntity::getSpuId, spuId));
        for (GoodsSkuEntity sku : skus) {
            sku.setSalesStatus(salesStatus);
            goodsSkuMapper.updateById(sku);
        }
    }

    private void refreshSpuPricingAndStock(Long spuId) {
        if (spuId == null) {
            return;
        }
        GoodsSpuEntity spu = goodsSpuMapper.selectById(spuId);
        if (spu == null) {
            return;
        }
        List<GoodsSkuEntity> skus = goodsSkuMapper.selectList(new LambdaQueryWrapper<GoodsSkuEntity>()
                .eq(GoodsSkuEntity::getSpuId, spuId));
        if (skus.isEmpty()) {
            spu.setMinPrice(BigDecimal.ZERO);
            spu.setMaxPrice(BigDecimal.ZERO);
            spu.setStockStatus("OUT_OF_STOCK");
            spu.setSalesStatus("SOLD_OUT");
            goodsSpuMapper.updateById(spu);
            return;
        }

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        int totalStock = 0;
        int onSaleCount = 0;
        for (GoodsSkuEntity sku : skus) {
            BigDecimal price = sku.getPrice() == null ? BigDecimal.ZERO : sku.getPrice();
            if (minPrice == null || price.compareTo(minPrice) < 0) {
                minPrice = price;
            }
            if (maxPrice == null || price.compareTo(maxPrice) > 0) {
                maxPrice = price;
            }
            totalStock += sku.getStockQty() == null ? 0 : Math.max(sku.getStockQty(), 0);
            if ("ON_SALE".equalsIgnoreCase(sku.getSalesStatus())) {
                onSaleCount++;
            }
        }

        spu.setMinPrice(minPrice == null ? BigDecimal.ZERO : minPrice);
        spu.setMaxPrice(maxPrice == null ? BigDecimal.ZERO : maxPrice);
        if (totalStock <= 0) {
            spu.setStockStatus("OUT_OF_STOCK");
            spu.setSalesStatus("SOLD_OUT");
        } else if (totalStock <= 20) {
            spu.setStockStatus("LOW_STOCK");
            if ("SOLD_OUT".equalsIgnoreCase(spu.getSalesStatus()) && onSaleCount > 0) {
                spu.setSalesStatus("ON_SALE");
            }
        } else {
            spu.setStockStatus("HAS_STOCK");
            if ("SOLD_OUT".equalsIgnoreCase(spu.getSalesStatus()) && onSaleCount > 0) {
                spu.setSalesStatus("ON_SALE");
            }
        }
        if (onSaleCount == 0 && !"SOLD_OUT".equalsIgnoreCase(spu.getSalesStatus())) {
            spu.setSalesStatus("OFF_SALE");
        }
        goodsSpuMapper.updateById(spu);
    }

    private String toTagsJson(String text) {
        if (!StringUtils.hasText(text)) {
            return "[]";
        }
        String[] parts = text.split(",");
        List<String> normalized = new ArrayList<String>();
        for (String part : parts) {
            String tag = part.trim();
            if (StringUtils.hasText(tag)) {
                normalized.add(tag);
            }
        }
        if (normalized.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < normalized.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(normalized.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
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

    private Integer parseInt(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Integer.valueOf(text);
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return new BigDecimal(text);
    }
}
