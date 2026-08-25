package com.example.codemindaibackend.service;

import com.example.codemindaibackend.dto.auth.LoginRequest;
import com.example.codemindaibackend.dto.auth.RefreshTokenRequest;
import com.example.codemindaibackend.dto.auth.RegisterRequest;
import com.example.codemindaibackend.vo.auth.LoginResponse;
import com.example.codemindaibackend.vo.auth.UserInfoVO;

/**
 * 认证业务接口
 *
 * @author CodeMind
 */
public interface AuthService {

    /**
     * 注册
     */
    void register(RegisterRequest request);

    /**
     * 登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 刷新令牌
     */
    LoginResponse refresh(RefreshTokenRequest request);

    /**
     * 登出
     *
     * @param accessToken 当前访问令牌，用于加入黑名单
     */
    void logout(String accessToken);

    /**
     * 当前用户信息
     */
    UserInfoVO getCurrentUser();
}
