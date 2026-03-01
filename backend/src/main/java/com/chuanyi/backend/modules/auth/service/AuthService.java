package com.chuanyi.backend.modules.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.common.util.MockUtil;
import com.chuanyi.backend.modules.auth.entity.UserAccountEntity;
import com.chuanyi.backend.modules.auth.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final int EXPIRES_IN_SECONDS = 7200;

    private final UserAccountMapper userAccountMapper;
    private final AuthTokenService authTokenService;

    public AuthService(UserAccountMapper userAccountMapper,
                       AuthTokenService authTokenService) {
        this.userAccountMapper = userAccountMapper;
        this.authTokenService = authTokenService;
    }

    public Map<String, Object> login(Map<String, Object> request) {
        Map<String, Object> body = request == null ? new HashMap<String, Object>() : request;
        String loginType = parseText(body.get("loginType"));
        String phone = parseText(body.get("phone"));
        String normalizedType = StringUtils.hasText(loginType) ? loginType.trim().toUpperCase() : "GUEST";
        UserAccountEntity user;
        if ("PHONE".equals(normalizedType)) {
            user = loginByPhone(phone);
        } else if ("GUEST".equals(normalizedType)) {
            user = createGuestUser();
        } else {
            throw new BizException(400, "unsupported loginType");
        }

        String token = authTokenService.issueToken(user.getId(), EXPIRES_IN_SECONDS);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", EXPIRES_IN_SECONDS);
        data.put("userNo", user.getUserNo());
        data.put("isGuest", !"PHONE".equalsIgnoreCase(user.getLoginType()));
        return data;
    }

    public Map<String, Object> me() {
        Long userId = UserContext.currentUserId();
        UserAccountEntity user = userAccountMapper.selectById(userId);
        if (user == null) {
            Map<String, Object> fallback = new HashMap<String, Object>();
            fallback.put("userId", userId);
            fallback.put("userNo", "U" + userId);
            fallback.put("nickname", "Guest");
            fallback.put("avatarUrl", "");
            fallback.put("phone", null);
            fallback.put("loginType", "GUEST");
            fallback.put("status", "ENABLED");
            fallback.put("isGuest", true);
            return fallback;
        }
        return mapUser(user);
    }

    public Boolean logout(String token) {
        authTokenService.revokeToken(token);
        return Boolean.TRUE;
    }

    private UserAccountEntity loginByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BizException(400, "phone is required when loginType is PHONE");
        }
        UserAccountEntity user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getPhone, phone)
                .last("LIMIT 1"));
        if (user == null) {
            user = new UserAccountEntity();
            user.setUserNo(MockUtil.nextNo("U"));
            user.setNickname(maskPhone(phone));
            user.setPhone(phone);
            user.setLoginType("PHONE");
            user.setStatus("ENABLED");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userAccountMapper.insert(user);
            return user;
        }
        if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw new BizException(403, "user is disabled");
        }
        if (!StringUtils.hasText(user.getUserNo())) {
            user.setUserNo(MockUtil.nextNo("U"));
        }
        user.setLoginType("PHONE");
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(user);
        return user;
    }

    private UserAccountEntity createGuestUser() {
        UserAccountEntity user = new UserAccountEntity();
        user.setUserNo(MockUtil.nextNo("U"));
        String userNo = user.getUserNo();
        if (userNo.length() > 6) {
            user.setNickname("Guest" + userNo.substring(userNo.length() - 6));
        } else {
            user.setNickname("Guest");
        }
        user.setPhone(null);
        user.setLoginType("GUEST");
        user.setStatus("ENABLED");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.insert(user);
        return user;
    }

    private String maskPhone(String phone) {
        String text = phone == null ? "" : phone.trim();
        if (text.length() < 7) {
            return "User" + text;
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    private Map<String, Object> mapUser(UserAccountEntity user) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("userId", user.getId());
        data.put("userNo", user.getUserNo());
        data.put("nickname", user.getNickname());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("phone", user.getPhone());
        data.put("loginType", user.getLoginType());
        data.put("status", user.getStatus());
        data.put("isGuest", !"PHONE".equalsIgnoreCase(user.getLoginType()));
        return data;
    }

    private String parseText(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
