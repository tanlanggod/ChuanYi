package com.chuanyi.backend.modules.snapshot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.common.util.MockUtil;
import com.chuanyi.backend.modules.design.entity.DesignDraftEntity;
import com.chuanyi.backend.modules.design.mapper.DesignDraftMapper;
import com.chuanyi.backend.modules.snapshot.entity.DesignSnapshotEntity;
import com.chuanyi.backend.modules.snapshot.mapper.DesignSnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SnapshotService {

    private final DesignDraftMapper designDraftMapper;
    private final DesignSnapshotMapper designSnapshotMapper;

    public SnapshotService(DesignDraftMapper designDraftMapper,
                           DesignSnapshotMapper designSnapshotMapper) {
        this.designDraftMapper = designDraftMapper;
        this.designSnapshotMapper = designSnapshotMapper;
    }

    public Map<String, Object> createFromDraft(Map<String, Object> body) {
        Long userId = UserContext.currentUserId();
        Long draftId = parseLong(body.get("draftId"));
        if (draftId == null) {
            throw new BizException(400, "draftId is required");
        }

        DesignDraftEntity draft = designDraftMapper.selectOne(new LambdaQueryWrapper<DesignDraftEntity>()
                .eq(DesignDraftEntity::getId, draftId)
                .eq(DesignDraftEntity::getUserId, userId)
                .last("LIMIT 1"));
        if (draft == null) {
            throw new BizException(404, "draft not found");
        }

        DesignSnapshotEntity snapshot = new DesignSnapshotEntity();
        snapshot.setSnapshotNo(MockUtil.nextNo("S"));
        snapshot.setUserId(userId);
        snapshot.setSourceDraftId(draft.getId());
        snapshot.setWristSizeCm(draft.getWristSizeCm());
        snapshot.setSnapshotJson(draft.getConfigJson());
        snapshot.setPreviewImageUrl(draft.getPreviewImageUrl());
        snapshot.setPriceSnapshot(draft.getPricePreview());
        snapshot.setCreatedAt(LocalDateTime.now());
        designSnapshotMapper.insert(snapshot);

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("snapshotNo", snapshot.getSnapshotNo());
        data.put("previewImageUrl", snapshot.getPreviewImageUrl());
        data.put("priceSnapshot", snapshot.getPriceSnapshot());
        return data;
    }

    public DesignSnapshotEntity detail(String snapshotNo) {
        Long userId = UserContext.currentUserId();
        if (!StringUtils.hasText(snapshotNo)) {
            throw new BizException(400, "snapshotNo is required");
        }
        return designSnapshotMapper.selectOne(new LambdaQueryWrapper<DesignSnapshotEntity>()
                .eq(DesignSnapshotEntity::getSnapshotNo, snapshotNo)
                .eq(DesignSnapshotEntity::getUserId, userId)
                .last("LIMIT 1"));
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
}
