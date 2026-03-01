package com.chuanyi.backend.common.web;

import com.chuanyi.backend.common.context.UserContext;
import com.chuanyi.backend.common.exception.BizException;
import com.chuanyi.backend.modules.auth.service.AuthTokenService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthTokenInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION = "Authorization";

    private final AuthTokenService authTokenService;

    public AuthTokenInterceptor(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        UserContext.clear();
        String authHeader = request.getHeader(AUTHORIZATION);
        if (!StringUtils.hasText(authHeader)) {
            UserContext.bindUserId(1L);
            return true;
        }
        String token = AuthHeaderUtil.extractToken(authHeader);
        if (!StringUtils.hasText(token)) {
            throw new BizException(401, "invalid Authorization header");
        }
        Long userId = authTokenService.resolveUserId(token);
        if (userId == null) {
            throw new BizException(401, "invalid or expired token");
        }
        UserContext.bindUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        UserContext.clear();
    }
}
