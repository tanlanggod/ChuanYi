package com.chuanyi.backend.modules.component.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.modules.component.service.ComponentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sku")
public class SkuController {

    private final ComponentService componentService;

    public SkuController(ComponentService componentService) {
        this.componentService = componentService;
    }

    @GetMapping("/categories")
    public ApiResponse<Map<String, Object>> categories() {
        return ApiResponse.ok(componentService.categories());
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(componentService.list(categoryId, keyword, pageNo, pageSize));
    }
}
