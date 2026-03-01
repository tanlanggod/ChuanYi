package com.chuanyi.backend.modules.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.content.entity.BannerContentEntity;
import com.chuanyi.backend.modules.content.entity.DesignGalleryEntity;
import com.chuanyi.backend.modules.content.entity.DesignGalleryLikeEntity;
import com.chuanyi.backend.modules.content.entity.DesignGalleryStatEntity;
import com.chuanyi.backend.modules.content.mapper.BannerContentMapper;
import com.chuanyi.backend.modules.content.mapper.DesignGalleryLikeMapper;
import com.chuanyi.backend.modules.content.mapper.DesignGalleryMapper;
import com.chuanyi.backend.modules.content.mapper.DesignGalleryStatMapper;
import com.chuanyi.backend.modules.snapshot.entity.DesignSnapshotEntity;
import com.chuanyi.backend.modules.snapshot.mapper.DesignSnapshotMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ContentService {

    private static final String SORT_TYPE_PUBLISH_TIME = "PUBLISH_TIME";
    private static final String SORT_TYPE_HOT = "HOT";

    private final BannerContentMapper bannerContentMapper;
    private final DesignGalleryMapper designGalleryMapper;
    private final DesignGalleryStatMapper designGalleryStatMapper;
    private final DesignGalleryLikeMapper designGalleryLikeMapper;
    private final DesignSnapshotMapper designSnapshotMapper;

    public ContentService(BannerContentMapper bannerContentMapper,
                          DesignGalleryMapper designGalleryMapper,
                          DesignGalleryStatMapper designGalleryStatMapper,
                          DesignGalleryLikeMapper designGalleryLikeMapper,
                          DesignSnapshotMapper designSnapshotMapper) {
        this.bannerContentMapper = bannerContentMapper;
        this.designGalleryMapper = designGalleryMapper;
        this.designGalleryStatMapper = designGalleryStatMapper;
        this.designGalleryLikeMapper = designGalleryLikeMapper;
        this.designSnapshotMapper = designSnapshotMapper;
    }

    public Map<String, Object> banners() {
        List<BannerContentEntity> banners = bannerContentMapper.selectList(
                new LambdaQueryWrapper<BannerContentEntity>()
                        .eq(BannerContentEntity::getStatus, "ENABLED")
                        .orderByAsc(BannerContentEntity::getSortNo, BannerContentEntity::getId)
        );
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", banners);
        return data;
    }

    public Map<String, Object> gallery(Integer pageNo, Integer pageSize, String sortType) {
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        long total = designGalleryMapper.countEnabled();
        String normalizedSortType = normalizeSortType(sortType);

        List<DesignGalleryEntity> rows;
        if (SORT_TYPE_HOT.equals(normalizedSortType)) {
            long offset = (long) (safePageNo - 1) * safePageSize;
            rows = designGalleryMapper.selectHotPage(offset, safePageSize);
        } else {
            Page<DesignGalleryEntity> page = designGalleryMapper.selectPage(
                    new Page<DesignGalleryEntity>(safePageNo, safePageSize),
                    new LambdaQueryWrapper<DesignGalleryEntity>()
                            .eq(DesignGalleryEntity::getDisplayStatus, "ENABLED")
                            .orderByDesc(DesignGalleryEntity::getPublishedAt, DesignGalleryEntity::getId)
            );
            rows = page.getRecords();
            total = page.getTotal();
        }

        List<Long> galleryIds = new ArrayList<Long>();
        for (DesignGalleryEntity row : rows) {
            if (row != null && row.getId() != null) {
                galleryIds.add(row.getId());
            }
        }
        Map<Long, DesignGalleryStatEntity> statMap = buildStatMap(galleryIds);
        Map<Long, Boolean> likedMap = buildLikedMap(galleryIds, UserContext.currentUserId());

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (DesignGalleryEntity item : rows) {
            DesignGalleryStatEntity stat = statMap.get(item.getId());
            boolean liked = Boolean.TRUE.equals(likedMap.get(item.getId()));
            list.add(mapGalleryItem(item, stat, liked));
        }

        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("pageNo", safePageNo);
        pageData.put("pageSize", safePageSize);
        pageData.put("total", total);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list);
        data.put("page", pageData);
        return data;
    }

    public Map<String, Object> galleryDetail(Long id) {
        if (id == null) {
            throw new BizException(400, "id is required");
        }
        DesignGalleryEntity item = requireEnabledGallery(id);
        ensureStatRow(id);
        increaseViewCount(id);
        DesignGalleryStatEntity stat = designGalleryStatMapper.selectById(id);
        boolean liked = isLikedByCurrentUser(id, UserContext.currentUserId());
        return mapGalleryItem(item, stat, liked);
    }

    public Map<String, Object> publish(Map<String, Object> body) {
        Map<String, Object> request = body == null ? new HashMap<String, Object>() : body;
        Long userId = UserContext.currentUserId();
        String snapshotNo = parseText(request.get("snapshotNo"));
        String title = parseText(request.get("title"));
        String coverImageUrl = parseText(request.get("coverImageUrl"));
        List<String> tags = normalizeTags(request.get("tags"));

        if (!StringUtils.hasText(snapshotNo)) {
            throw new BizException(400, "snapshotNo is required");
        }
        if (!StringUtils.hasText(title)) {
            throw new BizException(400, "title is required");
        }

        DesignSnapshotEntity snapshot = designSnapshotMapper.selectOne(
                new LambdaQueryWrapper<DesignSnapshotEntity>()
                        .eq(DesignSnapshotEntity::getSnapshotNo, snapshotNo)
                        .eq(DesignSnapshotEntity::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (snapshot == null) {
            throw new BizException(404, "snapshot not found");
        }

        LocalDateTime now = LocalDateTime.now();
        DesignGalleryEntity gallery = designGalleryMapper.selectOne(
                new LambdaQueryWrapper<DesignGalleryEntity>()
                        .eq(DesignGalleryEntity::getSnapshotId, snapshot.getId())
                        .eq(DesignGalleryEntity::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (gallery == null) {
            gallery = new DesignGalleryEntity();
            gallery.setSnapshotId(snapshot.getId());
            gallery.setUserId(userId);
            gallery.setTitle(title.trim());
            gallery.setCoverImageUrl(StringUtils.hasText(coverImageUrl) ? coverImageUrl.trim() : snapshot.getPreviewImageUrl());
            gallery.setTagsJson(toTagsJson(tags));
            gallery.setDisplayStatus("ENABLED");
            gallery.setPublishedAt(now);
            designGalleryMapper.insert(gallery);
        } else {
            gallery.setTitle(title.trim());
            gallery.setCoverImageUrl(StringUtils.hasText(coverImageUrl) ? coverImageUrl.trim() : snapshot.getPreviewImageUrl());
            gallery.setTagsJson(toTagsJson(tags));
            gallery.setDisplayStatus("ENABLED");
            gallery.setPublishedAt(now);
            designGalleryMapper.updateById(gallery);
        }
        ensureStatRow(gallery.getId());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", gallery.getId());
        data.put("snapshotId", gallery.getSnapshotId());
        data.put("publishedAt", gallery.getPublishedAt());
        data.put("displayStatus", gallery.getDisplayStatus());
        return data;
    }

    public Map<String, Object> like(Long id) {
        if (id == null) {
            throw new BizException(400, "id is required");
        }
        requireEnabledGallery(id);
        Long userId = UserContext.currentUserId();
        ensureStatRow(id);

        boolean inserted = insertLikeIfAbsent(id, userId);
        if (inserted) {
            increaseLikeCount(id);
        }
        DesignGalleryStatEntity stat = designGalleryStatMapper.selectById(id);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("galleryId", id);
        data.put("liked", true);
        data.put("likeCount", stat == null || stat.getLikeCount() == null ? 0 : stat.getLikeCount());
        return data;
    }

    public Map<String, Object> unlike(Long id) {
        if (id == null) {
            throw new BizException(400, "id is required");
        }
        requireEnabledGallery(id);
        Long userId = UserContext.currentUserId();
        ensureStatRow(id);

        int deletedRows = designGalleryLikeMapper.delete(
                new LambdaQueryWrapper<DesignGalleryLikeEntity>()
                        .eq(DesignGalleryLikeEntity::getGalleryId, id)
                        .eq(DesignGalleryLikeEntity::getUserId, userId)
        );
        if (deletedRows > 0) {
            decreaseLikeCount(id);
        }
        DesignGalleryStatEntity stat = designGalleryStatMapper.selectById(id);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("galleryId", id);
        data.put("liked", false);
        data.put("likeCount", stat == null || stat.getLikeCount() == null ? 0 : stat.getLikeCount());
        return data;
    }

    private Map<Long, DesignGalleryStatEntity> buildStatMap(List<Long> galleryIds) {
        Map<Long, DesignGalleryStatEntity> result = new HashMap<Long, DesignGalleryStatEntity>();
        if (galleryIds == null || galleryIds.isEmpty()) {
            return result;
        }
        List<DesignGalleryStatEntity> stats = designGalleryStatMapper.selectBatchIds(galleryIds);
        for (DesignGalleryStatEntity stat : stats) {
            if (stat != null && stat.getGalleryId() != null) {
                result.put(stat.getGalleryId(), stat);
            }
        }
        return result;
    }

    private Map<Long, Boolean> buildLikedMap(List<Long> galleryIds, Long userId) {
        Map<Long, Boolean> result = new HashMap<Long, Boolean>();
        if (userId == null || galleryIds == null || galleryIds.isEmpty()) {
            return result;
        }
        List<DesignGalleryLikeEntity> likes = designGalleryLikeMapper.selectList(
                new LambdaQueryWrapper<DesignGalleryLikeEntity>()
                        .eq(DesignGalleryLikeEntity::getUserId, userId)
                        .in(DesignGalleryLikeEntity::getGalleryId, galleryIds)
        );
        for (DesignGalleryLikeEntity like : likes) {
            if (like != null && like.getGalleryId() != null) {
                result.put(like.getGalleryId(), true);
            }
        }
        return result;
    }

    private DesignGalleryEntity requireEnabledGallery(Long id) {
        DesignGalleryEntity item = designGalleryMapper.selectOne(
                new LambdaQueryWrapper<DesignGalleryEntity>()
                        .eq(DesignGalleryEntity::getId, id)
                        .eq(DesignGalleryEntity::getDisplayStatus, "ENABLED")
                        .last("LIMIT 1")
        );
        if (item == null) {
            throw new BizException(404, "gallery not found");
        }
        return item;
    }

    private boolean isLikedByCurrentUser(Long galleryId, Long userId) {
        if (galleryId == null || userId == null) {
            return false;
        }
        DesignGalleryLikeEntity like = designGalleryLikeMapper.selectOne(
                new LambdaQueryWrapper<DesignGalleryLikeEntity>()
                        .eq(DesignGalleryLikeEntity::getGalleryId, galleryId)
                        .eq(DesignGalleryLikeEntity::getUserId, userId)
                        .last("LIMIT 1")
        );
        return like != null;
    }

    private boolean insertLikeIfAbsent(Long galleryId, Long userId) {
        DesignGalleryLikeEntity like = new DesignGalleryLikeEntity();
        like.setGalleryId(galleryId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        try {
            return designGalleryLikeMapper.insert(like) > 0;
        } catch (DuplicateKeyException duplicateKeyException) {
            return false;
        }
    }

    private void ensureStatRow(Long galleryId) {
        if (galleryId == null) {
            return;
        }
        DesignGalleryStatEntity stat = designGalleryStatMapper.selectById(galleryId);
        if (stat != null) {
            return;
        }
        DesignGalleryStatEntity entity = new DesignGalleryStatEntity();
        entity.setGalleryId(galleryId);
        entity.setViewCount(0);
        entity.setLikeCount(0);
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            designGalleryStatMapper.insert(entity);
        } catch (DuplicateKeyException ignored) {
            // idempotent protection when concurrent insert happens
        }
    }

    private void increaseViewCount(Long galleryId) {
        designGalleryStatMapper.update(
                null,
                new LambdaUpdateWrapper<DesignGalleryStatEntity>()
                        .eq(DesignGalleryStatEntity::getGalleryId, galleryId)
                        .setSql("view_count = view_count + 1")
                        .set(DesignGalleryStatEntity::getUpdatedAt, LocalDateTime.now())
        );
    }

    private void increaseLikeCount(Long galleryId) {
        designGalleryStatMapper.update(
                null,
                new LambdaUpdateWrapper<DesignGalleryStatEntity>()
                        .eq(DesignGalleryStatEntity::getGalleryId, galleryId)
                        .setSql("like_count = like_count + 1")
                        .set(DesignGalleryStatEntity::getUpdatedAt, LocalDateTime.now())
        );
    }

    private void decreaseLikeCount(Long galleryId) {
        designGalleryStatMapper.update(
                null,
                new LambdaUpdateWrapper<DesignGalleryStatEntity>()
                        .eq(DesignGalleryStatEntity::getGalleryId, galleryId)
                        .setSql("like_count = IF(like_count > 0, like_count - 1, 0)")
                        .set(DesignGalleryStatEntity::getUpdatedAt, LocalDateTime.now())
        );
    }

    private Map<String, Object> mapGalleryItem(DesignGalleryEntity item,
                                               DesignGalleryStatEntity stat,
                                               boolean isLiked) {
        int viewCount = stat == null || stat.getViewCount() == null ? 0 : stat.getViewCount();
        int likeCount = stat == null || stat.getLikeCount() == null ? 0 : stat.getLikeCount();
        long hotScore = likeCount * 10L + viewCount;

        Map<String, Object> mapped = new HashMap<String, Object>();
        mapped.put("id", item.getId());
        mapped.put("snapshotId", item.getSnapshotId());
        mapped.put("userId", item.getUserId());
        mapped.put("title", item.getTitle());
        mapped.put("coverImageUrl", item.getCoverImageUrl());
        mapped.put("tags", parseTags(item.getTagsJson()));
        mapped.put("authorNickname", item.getUserId() == null ? "Guest" : "User" + item.getUserId());
        mapped.put("displayStatus", item.getDisplayStatus());
        mapped.put("publishedAt", item.getPublishedAt());
        mapped.put("viewCount", viewCount);
        mapped.put("likeCount", likeCount);
        mapped.put("hotScore", hotScore);
        mapped.put("isLiked", isLiked);
        return mapped;
    }

    private String normalizeSortType(String sortType) {
        if (SORT_TYPE_HOT.equalsIgnoreCase(sortType)) {
            return SORT_TYPE_HOT;
        }
        return SORT_TYPE_PUBLISH_TIME;
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    private List<String> normalizeTags(Object rawTags) {
        List<String> source = new ArrayList<String>();
        if (rawTags instanceof List) {
            List<?> list = (List<?>) rawTags;
            for (Object item : list) {
                source.add(item == null ? "" : String.valueOf(item));
            }
        } else if (rawTags != null) {
            String text = String.valueOf(rawTags);
            String[] parts = text.split("[,，]");
            for (String part : parts) {
                source.add(part);
            }
        }

        Set<String> dedup = new LinkedHashSet<String>();
        for (String tag : source) {
            String normalized = tag == null ? "" : tag.trim();
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            if (normalized.length() > 10) {
                throw new BizException(400, "tag length must be <= 10");
            }
            dedup.add(normalized);
            if (dedup.size() > 5) {
                throw new BizException(400, "tags size must be <= 5");
            }
        }
        return new ArrayList<String>(dedup);
    }

    private String toTagsJson(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(tags.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
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
