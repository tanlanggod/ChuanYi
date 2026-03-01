package com.chuanyi.backend.modules.goods.service;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoodsService {

    private final GoodsCategoryMapper goodsCategoryMapper;
    private final GoodsSpuMapper goodsSpuMapper;
    private final GoodsSkuMapper goodsSkuMapper;

    public GoodsService(GoodsCategoryMapper goodsCategoryMapper,
                        GoodsSpuMapper goodsSpuMapper,
                        GoodsSkuMapper goodsSkuMapper) {
        this.goodsCategoryMapper = goodsCategoryMapper;
        this.goodsSpuMapper = goodsSpuMapper;
        this.goodsSkuMapper = goodsSkuMapper;
    }

    public Map<String, Object> categories(Boolean withCount) {
        List<GoodsCategoryEntity> categories = goodsCategoryMapper.selectList(
                new LambdaQueryWrapper<GoodsCategoryEntity>()
                        .eq(GoodsCategoryEntity::getStatus, "ENABLED")
                        .orderByAsc(GoodsCategoryEntity::getSortNo, GoodsCategoryEntity::getId)
        );
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (GoodsCategoryEntity category : categories) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", category.getId());
            item.put("parentId", category.getParentId());
            item.put("name", category.getName());
            item.put("iconUrl", category.getIconUrl());
            item.put("sortNo", category.getSortNo());
            item.put("status", category.getStatus());
            if (Boolean.TRUE.equals(withCount)) {
                Long count = goodsSpuMapper.selectCount(new LambdaQueryWrapper<GoodsSpuEntity>()
                        .eq(GoodsSpuEntity::getCategoryId, category.getId()));
                item.put("goodsCount", count == null ? 0 : count.intValue());
            } else {
                item.put("goodsCount", null);
            }
            result.add(item);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("categories", result);
        return data;
    }

    public Map<String, Object> list(Long categoryId, String keyword, String sortType, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<GoodsSpuEntity> wrapper = new LambdaQueryWrapper<GoodsSpuEntity>();
        if (categoryId != null) {
            wrapper.eq(GoodsSpuEntity::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(GoodsSpuEntity::getTitle, keyword)
                    .or().like(GoodsSpuEntity::getSubtitle, keyword));
        }
        applySort(wrapper, sortType);

        Page<GoodsSpuEntity> page = goodsSpuMapper.selectPage(new Page<GoodsSpuEntity>(pageNo, pageSize), wrapper);

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (GoodsSpuEntity spu : page.getRecords()) {
            list.add(mapSpu(spu));
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

    public Map<String, Object> detail(Long spuId) {
        GoodsSpuEntity spu = goodsSpuMapper.selectById(spuId);
        if (spu == null) {
            throw new BizException(404, "goods not found");
        }
        List<GoodsSkuEntity> skuEntities = goodsSkuMapper.selectList(
                new LambdaQueryWrapper<GoodsSkuEntity>()
                        .eq(GoodsSkuEntity::getSpuId, spuId)
                        .orderByAsc(GoodsSkuEntity::getId)
        );
        List<Map<String, Object>> skus = new ArrayList<Map<String, Object>>();
        for (GoodsSkuEntity sku : skuEntities) {
            Map<String, Object> mapped = new HashMap<String, Object>();
            mapped.put("skuId", sku.getId());
            mapped.put("spuId", sku.getSpuId());
            mapped.put("specText", sku.getSpecText());
            mapped.put("skuImageUrl", sku.getSkuImageUrl());
            mapped.put("price", sku.getPrice());
            mapped.put("originPrice", sku.getOriginPrice());
            mapped.put("stockQty", sku.getStockQty());
            mapped.put("salesStatus", sku.getSalesStatus());
            skus.add(mapped);
        }
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("spu", mapSpu(spu));
        data.put("skus", skus);
        data.put("imageUrls", parseTags(spu.getImageUrlsJson()));
        data.put("detailHtml", buildDetailHtml(spu));
        return data;
    }

    private void applySort(LambdaQueryWrapper<GoodsSpuEntity> wrapper, String sortType) {
        if ("PRICE_ASC".equalsIgnoreCase(sortType)) {
            wrapper.orderByAsc(GoodsSpuEntity::getMinPrice, GoodsSpuEntity::getId);
            return;
        }
        if ("PRICE_DESC".equalsIgnoreCase(sortType)) {
            wrapper.orderByDesc(GoodsSpuEntity::getMinPrice, GoodsSpuEntity::getId);
            return;
        }
        if ("NEWEST".equalsIgnoreCase(sortType)) {
            wrapper.orderByDesc(GoodsSpuEntity::getId);
            return;
        }
        wrapper.orderByAsc(GoodsSpuEntity::getSortNo).orderByDesc(GoodsSpuEntity::getId);
    }

    private Map<String, Object> mapSpu(GoodsSpuEntity spu) {
        Map<String, Object> mapped = new HashMap<String, Object>();
        mapped.put("spuId", spu.getId());
        mapped.put("categoryId", spu.getCategoryId());
        mapped.put("title", spu.getTitle());
        mapped.put("subtitle", spu.getSubtitle());
        mapped.put("coverImageUrl", spu.getCoverImageUrl());
        mapped.put("imageUrls", parseTags(spu.getImageUrlsJson()));
        mapped.put("minPrice", spu.getMinPrice());
        mapped.put("maxPrice", spu.getMaxPrice());
        mapped.put("salesStatus", spu.getSalesStatus());
        mapped.put("stockStatus", spu.getStockStatus());
        mapped.put("tags", parseTags(spu.getTagsJson()));
        mapped.put("sortNo", spu.getSortNo());
        return mapped;
    }

    private String buildDetailHtml(GoodsSpuEntity spu) {
        return "<p>商品名称：" + safe(spu.getTitle()) + "</p>"
                + "<p>商品副标题：" + safe(spu.getSubtitle()) + "</p>"
                + "<p>销售状态：" + safe(spu.getSalesStatus()) + "</p>"
                + "<p>库存状态：" + safe(spu.getStockStatus()) + "</p>";
    }

    private String safe(String text) {
        return text == null ? "" : text;
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
}
