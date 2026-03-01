package com.chuanyi.backend.modules.snapshot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("design_snapshot")
public class DesignSnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String snapshotNo;
    private Long userId;
    private Long sourceDraftId;
    private BigDecimal wristSizeCm;
    private String snapshotJson;
    private String previewImageUrl;
    private BigDecimal priceSnapshot;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSnapshotNo() {
        return snapshotNo;
    }

    public void setSnapshotNo(String snapshotNo) {
        this.snapshotNo = snapshotNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSourceDraftId() {
        return sourceDraftId;
    }

    public void setSourceDraftId(Long sourceDraftId) {
        this.sourceDraftId = sourceDraftId;
    }

    public BigDecimal getWristSizeCm() {
        return wristSizeCm;
    }

    public void setWristSizeCm(BigDecimal wristSizeCm) {
        this.wristSizeCm = wristSizeCm;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public void setPreviewImageUrl(String previewImageUrl) {
        this.previewImageUrl = previewImageUrl;
    }

    public BigDecimal getPriceSnapshot() {
        return priceSnapshot;
    }

    public void setPriceSnapshot(BigDecimal priceSnapshot) {
        this.priceSnapshot = priceSnapshot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
