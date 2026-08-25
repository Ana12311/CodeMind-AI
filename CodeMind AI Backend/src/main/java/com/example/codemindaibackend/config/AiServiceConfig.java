package com.example.codemindaibackend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * AI 服务 WebClient 配置。
 * 仅构建 HTTP 客户端，不启动任何服务；AI 服务不可用不影响本服务启动。
 *
 * @author CodeMind
 */
@Configuration
@EnableConfigurationProperties(AiServiceProperties.class)
public class AiServiceConfig {

    @Bean
    public WebClient aiServiceWebClient(AiServiceProperties properties) {
        return WebClient.builder().baseUrl(properties.getUrl()).build();
    }
}
