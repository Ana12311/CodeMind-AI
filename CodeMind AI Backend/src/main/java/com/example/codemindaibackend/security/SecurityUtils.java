package com.example.codemindaibackend.security;

import com.example.codemindaibackend.common.exception.BusinessException;
import com.example.codemindaibackend.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具类
 *
 * @author CodeMind
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未登录返回 null
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) authentication.getPrincipal();
    }

    /**
     * 是否已认证
     */
    public static boolean isAuthenticated() {
        return getLoginUser() != null;
    }

    /**
     * 获取当前用户 ID，未登录抛异常
     */
    public static Long getCurrentUserId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return loginUser.getUserId();
    }

    /**
     * 是否管理员
     */
    public static boolean isAdmin() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            return false;
        }
        return loginUser.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
