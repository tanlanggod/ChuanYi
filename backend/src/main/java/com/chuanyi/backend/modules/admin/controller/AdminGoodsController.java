package com.chuanyi.backend.modules.admin.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.web.AuthHeaderUtil;
import com.chuanyi.backend.modules.admin.service.AdminAuthService;
import com.chuanyi.backend.modules.admin.service.AdminGoodsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/goods")
public class AdminGoodsController {

    private final AdminAuthService adminAuthService;
    private final AdminGoodsService adminGoodsService;

    public AdminGoodsController(AdminAuthService adminAuthService,
                                AdminGoodsService adminGoodsService) {
        this.adminAuthService = adminAuthService;
        this.adminGoodsService = adminGoodsService;
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String salesStatus,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.list(categoryId, keyword, salesStatus, pageNo, pageSize));
    }

    @GetMapping("/{spuId}")
    public ApiResponse<Map<String, Object>> detail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @PathVariable Long spuId) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.detail(spuId));
    }

    @PostMapping("/{spuId}/status")
    public ApiResponse<Map<String, Object>> updateStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                         @PathVariable Long spuId,
                                                         @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.updateSalesStatus(spuId, body));
    }

    @PostMapping("/{spuId}/save")
    public ApiResponse<Map<String, Object>> save(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @PathVariable Long spuId,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.save(spuId, body));
    }

    @PostMapping("/{spuId}/sku/create")
    public ApiResponse<Map<String, Object>> createSku(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @PathVariable Long spuId,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.createSku(spuId, body));
    }

    @PostMapping("/sku/{skuId}/save")
    public ApiResponse<Map<String, Object>> saveSku(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @PathVariable Long skuId,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.saveSku(skuId, body));
    }

    @PostMapping("/sku/{skuId}/status")
    public ApiResponse<Map<String, Object>> updateSkuStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                            @PathVariable Long skuId,
                                                            @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.updateSkuStatus(skuId, body));
    }

    @DeleteMapping("/sku/{skuId}")
    public ApiResponse<Boolean> deleteSku(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable Long skuId) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminGoodsService.deleteSku(skuId));
    }
}
