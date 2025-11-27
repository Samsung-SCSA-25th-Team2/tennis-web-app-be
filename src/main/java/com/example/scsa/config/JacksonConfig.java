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
 * 프론트엔드 규약: 2025-11-17T19:00:00Z 형식 (UTC 타임존 표시 포함)
 */
@Configuration
public class JacksonConfig {

    /**
     * 역직렬화용 DateTimeFormatter
     * Z(UTC 타임존 표시)가 있어도 없어도 모두 파싱 가능
     *
     * 지원 형식:
     * - 2025-11-17T19:00:00
     * - 2025-11-17T19:00:00Z
     */
    private static final DateTimeFormatter FLEXIBLE_DESERIALIZER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendPattern(".SSS")  // 밀리초 선택적
            .optionalEnd()
            .optionalStart()
            .appendPattern("X")     // Z 또는 타임존 오프셋 선택적
            .optionalEnd()
            .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)  // Z가 없으면 UTC로 간주
            .toFormatter();

    /**
     * 직렬화용 DateTimeFormatter
     * 항상 Z를 붙여서 응답 (프론트엔드 규약)
     * 예: 2025-11-17T19:00:00Z
     */
    private static final DateTimeFormatter SERIALIZER_WITH_Z = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendLiteral('Z')
            .toFormatter();

    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 직렬화: Z를 붙여서 응답
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(SERIALIZER_WITH_Z));

        // LocalDateTime 역직렬화: Z가 있든 없든 모두 파싱
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(FLEXIBLE_DESERIALIZER));

        return Jackson2ObjectMapperBuilder.json()
                .modules(javaTimeModule)
                // WRITE_DATES_AS_TIMESTAMPS를 비활성화하여 ISO 8601 문자열 형식 사용
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // 알 수 없는 속성이 있어도 무시 (프론트엔드 호환성)
                .featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}
