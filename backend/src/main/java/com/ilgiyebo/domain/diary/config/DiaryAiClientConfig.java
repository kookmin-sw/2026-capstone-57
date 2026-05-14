package com.ilgiyebo.domain.diary.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.common.config.AiServerProperties;
import com.ilgiyebo.domain.diary.service.DiaryAiClient;
import com.ilgiyebo.domain.diary.service.HttpDiaryAiClient;
import com.ilgiyebo.domain.diary.service.StubDiaryAiClient;
import kong.unirest.core.Unirest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * DiaryAiClient 빈 설정.
 * AI 서버 URL이 설정되어 있으면 HTTP 클라이언트를, 아니면 스텁을 사용한다.
 */
@Slf4j
@Configuration
public class DiaryAiClientConfig {

    @Bean
    @Primary
    public DiaryAiClient diaryAiClient(AiServerProperties aiServerProperties, ObjectMapper objectMapper) {
        if (aiServerProperties.isConfigured()) {
            log.info("AI 서버 일기 클라이언트 활성화: baseUrl={}", aiServerProperties.getBaseUrl());

            // Unirest 글로벌 타임아웃 설정
            Unirest.config()
                    .connectTimeout(aiServerProperties.getTimeoutMs());

            return new HttpDiaryAiClient(aiServerProperties, objectMapper);
        }

        log.info("AI 서버 URL 미설정 - 스텁 일기 클라이언트 사용");
        return new StubDiaryAiClient();
    }
}
