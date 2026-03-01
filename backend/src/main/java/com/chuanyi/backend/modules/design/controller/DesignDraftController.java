package com.chuanyi.backend.modules.design.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.design.entity.DesignDraftEntity;
import com.chuanyi.backend.modules.design.service.DesignDraftService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/design/draft")
public class DesignDraftController {

    private final DesignDraftService designDraftService;

    public DesignDraftController(DesignDraftService designDraftService) {
        this.designDraftService = designDraftService;
    }

    @PostMapping("/save")
    public ApiResponse<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(designDraftService.save(body));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(designDraftService.list(pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        DesignDraftEntity entity = designDraftService.detail(id);
        if (entity == null) {
            throw new BizException(404, "draft not found");
        }
        Map<String, Object> data = new java.util.HashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("draftNo", entity.getDraftNo());
        data.put("title", entity.getTitle());
        data.put("wristSizeCm", entity.getWristSizeCm());
        data.put("pricePreview", entity.getPricePreview());
        data.put("previewImageUrl", entity.getPreviewImageUrl());
        data.put("configJson", entity.getConfigJson());
        data.put("version", entity.getVersion());
        data.put("updatedAt", entity.getUpdatedAt());
        return ApiResponse.ok(data);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> delete(@PathVariable Long id) {
        return ApiResponse.ok(designDraftService.delete(id));
    }
}
