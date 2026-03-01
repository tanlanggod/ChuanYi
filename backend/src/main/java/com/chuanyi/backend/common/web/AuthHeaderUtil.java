package com.chuanyi.backend.common.web;

import org.springframework.util.StringUtils;

public final class AuthHeaderUtil {

    private AuthHeaderUtil() {
    }

    public static String extractToken(String authHeader) {
        String value = authHeader == null ? "" : authHeader.trim();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() > 7 && "bearer ".equalsIgnoreCase(value.substring(0, 7))) {
            return value.substring(7).trim();
        }
        return value;
    }
}
