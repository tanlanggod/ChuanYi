package com.chuanyi.backend.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.content.entity.BannerContentEntity;
import com.chuanyi.backend.modules.content.entity.DesignGalleryEntity;
import com.chuanyi.backend.modules.content.mapper.BannerContentMapper;
import com.chuanyi.backend.modules.content.mapper.DesignGalleryMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminContentService {

    private final BannerContentMapper bannerContentMapper;
    private final DesignGalleryMapper designGalleryMapper;

    public AdminContentService(BannerContentMapper bannerContentMapper,
                               DesignGalleryMapper designGalleryMapper) {
        this.bannerContentMapper = bannerContentMapper;
        this.designGalleryMapper = designGalleryMapper;
    }

    public Map<String, Object> listBanners(String keyword,
                                           String status,
                                           Integer pageNo,
                                           Integer pageSize) {
        LambdaQueryWrapper<BannerContentEntity> wrapper = new LambdaQueryWrapper<BannerContentEntity>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(BannerContentEntity::getTitle, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BannerContentEntity::getStatus, status);
        }
        wrapper.orderByAsc(BannerContentEntity::getSortNo, BannerContentEntity::getId);

        Page<BannerContentEntity> page = bannerContentMapper.selectPage(new Page<BannerContentEntity>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (BannerContentEntity banner : page.getRecords()) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("id", banner.getId());
            row.put("title", banner.getTitle());
            row.put("imageUrl", banner.getImageUrl());
            row.put("jumpType", banner.getJumpType());
            row.put("jumpValue", banner.getJumpValue());
            row.put("sortNo", banner.getSortNo());
            row.put("status", banner.getStatus());
            rows.add(row);
        }
        return buildPageData(rows, pageNo, pageSize, page.getTotal());
    }

    public Map<String, Object> updateBannerStatus(Long id, Map<String, Object> body) {
        BannerContentEntity entity = bannerContentMapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "banner not found");
        }
        String status = parseText(body == null ? null : body.get("status"));
        if (!"ENABLED".equalsIgnoreCase(status) && !"DISABLED".equalsIgnoreCase(status)) {
            throw new BizException(400, "status must be ENABLED or DISABLED");
        }
        entity.setStatus(status.toUpperCase());
        bannerContentMapper.updateById(entity);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("status", entity.getStatus());
        return data;
    }

    public Map<String, Object> saveBanner(Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        Long id = parseLong(request.get("id"));
        BannerContentEntity entity = id == null ? new BannerContentEntity() : bannerContentMapper.selectById(id);
        if (id != null && entity == null) {
            throw new BizException(404, "banner not found");
        }

        String title = parseText(request.get("title"));
        String imageUrl = parseText(request.get("imageUrl"));
        String jumpType = parseText(request.get("jumpType"));
        String jumpValue = parseText(request.get("jumpValue"));
        Integer sortNo = parseInt(request.get("sortNo"));
        String status = parseText(request.get("status"));

        if (!StringUtils.hasText(title)) {
            throw new BizException(400, "title is required");
        }
        entity.setTitle(title);
        entity.setImageUrl(imageUrl);
        entity.setJumpType(StringUtils.hasText(jumpType) ? jumpType.toUpperCase() : "NONE");
        entity.setJumpValue(jumpValue);
        entity.setSortNo(sortNo == null ? 0 : sortNo);
        entity.setStatus(StringUtils.hasText(status) ? status.toUpperCase() : "ENABLED");

        if (entity.getId() == null) {
            bannerContentMapper.insert(entity);
        } else {
            bannerContentMapper.updateById(entity);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("title", entity.getTitle());
        data.put("status", entity.getStatus());
        return data;
    }

    public Map<String, Object> listGallery(String keyword,
                                           String displayStatus,
                                           Integer pageNo,
                                           Integer pageSize) {
        LambdaQueryWrapper<DesignGalleryEntity> wrapper = new LambdaQueryWrapper<DesignGalleryEntity>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DesignGalleryEntity::getTitle, keyword);
        }
        if (StringUtils.hasText(displayStatus)) {
            wrapper.eq(DesignGalleryEntity::getDisplayStatus, displayStatus);
        }
        wrapper.orderByDesc(DesignGalleryEntity::getPublishedAt, DesignGalleryEntity::getId);

        Page<DesignGalleryEntity> page = designGalleryMapper.selectPage(new Page<DesignGalleryEntity>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (DesignGalleryEntity gallery : page.getRecords()) {
            Map<String, Object> row = new HashMap<String, Object>();
            row.put("id", gallery.getId());
            row.put("snapshotId", gallery.getSnapshotId());
            row.put("userId", gallery.getUserId());
            row.put("title", gallery.getTitle());
            row.put("coverImageUrl", gallery.getCoverImageUrl());
            row.put("tags", parseTags(gallery.getTagsJson()));
            row.put("displayStatus", gallery.getDisplayStatus());
            row.put("publishedAt", gallery.getPublishedAt());
            rows.add(row);
        }
        return buildPageData(rows, pageNo, pageSize, page.getTotal());
    }

    public Map<String, Object> updateGalleryStatus(Long id, Map<String, Object> body) {
        DesignGalleryEntity entity = designGalleryMapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "gallery not found");
        }
        String displayStatus = parseText(body == null ? null : body.get("displayStatus"));
        if (!"ENABLED".equalsIgnoreCase(displayStatus)
                && !"DISABLED".equalsIgnoreCase(displayStatus)
                && !"HIDDEN".equalsIgnoreCase(displayStatus)) {
            throw new BizException(400, "displayStatus must be ENABLED, DISABLED or HIDDEN");
        }
        entity.setDisplayStatus(displayStatus.toUpperCase());
        designGalleryMapper.updateById(entity);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("displayStatus", entity.getDisplayStatus());
        return data;
    }

    public Map<String, Object> saveGallery(Long id, Map<String, Object> body) {
        DesignGalleryEntity entity = designGalleryMapper.selectById(id);
        if (entity == null) {
            throw new BizException(404, "gallery not found");
        }
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        String title = parseText(request.get("title"));
        String coverImageUrl = parseText(request.get("coverImageUrl"));
        String tags = parseText(request.get("tags"));
        String displayStatus = parseText(request.get("displayStatus"));

        if (StringUtils.hasText(title)) {
            entity.setTitle(title);
        }
        if (coverImageUrl != null) {
            entity.setCoverImageUrl(coverImageUrl);
        }
        if (tags != null) {
            entity.setTagsJson(toTagsJson(tags));
        }
        if (StringUtils.hasText(displayStatus)) {
            String normalized = displayStatus.toUpperCase();
            if (!"ENABLED".equals(normalized) && !"DISABLED".equals(normalized) && !"HIDDEN".equals(normalized)) {
                throw new BizException(400, "invalid displayStatus");
            }
            entity.setDisplayStatus(normalized);
        }
        designGalleryMapper.updateById(entity);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("title", entity.getTitle());
        data.put("displayStatus", entity.getDisplayStatus());
        return data;
    }

    private Map<String, Object> buildPageData(List<Map<String, Object>> list,
                                              Integer pageNo,
                                              Integer pageSize,
                                              long total) {
        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", pageNo);
        pageData.put("pageSize", pageSize);
        pageData.put("total", total);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("page", pageData);
        return data;
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
}
