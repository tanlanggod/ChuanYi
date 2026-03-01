package com.chuanyi.backend.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.component.entity.ComponentCategoryEntity;
import com.chuanyi.backend.modules.component.entity.ComponentSkuEntity;
import com.chuanyi.backend.modules.component.mapper.ComponentCategoryMapper;
import com.chuanyi.backend.modules.component.mapper.ComponentSkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminComponentService {

    private final ComponentCategoryMapper componentCategoryMapper;
    private final ComponentSkuMapper componentSkuMapper;

    public AdminComponentService(ComponentCategoryMapper componentCategoryMapper,
                                 ComponentSkuMapper componentSkuMapper) {
        this.componentCategoryMapper = componentCategoryMapper;
        this.componentSkuMapper = componentSkuMapper;
    }

    public Map<String, Object> categories(Boolean withCount) {
        List<ComponentCategoryEntity> categories = componentCategoryMapper.selectList(
                new LambdaQueryWrapper<ComponentCategoryEntity>()
                        .orderByAsc(ComponentCategoryEntity::getSortNo, ComponentCategoryEntity::getId)
        );
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (ComponentCategoryEntity category : categories) {
            Map<String, Object> row = mapCategory(category);
            if (Boolean.TRUE.equals(withCount)) {
                Long count = componentSkuMapper.selectCount(new LambdaQueryWrapper<ComponentSkuEntity>()
                        .eq(ComponentSkuEntity::getCategoryId, category.getId()));
                row.put("skuCount", count == null ? 0 : count.intValue());
            }
            list.add(row);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("categories", list);
        return data;
    }

    public Map<String, Object> saveCategory(Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        Long id = parseLong(request.get("id"));
        String name = parseText(request.get("name"));
        if (!StringUtils.hasText(name)) {
            throw new BizException(400, "name is required");
        }

        ComponentCategoryEntity entity;
        if (id == null) {
            entity = new ComponentCategoryEntity();
        } else {
            entity = componentCategoryMapper.selectById(id);
            if (entity == null) {
                throw new BizException(404, "category not found");
            }
        }

        entity.setName(name);
        if (request.containsKey("parentId")) {
            entity.setParentId(parseLong(request.get("parentId")));
        }
        if (request.containsKey("categoryType")) {
            String categoryType = parseText(request.get("categoryType"));
            entity.setCategoryType(StringUtils.hasText(categoryType) ? categoryType.toUpperCase() : null);
        }
        if (request.containsKey("sortNo")) {
            Integer sortNo = parseInt(request.get("sortNo"));
            entity.setSortNo(sortNo == null ? 0 : sortNo);
        } else if (entity.getSortNo() == null) {
            entity.setSortNo(0);
        }
        if (request.containsKey("status")) {
            String status = parseText(request.get("status"));
            entity.setStatus(StringUtils.hasText(status) ? status.toUpperCase() : "ENABLED");
        } else if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("ENABLED");
        }

        if (entity.getId() == null) {
            componentCategoryMapper.insert(entity);
        } else {
            componentCategoryMapper.updateById(entity);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("name", entity.getName());
        data.put("status", entity.getStatus());
        return data;
    }

    public Map<String, Object> updateCategoryStatus(Long id, Map<String, Object> body) {
        ComponentCategoryEntity entity = componentCategoryMapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "category not found");
        }
        String status = parseText(body == null ? null : body.get("status"));
        if (!"ENABLED".equalsIgnoreCase(status) && !"DISABLED".equalsIgnoreCase(status)) {
            throw new BizException(400, "status must be ENABLED or DISABLED");
        }
        entity.setStatus(status.toUpperCase());
        componentCategoryMapper.updateById(entity);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("status", entity.getStatus());
        return data;
    }

    public Boolean deleteCategory(Long id) {
        ComponentCategoryEntity entity = componentCategoryMapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "category not found");
        }

        Long childCount = componentCategoryMapper.selectCount(new LambdaQueryWrapper<ComponentCategoryEntity>()
                .eq(ComponentCategoryEntity::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException(400, "category has child categories");
        }

        Long skuCount = componentSkuMapper.selectCount(new LambdaQueryWrapper<ComponentSkuEntity>()
                .eq(ComponentSkuEntity::getCategoryId, id));
        if (skuCount != null && skuCount > 0) {
            throw new BizException(400, "category has sku records");
        }

        componentCategoryMapper.deleteById(id);
        return Boolean.TRUE;
    }

    public Map<String, Object> skuList(Long categoryId,
                                       String keyword,
                                       String salesStatus,
                                       Integer pageNo,
                                       Integer pageSize) {
        LambdaQueryWrapper<ComponentSkuEntity> wrapper = new LambdaQueryWrapper<ComponentSkuEntity>()
                .orderByDesc(ComponentSkuEntity::getId);
        if (categoryId != null) {
            wrapper.eq(ComponentSkuEntity::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ComponentSkuEntity::getName, keyword)
                    .or().like(ComponentSkuEntity::getSpecText, keyword)
                    .or().like(ComponentSkuEntity::getSkuCode, keyword));
        }
        if (StringUtils.hasText(salesStatus)) {
            wrapper.eq(ComponentSkuEntity::getSalesStatus, salesStatus.toUpperCase());
        }

        Page<ComponentSkuEntity> page = componentSkuMapper.selectPage(new Page<ComponentSkuEntity>(pageNo, pageSize), wrapper);
        Map<Long, ComponentCategoryEntity> categoryMap = loadCategoryMap();

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (ComponentSkuEntity sku : page.getRecords()) {
            list.add(mapSku(sku, categoryMap.get(sku.getCategoryId())));
        }

        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", pageNo);
        pageData.put("pageSize", pageSize);
        pageData.put("total", page.getTotal());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("page", pageData);
        return data;
    }

    public Map<String, Object> createSku(Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        Long categoryId = parseLong(request.get("categoryId"));
        String name = parseText(request.get("name"));
        BigDecimal price = parseDecimal(request.get("price"));
        if (categoryId == null) {
            throw new BizException(400, "categoryId is required");
        }
        if (!StringUtils.hasText(name)) {
            throw new BizException(400, "name is required");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(400, "price must be >= 0");
        }
        ComponentCategoryEntity category = componentCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException(400, "category not found");
        }

        ComponentSkuEntity entity = new ComponentSkuEntity();
        entity.setSkuCode(buildSkuCode(parseText(request.get("skuCode"))));
        entity.setCategoryId(categoryId);
        entity.setName(name);
        entity.setSpecText(parseText(request.get("specText")));
        entity.setBeadDiameterMm(parseDecimal(request.get("beadDiameterMm")));
        entity.setPrice(price);
        Integer stockQty = parseInt(request.get("stockQty"));
        entity.setStockQty(stockQty == null ? 0 : Math.max(stockQty, 0));
        Integer stockWarnQty = parseInt(request.get("stockWarnQty"));
        entity.setStockWarnQty(stockWarnQty == null ? 0 : Math.max(stockWarnQty, 0));
        String salesStatus = parseText(request.get("salesStatus"));
        entity.setSalesStatus(StringUtils.hasText(salesStatus) ? salesStatus.toUpperCase() : "ON_SALE");
        entity.setImageUrl(parseText(request.get("imageUrl")));
        componentSkuMapper.insert(entity);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("skuId", entity.getId());
        data.put("skuCode", entity.getSkuCode());
        return data;
    }

    public Map<String, Object> saveSku(Long skuId, Map<String, Object> body) {
        ComponentSkuEntity entity = componentSkuMapper.selectById(skuId);
        if (entity == null) {
            throw new BizException(404, "sku not found");
        }
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        if (request.containsKey("skuCode")) {
            entity.setSkuCode(buildSkuCode(parseText(request.get("skuCode"))));
        }
        if (request.containsKey("categoryId")) {
            Long categoryId = parseLong(request.get("categoryId"));
            if (categoryId != null) {
                ComponentCategoryEntity category = componentCategoryMapper.selectById(categoryId);
                if (category == null) {
                    throw new BizException(400, "category not found");
                }
                entity.setCategoryId(categoryId);
            }
        }
        if (request.containsKey("name")) {
            String name = parseText(request.get("name"));
            if (!StringUtils.hasText(name)) {
                throw new BizException(400, "name cannot be empty");
            }
            entity.setName(name);
        }
        if (request.containsKey("specText")) {
            entity.setSpecText(parseText(request.get("specText")));
        }
        if (request.containsKey("beadDiameterMm")) {
            entity.setBeadDiameterMm(parseDecimal(request.get("beadDiameterMm")));
        }
        if (request.containsKey("price")) {
            BigDecimal price = parseDecimal(request.get("price"));
            if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(400, "price must be >= 0");
            }
            entity.setPrice(price);
        }
        if (request.containsKey("stockQty")) {
            Integer stockQty = parseInt(request.get("stockQty"));
            entity.setStockQty(stockQty == null ? 0 : Math.max(stockQty, 0));
        }
        if (request.containsKey("stockWarnQty")) {
            Integer stockWarnQty = parseInt(request.get("stockWarnQty"));
            entity.setStockWarnQty(stockWarnQty == null ? 0 : Math.max(stockWarnQty, 0));
        }
        if (request.containsKey("salesStatus")) {
            String salesStatus = parseText(request.get("salesStatus"));
            if (!"ON_SALE".equalsIgnoreCase(salesStatus) && !"OFF_SALE".equalsIgnoreCase(salesStatus)) {
                throw new BizException(400, "salesStatus must be ON_SALE or OFF_SALE");
            }
            entity.setSalesStatus(salesStatus.toUpperCase());
        }
        if (request.containsKey("imageUrl")) {
            entity.setImageUrl(parseText(request.get("imageUrl")));
        }
        componentSkuMapper.updateById(entity);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("skuId", entity.getId());
        data.put("skuCode", entity.getSkuCode());
        return data;
    }

    public Map<String, Object> updateSkuStatus(Long skuId, Map<String, Object> body) {
        ComponentSkuEntity entity = componentSkuMapper.selectById(skuId);
        if (entity == null) {
            throw new BizException(404, "sku not found");
        }
        String salesStatus = parseText(body == null ? null : body.get("salesStatus"));
        if (!"ON_SALE".equalsIgnoreCase(salesStatus) && !"OFF_SALE".equalsIgnoreCase(salesStatus)) {
            throw new BizException(400, "salesStatus must be ON_SALE or OFF_SALE");
        }
        entity.setSalesStatus(salesStatus.toUpperCase());
        componentSkuMapper.updateById(entity);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("skuId", entity.getId());
        data.put("salesStatus", entity.getSalesStatus());
        return data;
    }

    public Map<String, Object> batchUpdateSkuStatus(Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        String salesStatus = parseText(request.get("salesStatus"));
        if (!"ON_SALE".equalsIgnoreCase(salesStatus) && !"OFF_SALE".equalsIgnoreCase(salesStatus)) {
            throw new BizException(400, "salesStatus must be ON_SALE or OFF_SALE");
        }

        List<Long> skuIds = parseLongList(request.get("skuIds"));
        if (skuIds.isEmpty()) {
            throw new BizException(400, "skuIds is required");
        }
        if (skuIds.size() > 500) {
            throw new BizException(400, "skuIds max size is 500");
        }

        String normalizedStatus = salesStatus.toUpperCase();
        int updated = componentSkuMapper.update(null, new LambdaUpdateWrapper<ComponentSkuEntity>()
                .set(ComponentSkuEntity::getSalesStatus, normalizedStatus)
                .in(ComponentSkuEntity::getId, skuIds));

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("updated", updated);
        data.put("salesStatus", normalizedStatus);
        data.put("skuIds", skuIds);
        return data;
    }

    public Boolean deleteSku(Long skuId) {
        componentSkuMapper.deleteById(skuId);
        return Boolean.TRUE;
    }

    private String buildSkuCode(String input) {
        if (StringUtils.hasText(input)) {
            return input;
        }
        return "CSKU" + System.currentTimeMillis();
    }

    private Map<String, Object> mapCategory(ComponentCategoryEntity entity) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("id", entity.getId());
        row.put("parentId", entity.getParentId());
        row.put("categoryType", entity.getCategoryType());
        row.put("name", entity.getName());
        row.put("sortNo", entity.getSortNo());
        row.put("status", entity.getStatus());
        return row;
    }

    private Map<String, Object> mapSku(ComponentSkuEntity sku, ComponentCategoryEntity category) {
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("skuId", sku.getId());
        row.put("skuCode", sku.getSkuCode());
        row.put("categoryId", sku.getCategoryId());
        row.put("categoryName", category == null ? null : category.getName());
        row.put("categoryType", category == null ? null : category.getCategoryType());
        row.put("name", sku.getName());
        row.put("specText", sku.getSpecText());
        row.put("beadDiameterMm", sku.getBeadDiameterMm());
        row.put("price", sku.getPrice());
        row.put("stockQty", sku.getStockQty());
        row.put("stockWarnQty", sku.getStockWarnQty());
        row.put("salesStatus", sku.getSalesStatus());
        row.put("imageUrl", sku.getImageUrl());
        return row;
    }

    private Map<Long, ComponentCategoryEntity> loadCategoryMap() {
        List<ComponentCategoryEntity> categories = componentCategoryMapper.selectList(
                new LambdaQueryWrapper<ComponentCategoryEntity>().orderByAsc(ComponentCategoryEntity::getSortNo, ComponentCategoryEntity::getId)
        );
        Map<Long, ComponentCategoryEntity> map = new HashMap<Long, ComponentCategoryEntity>();
        for (ComponentCategoryEntity category : categories) {
            map.put(category.getId(), category);
        }
        return map;
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

    private List<Long> parseLongList(Object value) {
        List<Long> list = new ArrayList<Long>();
        if (value == null) {
            return list;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                Long parsed = parseLong(item);
                if (parsed != null) {
                    list.add(parsed);
                }
            }
            return list;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return list;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            Long parsed = parseLong(part);
            if (parsed != null) {
                list.add(parsed);
            }
        }
        return list;
    }
}
