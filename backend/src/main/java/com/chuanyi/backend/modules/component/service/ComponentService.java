package com.chuanyi.backend.modules.component.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.modules.component.entity.ComponentCategoryEntity;
import com.chuanyi.backend.modules.component.entity.ComponentSkuEntity;
import com.chuanyi.backend.modules.component.mapper.ComponentCategoryMapper;
import com.chuanyi.backend.modules.component.mapper.ComponentSkuMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ComponentService {

    private final ComponentCategoryMapper componentCategoryMapper;
    private final ComponentSkuMapper componentSkuMapper;

    public ComponentService(ComponentCategoryMapper componentCategoryMapper,
                            ComponentSkuMapper componentSkuMapper) {
        this.componentCategoryMapper = componentCategoryMapper;
        this.componentSkuMapper = componentSkuMapper;
    }

    public Map<String, Object> categories() {
        List<ComponentCategoryEntity> categories = componentCategoryMapper.selectList(
                new LambdaQueryWrapper<ComponentCategoryEntity>()
                        .eq(ComponentCategoryEntity::getStatus, "ENABLED")
                        .orderByAsc(ComponentCategoryEntity::getSortNo, ComponentCategoryEntity::getId)
        );
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("categories", categories);
        return data;
    }

    public Map<String, Object> list(Long categoryId, String keyword, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<ComponentSkuEntity> wrapper = new LambdaQueryWrapper<ComponentSkuEntity>()
                .orderByDesc(ComponentSkuEntity::getId);
        if (categoryId != null) {
            wrapper.eq(ComponentSkuEntity::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ComponentSkuEntity::getName, keyword)
                    .or().like(ComponentSkuEntity::getSpecText, keyword));
        }

        Page<ComponentSkuEntity> page = componentSkuMapper.selectPage(new Page<ComponentSkuEntity>(pageNo, pageSize), wrapper);
        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", pageNo);
        pageData.put("pageSize", pageSize);
        pageData.put("total", page.getTotal());

        List<Map<String, Object>> list = new java.util.ArrayList<Map<String, Object>>();
        for (ComponentSkuEntity item : page.getRecords()) {
            Map<String, Object> mapped = new HashMap<String, Object>();
            mapped.put("skuId", item.getId());
            mapped.put("categoryId", item.getCategoryId());
            mapped.put("name", item.getName());
            mapped.put("specText", item.getSpecText());
            mapped.put("beadDiameterMm", item.getBeadDiameterMm());
            mapped.put("price", item.getPrice());
            mapped.put("stockQty", item.getStockQty());
            mapped.put("salesStatus", item.getSalesStatus());
            mapped.put("imageUrl", item.getImageUrl());
            list.add(mapped);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("page", pageData);
        return data;
    }
}
