package com.ilgiyebo.domain.review.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.common.config.AiServerProperties;
import com.ilgiyebo.domain.review.service.HttpReviewAiClient;
import com.ilgiyebo.domain.review.service.ReviewAiClient;
import com.ilgiyebo.domain.review.service.StubReviewAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ReviewAiClient 빈 설정.
 * AI 서버 URL이 설정되어 있으면 HTTP 클라이언트를, 아니면 스텁을 사용한다.
 */
@Slf4j
@Configuration
public class ReviewAiClientConfig {

    @Bean
    public ReviewAiClient reviewAiClient(AiServerProperties aiServerProperties, ObjectMapper objectMapper) {
        if (aiServerProperties.isConfigured()) {
            log.info("AI 서버 회고 클라이언트 활성화: baseUrl={}", aiServerProperties.getBaseUrl());
            return new HttpReviewAiClient(aiServerProperties, objectMapper);
        }

        log.info("AI 서버 URL 미설정 - 스텁 회고 클라이언트 사용");
        return new StubReviewAiClient();
    }
}
