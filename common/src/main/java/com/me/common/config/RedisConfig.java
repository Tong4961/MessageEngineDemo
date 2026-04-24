package com.me.common.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @ClassName RedisConfig
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/24 11:39
 * @Version 1.0
 */
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        Jackson2JsonRedisSerializer<Object> jacksonSerializer = new Jackson2JsonRedisSerializer<>(Object.class); // 使用 Jackson2JsonRedisSerializer 序列化 value
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);// 允许序列化对象的私有字段
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);// 存储类型信息（反序列化时需要）
        objectMapper.registerModule(new JavaTimeModule());// 支持 LocalDateTime 等 Java 8 时间类型
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);// 禁用将日期写为时间戳
        jacksonSerializer.setObjectMapper(objectMapper);
        // 配置序列化规则
        // key 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // value 使用 JSON 序列化
        template.setValueSerializer(jacksonSerializer);
        template.setHashValueSerializer(jacksonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
