package com.example.codemindaibackend.security;

import com.example.codemindaibackend.common.result.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 内部服务鉴权过滤器：HMAC-SHA256 签名 + 时间戳防重放
 *
 * <p>FastAPI 侧签名规则（与后端一致）：
 * <pre>
 * canonical = METHOD + "\n" + (requestURI + query) + "\n" + timestamp + "\n" + body
 * signature = lowercaseHex(HmacSHA256(internal.secret, canonical))
 * </pre>
 * 请求头：{@code X-Timestamp}（epoch 毫秒）、{@code X-Signature}（hex 小写）。</p>
 *
 * @author CodeMind
 */
@Component
public class InternalAuthFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final AntPathMatcher ANT_MATCHER = new AntPathMatcher();

    /** 时间戳窗口：5 分钟，超出即拒绝，防重放 */
    private static final long MAX_CLOCK_SKEW_MS = 300_000L;

    private final byte[] secret;

    public InternalAuthFilter(@Value("${internal.secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isInternalEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 读取请求体，并包装以便下游（@RequestBody）再次读取
        byte[] body = request.getInputStream().readAllBytes();
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, body);

        String timestamp = request.getHeader("X-Timestamp");
        String signature = request.getHeader("X-Signature");
        if (!verify(cachedRequest, body, timestamp, signature)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.failure(403, "内部服务鉴权失败")));
            return;
        }
        filterChain.doFilter(cachedRequest, response);
    }

    private boolean verify(HttpServletRequest request, byte[] body, String timestamp, String signature) {
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(signature)) {
            return false;
        }
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(System.currentTimeMillis() - ts) > MAX_CLOCK_SKEW_MS) {
            return false;
        }

        String pathWithQuery = request.getRequestURI();
        if (StringUtils.hasText(request.getQueryString())) {
            pathWithQuery += "?" + request.getQueryString();
        }
        String canonical = request.getMethod() + "\n" + pathWithQuery + "\n" + timestamp + "\n"
                + new String(body, StandardCharsets.UTF_8);
        String expected = hmacSha256Hex(canonical);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256Hex(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    private boolean isInternalEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return ("GET".equals(method) && ANT_MATCHER.match("/api/v1/ai-tasks/pending", uri))
                || ("POST".equals(method) && ANT_MATCHER.match("/api/v1/ai-reviews", uri))
                || ("PUT".equals(method) && ANT_MATCHER.match("/api/v1/ai-tasks/*/status", uri))
                || ("PUT".equals(method) && ANT_MATCHER.match("/api/v1/ai-tasks/*/result", uri))
                || ("POST".equals(method) && ANT_MATCHER.match("/api/ai/task/callback", uri));
    }
}
