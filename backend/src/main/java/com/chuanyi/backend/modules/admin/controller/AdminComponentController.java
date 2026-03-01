package com.chuanyi.backend.modules.admin.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.web.AuthHeaderUtil;
import com.chuanyi.backend.modules.admin.service.AdminAuthService;
import com.chuanyi.backend.modules.admin.service.AdminComponentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/component")
public class AdminComponentController {

    private final AdminAuthService adminAuthService;
    private final AdminComponentService adminComponentService;

    public AdminComponentController(AdminAuthService adminAuthService,
                                    AdminComponentService adminComponentService) {
        this.adminAuthService = adminAuthService;
        this.adminComponentService = adminComponentService;
    }

    @GetMapping("/categories")
    public ApiResponse<Map<String, Object>> categories(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                       @RequestParam(defaultValue = "true") Boolean withCount) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.categories(withCount));
    }

    @PostMapping("/category/save")
    public ApiResponse<Map<String, Object>> saveCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                         @RequestBody Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.saveCategory(body));
    }

    @PostMapping("/category/{id}/status")
    public ApiResponse<Map<String, Object>> updateCategoryStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                 @PathVariable Long id,
                                                                 @RequestBody Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.updateCategoryStatus(id, body));
    }

    @DeleteMapping("/category/{id}")
    public ApiResponse<Boolean> deleteCategory(@RequestHeader(value = "Authorization", required = false) String authorization,
                                               @PathVariable Long id) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.deleteCategory(id));
    }

    @GetMapping("/sku/list")
    public ApiResponse<Map<String, Object>> skuList(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String salesStatus,
                                                    @RequestParam(defaultValue = "1") Integer pageNo,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.skuList(categoryId, keyword, salesStatus, pageNo, pageSize));
    }

    @PostMapping("/sku/create")
    public ApiResponse<Map<String, Object>> createSku(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                      @RequestBody Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.createSku(body));
    }

    @PostMapping("/sku/{skuId}/save")
    public ApiResponse<Map<String, Object>> saveSku(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                    @PathVariable Long skuId,
                                                    @RequestBody Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.saveSku(skuId, body));
    }

    @PostMapping("/sku/{skuId}/status")
    public ApiResponse<Map<String, Object>> updateSkuStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                            @PathVariable Long skuId,
                                                            @RequestBody Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.updateSkuStatus(skuId, body));
    }

    @PostMapping("/sku/batch-status")
    public ApiResponse<Map<String, Object>> batchUpdateSkuStatus(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                                 @RequestBody Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.batchUpdateSkuStatus(body));
    }

    @DeleteMapping("/sku/{skuId}")
    public ApiResponse<Boolean> deleteSku(@RequestHeader(value = "Authorization", required = false) String authorization,
                                          @PathVariable Long skuId) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminComponentService.deleteSku(skuId));
    }
}
