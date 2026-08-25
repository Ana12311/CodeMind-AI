package com.example.codemindaibackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 服务（FastAPI）配置，对应 application.properties 中 ai.service.*
 *
 * @author CodeMind
 */
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    /** AI 服务基础地址，如 http://localhost:8000 */
    private String url = "http://localhost:8000";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
