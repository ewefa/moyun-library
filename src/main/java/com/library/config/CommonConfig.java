package com.library.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 密码加密器与 JSON 时间格式。
 */
@Configuration
public class CommonConfig {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt 自带随机盐，同一明文每次加密结果都不同
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DATE_TIME))
                .deserializers(new LocalDateTimeDeserializer(DATE_TIME));
    }
}
