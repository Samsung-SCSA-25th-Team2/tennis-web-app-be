package com.example.scsa.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Jackson 직렬화/역직렬화 설정
 *
 * ISO 8601 형식으로 LocalDateTime을 직렬화/역직렬화합니다.
 * 프론트엔드 규약: 2025-11-17T19:00:00 형식 (Z 없이, UTC+9 한국 시간대)
 */
@Configuration
public class JacksonConfig {

    /**
     * 직렬화/역직렬화용 DateTimeFormatter
     * ISO 8601 형식 (Z 없이, 한국 시간대)
     *
     * 지원 형식: 2025-11-17T19:00:00
     */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 직렬화: Z 없이 응답
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(ISO_FORMATTER));

        // LocalDateTime 역직렬화: ISO 8601 형식 파싱
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(ISO_FORMATTER));

        return Jackson2ObjectMapperBuilder.json()
                .modules(javaTimeModule)
                // WRITE_DATES_AS_TIMESTAMPS를 비활성화하여 ISO 8601 문자열 형식 사용
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 알 수 없는 속성이 있어도 무시 (프론트엔드 호환성)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
