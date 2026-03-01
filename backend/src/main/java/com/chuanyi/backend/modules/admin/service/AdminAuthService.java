package com.chuanyi.backend.modules.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.admin.entity.AdminAccountEntity;
import com.chuanyi.backend.modules.admin.mapper.AdminAccountMapper;
import com.chuanyi.backend.modules.auth.service.AuthTokenService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdminAuthService {

    private static final long ADMIN_TOKEN_USER_ID_BASE = 900000000000L;
    private static final int EXPIRES_IN_SECONDS = 7200;

    private final AdminAccountMapper adminAccountMapper;
    private final AuthTokenService authTokenService;

    public AdminAuthService(AdminAccountMapper adminAccountMapper,
                            AuthTokenService authTokenService) {
        this.adminAccountMapper = adminAccountMapper;
        this.authTokenService = authTokenService;
    }

    public Map<String, Object> login(Map<String, Object> request) {
        Map<String, Object> body = request == null ? new HashMap<String, Object>() : request;
        String username = parseText(body.get("username"));
        String password = parseText(body.get("password"));
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException(400, "username and password are required");
        }

        AdminAccountEntity admin = adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccountEntity>()
                .eq(AdminAccountEntity::getUsername, username)
                .last("LIMIT 1"));
        if (admin == null || !password.equals(admin.getPassword())) {
            throw new BizException(401, "invalid username or password");
        }
        if (!"ENABLED".equalsIgnoreCase(admin.getStatus())) {
            throw new BizException(403, "admin account is disabled");
        }

        String token = authTokenService.issueToken(toTokenUserId(admin.getId()), EXPIRES_IN_SECONDS);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", EXPIRES_IN_SECONDS);
        data.put("admin", mapAdmin(admin));
        return data;
    }

    public Map<String, Object> me(String token) {
        AdminAccountEntity admin = requireAdminByToken(token);
        return mapAdmin(admin);
    }

    public Boolean logout(String token) {
        if (StringUtils.hasText(token)) {
            authTokenService.revokeToken(token);
        }
        return Boolean.TRUE;
    }

    public Long requireAdminId(String token) {
        return requireAdminByToken(token).getId();
    }

    private AdminAccountEntity requireAdminByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BizException(401, "admin token required");
        }
        Long tokenUserId = authTokenService.resolveUserId(token);
        if (tokenUserId == null) {
            throw new BizException(401, "invalid or expired admin token");
        }
        if (tokenUserId.longValue() < ADMIN_TOKEN_USER_ID_BASE) {
            throw new BizException(403, "admin token required");
        }
        Long adminId = tokenUserId.longValue() - ADMIN_TOKEN_USER_ID_BASE;
        AdminAccountEntity admin = adminAccountMapper.selectById(adminId);
        if (admin == null) {
            throw new BizException(401, "admin not found");
        }
        if (!"ENABLED".equalsIgnoreCase(admin.getStatus())) {
            throw new BizException(403, "admin account is disabled");
        }
        return admin;
    }

    private long toTokenUserId(Long adminId) {
        return ADMIN_TOKEN_USER_ID_BASE + (adminId == null ? 0L : adminId.longValue());
    }

    private Map<String, Object> mapAdmin(AdminAccountEntity admin) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("id", admin.getId());
        data.put("username", admin.getUsername());
        data.put("nickname", StringUtils.hasText(admin.getNickname()) ? admin.getNickname() : admin.getUsername());
        data.put("status", admin.getStatus());
        return data;
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
