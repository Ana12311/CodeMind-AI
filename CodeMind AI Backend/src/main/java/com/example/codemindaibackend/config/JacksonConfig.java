package com.example.codemindaibackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Jackson 序列化配置：Long 转 String，规避前端雪花 ID 精度丢失
 *
 * <p>Spring Boot 4 使用 Jackson 3（{@code tools.jackson} 包），
 * 通过 {@link SimpleModule}（{@code JacksonModule}）注册自定义序列化器。</p>
 *
 * @author CodeMind
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule longToStringModule() {
        SimpleModule module = new SimpleModule("long-to-string");
        module.addSerializer(Long.class, ToStringSerializer.instance);
        return module;
    }
}
