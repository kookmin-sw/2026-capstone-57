package com.ilgiyebo.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.server")
public class AiServerProperties {

    /**
     * AI 서버 기본 URL (예: http://localhost:8081)
     * 비어있으면 스텁 클라이언트를 사용한다.
     */
    private String baseUrl = "";

    /**
     * HTTP 요청 타임아웃 (밀리초) - 일반 요청 (first-question, next-question)
     */
    private int timeoutMs = 10000;

    /**
     * 일기 생성(generate) 요청 전용 타임아웃 (밀리초).
     * LLM 기반 생성은 시간이 오래 걸리므로 별도 분리.
     */
    private int generateTimeoutMs = 60000;

    /**
     * 최대 재시도 횟수
     */
    private int maxRetries = 3;

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
