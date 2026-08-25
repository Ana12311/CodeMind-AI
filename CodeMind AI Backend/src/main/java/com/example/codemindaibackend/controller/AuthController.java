package com.example.codemindaibackend.controller;

import com.example.codemindaibackend.common.result.Result;
import com.example.codemindaibackend.dto.auth.LoginRequest;
import com.example.codemindaibackend.dto.auth.RefreshTokenRequest;
import com.example.codemindaibackend.dto.auth.RegisterRequest;
import com.example.codemindaibackend.service.AuthService;
import com.example.codemindaibackend.vo.auth.LoginResponse;
import com.example.codemindaibackend.vo.auth.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口
 *
 * @author CodeMind
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refresh(request));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(resolveToken(request));
        return Result.success();
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 当前用户信息
     */
    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.getCurrentUser());
    }
}
