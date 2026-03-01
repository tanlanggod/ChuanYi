package com.chuanyi.backend.modules.admin.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.web.AuthHeaderUtil;
import com.chuanyi.backend.modules.admin.service.AdminAuthService;
import com.chuanyi.backend.modules.admin.service.AdminOrderService;
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
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    private final AdminAuthService adminAuthService;
    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminAuthService adminAuthService,
                                AdminOrderService adminOrderService) {
        this.adminAuthService = adminAuthService;
        this.adminOrderService = adminOrderService;
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") Integer pageNo,
                                                 @RequestParam(defaultValue = "20") Integer pageSize) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminOrderService.list(status, keyword, pageNo, pageSize));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<Map<String, Object>> detail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @PathVariable String orderNo) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminOrderService.detail(orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<Map<String, Object>> cancel(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                   @PathVariable String orderNo,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        Long adminId = adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminOrderService.cancel(orderNo, body, adminId));
    }

    @PostMapping("/{orderNo}/ship")
    public ApiResponse<Map<String, Object>> ship(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                 @PathVariable String orderNo,
                                                 @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        Long adminId = adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminOrderService.ship(orderNo, body, adminId));
    }

    @PostMapping("/{orderNo}/complete")
    public ApiResponse<Map<String, Object>> complete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     @PathVariable String orderNo,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        String token = AuthHeaderUtil.extractToken(authorization);
        Long adminId = adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminOrderService.markCompleted(orderNo, body, adminId));
    }
}
