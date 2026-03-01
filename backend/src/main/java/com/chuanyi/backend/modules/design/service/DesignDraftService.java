package com.chuanyi.backend.modules.design.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.util.MockUtil;
import com.chuanyi.backend.modules.design.entity.DesignDraftEntity;
import com.chuanyi.backend.modules.design.mapper.DesignDraftMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DesignDraftService {

    private final DesignDraftMapper designDraftMapper;

    public DesignDraftService(DesignDraftMapper designDraftMapper) {
        this.designDraftMapper = designDraftMapper;
    }

    public Map<String, Object> save(Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        Long draftId = parseLong(body.get("draftId"));

        DesignDraftEntity entity;
        if (draftId == null) {
            entity = new DesignDraftEntity();
            entity.setDraftNo(MockUtil.nextNo("D"));
            entity.setUserId(userId);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setStatus("ACTIVE");
        } else {
            entity = designDraftMapper.selectOne(new LambdaQueryWrapper<DesignDraftEntity>()
                    .eq(DesignDraftEntity::getId, draftId)
                    .eq(DesignDraftEntity::getUserId, userId)
                    .last("LIMIT 1"));
            if (entity == null) {
                entity = new DesignDraftEntity();
                entity.setId(draftId);
                entity.setDraftNo(MockUtil.nextNo("D"));
                entity.setUserId(userId);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setStatus("ACTIVE");
            }
        }

        entity.setTitle(parseText(body.get("title")));
        entity.setWristSizeCm(parseDecimal(body.get("wristSizeCm")));
        entity.setConfigJson(parseText(body.get("configJson")));
        entity.setPreviewImageUrl(parseText(body.get("previewImageUrl")));
        entity.setPricePreview(parseDecimal(body.get("pricePreview")));
        entity.setVersion(parseInt(body.get("version"), 1));
        entity.setUpdatedAt(LocalDateTime.now());

        if (entity.getId() == null) {
            designDraftMapper.insert(entity);
        } else {
            designDraftMapper.updateById(entity);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("draftId", entity.getId());
        data.put("draftNo", entity.getDraftNo());
        data.put("updatedAt", entity.getUpdatedAt());
        return data;
    }

    public Map<String, Object> list(Integer pageNo, Integer pageSize) {
        Long userId = UserContext.currentUserId();
        Page<DesignDraftEntity> page = designDraftMapper.selectPage(
                new Page<DesignDraftEntity>(pageNo, pageSize),
                new LambdaQueryWrapper<DesignDraftEntity>()
                        .eq(DesignDraftEntity::getUserId, userId)
                        .orderByDesc(DesignDraftEntity::getUpdatedAt, DesignDraftEntity::getId)
        );

        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", pageNo);
        pageData.put("pageSize", pageSize);
        pageData.put("total", page.getTotal());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", page.getRecords());
        data.put("page", pageData);
        return data;
    }

    public DesignDraftEntity detail(Long id) {
        Long userId = UserContext.currentUserId();
        return designDraftMapper.selectOne(new LambdaQueryWrapper<DesignDraftEntity>()
                .eq(DesignDraftEntity::getId, id)
                .eq(DesignDraftEntity::getUserId, userId)
                .last("LIMIT 1"));
    }

    public Boolean delete(Long id) {
        Long userId = UserContext.currentUserId();
        designDraftMapper.delete(new LambdaQueryWrapper<DesignDraftEntity>()
                .eq(DesignDraftEntity::getId, id)
                .eq(DesignDraftEntity::getUserId, userId));
        return Boolean.TRUE;
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

    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(text);
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
