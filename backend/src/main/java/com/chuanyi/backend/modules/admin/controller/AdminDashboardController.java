package com.chuanyi.backend.modules.admin.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.web.AuthHeaderUtil;
import com.chuanyi.backend.modules.admin.service.AdminAuthService;
import com.chuanyi.backend.modules.admin.service.AdminDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminAuthService adminAuthService;
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminAuthService adminAuthService,
                                    AdminDashboardService adminDashboardService) {
        this.adminAuthService = adminAuthService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = AuthHeaderUtil.extractToken(authorization);
        adminAuthService.requireAdminId(token);
        return ApiResponse.ok(adminDashboardService.overview());
    }
}
