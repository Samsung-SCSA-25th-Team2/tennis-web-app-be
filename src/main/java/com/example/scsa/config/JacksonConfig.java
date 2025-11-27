package com.example.scsa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 직렬화/역직렬화 설정
 *
 * ISO 8601 형식으로 LocalDateTime을 직렬화합니다.
 * 프론트엔드 요구사항: 2025-11-17T19:00:00Z 형식
 */
@Configuration
public class JacksonConfig {

    /**
     * ISO 8601 형식의 DateTimeFormatter
     * 예: 2025-11-17T19:00:00
     */
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime을 ISO 8601 형식으로 직렬화
        javaTimeModule.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(ISO_DATETIME_FORMATTER));

        return Jackson2ObjectMapperBuilder.json()
                .modules(javaTimeModule)
                // WRITE_DATES_AS_TIMESTAMPS를 비활성화하여 ISO 8601 문자열 형식 사용
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }
}
