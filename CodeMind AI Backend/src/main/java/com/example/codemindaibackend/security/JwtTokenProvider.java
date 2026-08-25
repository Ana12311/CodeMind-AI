package com.example.codemindaibackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与解析
 *
 * @author CodeMind
 */
@Component
public class JwtTokenProvider {

    public static final String BLACKLIST_KEY_PREFIX = "blacklist:token:";

    private final SecretKey key;

    private final long accessTokenExpiration;

    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, accessTokenExpiration);
    }

    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, refreshTokenExpiration);
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    public Long getUserId(String token) {
        Number userId = parse(token).get("userId", Number.class);
        return userId != null ? userId.longValue() : null;
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * 令牌剩余有效时间（毫秒），已过期返回 0
     */
    public long getRemainingMillis(String token) {
        return Math.max(0, parse(token).getExpiration().getTime() - System.currentTimeMillis());
    }

    private String generateToken(Long userId, String username, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(key)
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
