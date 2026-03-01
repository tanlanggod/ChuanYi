package com.chuanyi.backend.modules.snapshot.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.snapshot.entity.DesignSnapshotEntity;
import com.chuanyi.backend.modules.snapshot.service.SnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/design/snapshot")
public class SnapshotController {

    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @PostMapping("/create-from-draft")
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(snapshotService.createFromDraft(body));
    }

    @GetMapping("/{snapshotNo}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String snapshotNo) {
        DesignSnapshotEntity entity = snapshotService.detail(snapshotNo);
        if (entity == null) {
            throw new BizException(404, "snapshot not found");
        }
        Map<String, Object> data = new java.util.HashMap<String, Object>();
        data.put("snapshotNo", entity.getSnapshotNo());
        data.put("title", "独一无二的定制");
        data.put("wristSizeCm", entity.getWristSizeCm());
        data.put("snapshotJson", entity.getSnapshotJson());
        data.put("previewImageUrl", entity.getPreviewImageUrl());
        data.put("priceSnapshot", entity.getPriceSnapshot());
        return ApiResponse.ok(data);
    }
}
