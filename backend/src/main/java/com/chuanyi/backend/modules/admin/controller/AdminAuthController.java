package com.chuanyi.backend.modules.admin.controller;

import com.chuanyi.backend.common.api.ApiResponse;
import com.chuanyi.backend.common.web.AuthHeaderUtil;
import com.chuanyi.backend.modules.admin.service.AdminAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody(required = false) Map<String, Object> body) {
        return ApiResponse.ok(adminAuthService.login(body));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        String token = AuthHeaderUtil.extractToken(request.getHeader("Authorization"));
        return ApiResponse.ok(adminAuthService.me(token));
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(HttpServletRequest request) {
        String token = AuthHeaderUtil.extractToken(request.getHeader("Authorization"));
        return ApiResponse.ok(adminAuthService.logout(token));
    }
}
