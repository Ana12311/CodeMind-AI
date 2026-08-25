package com.example.codemindaibackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Redis 配置：JSON 序列化的 RedisTemplate
 *
 * <p>Spring Data Redis 4.x 提供 Jackson 3 序列化器
 * {@link GenericJacksonJsonRedisSerializer}（无 "2"），
 * 复用 Spring Boot 自动配置的 {@link JsonMapper}。</p>
 *
 * @author CodeMind
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, JsonMapper jsonMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJacksonJsonRedisSerializer jsonSerializer = new GenericJacksonJsonRedisSerializer(jsonMapper);

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
