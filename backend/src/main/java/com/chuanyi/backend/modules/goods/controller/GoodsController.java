package com.chuanyi.backend.modules.goods.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.modules.goods.service.GoodsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping("/categories")
    public ApiResponse<Map<String, Object>> categories(@RequestParam(defaultValue = "true") Boolean withCount) {
        return ApiResponse.ok(goodsService.categories(withCount));
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "DEFAULT") String sortType,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.ok(goodsService.list(categoryId, keyword, sortType, pageNo, pageSize));
    }

    @GetMapping("/{spuId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long spuId) {
        return ApiResponse.ok(goodsService.detail(spuId));
    }
}
