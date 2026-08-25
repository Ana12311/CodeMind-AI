package com.example.codemindaibackend.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌请求
 *
 * @author CodeMind
 */
public class RefreshTokenRequest {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
