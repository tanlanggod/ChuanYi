package com.chuanyi.backend.modules.content.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.modules.content.service.ContentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping("/api/content/banner/list")
    public ApiResponse<Map<String, Object>> banners() {
        return ApiResponse.ok(contentService.banners());
    }

    @GetMapping("/api/gallery/list")
    public ApiResponse<Map<String, Object>> gallery(@RequestParam(defaultValue = "1") Integer pageNo,
                                                    @RequestParam(defaultValue = "20") Integer pageSize,
                                                    @RequestParam(defaultValue = "PUBLISH_TIME") String sortType) {
        return ApiResponse.ok(contentService.gallery(pageNo, pageSize, sortType));
    }

    @GetMapping("/api/gallery/{id}")
    public ApiResponse<Map<String, Object>> galleryDetail(@PathVariable Long id) {
        return ApiResponse.ok(contentService.galleryDetail(id));
    }

    @PostMapping("/api/gallery/publish")
    public ApiResponse<Map<String, Object>> publish(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(contentService.publish(body));
    }

    @PostMapping("/api/gallery/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id) {
        return ApiResponse.ok(contentService.like(id));
    }

    @DeleteMapping("/api/gallery/{id}/like")
    public ApiResponse<Map<String, Object>> unlike(@PathVariable Long id) {
        return ApiResponse.ok(contentService.unlike(id));
    }
}
