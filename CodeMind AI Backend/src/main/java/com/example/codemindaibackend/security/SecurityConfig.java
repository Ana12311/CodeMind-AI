package com.example.codemindaibackend.security;

import com.example.codemindaibackend.common.result.Result;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 配置
 *
 * @author CodeMind
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final InternalAuthFilter internalAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, InternalAuthFilter internalAuthFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.internalAuthFilter = internalAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/ai-tasks/pending").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/ai-tasks/*/status", "/api/v1/ai-tasks/*/result").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/ai-reviews").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/ai/task/callback").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "未认证或登录已过期"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN, 403, "无权限访问")))
                .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 阻止 JwtAuthenticationFilter 被 Servlet 容器重复注册（已在安全链中注册）
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 阻止 InternalAuthFilter 被 Servlet 容器重复注册
     */
    @Bean
    public FilterRegistrationBean<InternalAuthFilter> internalFilterRegistration(InternalAuthFilter filter) {
        FilterRegistrationBean<InternalAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private static void writeJson(HttpServletResponse response, int status, int code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.failure(code, message)));
    }
}
