package com.example.codemindaibackend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：解析请求头 Bearer Token，校验黑名单后写入 SecurityContext
 *
 * @author CodeMind
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    private final UserDetailsServiceImpl userDetailsService;

    private final StringRedisTemplate stringRedisTemplate;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   UserDetailsServiceImpl userDetailsService,
                                   StringRedisTemplate stringRedisTemplate) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && !isBlacklisted(token) && tokenProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = tokenProvider.getUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 令牌有效但用户已不存在/禁用等，跳过认证，走 401 入口
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * 已登出的 Access Token 命中黑名单则拒绝
     */
    private boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(JwtTokenProvider.BLACKLIST_KEY_PREFIX + token));
    }
}
