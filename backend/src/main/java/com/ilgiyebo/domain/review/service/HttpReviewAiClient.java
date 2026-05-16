package com.ilgiyebo.domain.review.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.common.config.AiServerProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 실제 AI 서버와 HTTP 통신하는 회고 AI 클라이언트.
 * diary의 HttpDiaryAiClient와 동일한 패턴 (재시도, 타임아웃).
 */
@Slf4j
public class HttpReviewAiClient implements ReviewAiClient {

    private final AiServerProperties aiServerProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpReviewAiClient(AiServerProperties aiServerProperties, ObjectMapper objectMapper) {
        this.aiServerProperties = aiServerProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(aiServerProperties.getTimeoutMs()))
                .build();
    }

    @Override
    public List<String> generateReviewQuestions(String missionDescription, String missionLocation) {
        String url = aiServerProperties.getBaseUrl() + "/api/review/questions";

        ReviewQuestionsRequest request = new ReviewQuestionsRequest(missionDescription, missionLocation);
        ReviewQuestionsResponse response = executeWithRetry(url, request, ReviewQuestionsResponse.class);

        log.info("AI 회고 질문 생성 완료: 질문 수={}", response.questions().size());
        return response.questions();
    }

    @Override
    public String generateReviewContent(List<QuestionAnswer> answers) {
        String url = aiServerProperties.getBaseUrl() + "/api/review/generate";

        List<QaItem> qaItems = answers.stream()
                .map(qa -> new QaItem(qa.question(), qa.answer()))
                .toList();
        ReviewGenerateRequest request = new ReviewGenerateRequest(qaItems);
        ReviewGenerateResponse response = executeWithRetry(url, request, ReviewGenerateResponse.class);

        log.info("AI 회고 글 생성 완료: 글자 수={}", response.generatedContent().length());
        return response.generatedContent();
    }

    // --- Private helpers ---

    private <T> T executeWithRetry(String url, Object requestBody, Class<T> responseType) {
        int maxRetries = aiServerProperties.getMaxRetries();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

                log.debug("AI 서버 요청: url={}, attempt={}/{}", url, attempt, maxRetries);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofMillis(aiServerProperties.getTimeoutMs()))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 400 && response.statusCode() < 500) {
                    // 4xx 클라이언트 오류는 재시도 없이 즉시 예외
                    throw new RuntimeException("AI 서버 클라이언트 오류 (재시도 불가): status=" + response.statusCode());
                }

                if (response.statusCode() >= 500) {
                    log.warn("AI 서버 응답 오류: url={}, status={}, attempt={}/{}",
                            url, response.statusCode(), attempt, maxRetries);
                    if (attempt == maxRetries) {
                        throw new RuntimeException("AI 서버 요청 실패 (최대 재시도 초과): status=" + response.statusCode());
                    }
                    continue;
                }

                return objectMapper.readValue(response.body(), responseType);

            } catch (RuntimeException e) {
                if (attempt == maxRetries) throw e;
            } catch (Exception e) {
                log.warn("AI 서버 통신 오류: url={}, attempt={}/{}, error={}", url, attempt, maxRetries, e.getMessage());
                if (attempt == maxRetries) {
                    throw new RuntimeException("AI 서버 요청 실패 (최대 재시도 초과): " + e.getMessage(), e);
                }
            }
        }

        // maxRetries가 0 이하인 경우에만 도달 (정상적으로는 도달 불가)
        throw new RuntimeException("AI 서버 요청 실패: 재시도 횟수 설정 오류");
    }

    // --- Request/Response DTOs ---

    private record ReviewQuestionsRequest(
            @JsonProperty("missionDescription") String missionDescription,
            @JsonProperty("missionLocation") String missionLocation
    ) {}

    private record ReviewQuestionsResponse(
            @JsonProperty("questions") List<String> questions
    ) {}

    private record ReviewGenerateRequest(
            @JsonProperty("answers") List<QaItem> answers
    ) {}

    private record QaItem(
            @JsonProperty("question") String question,
            @JsonProperty("answer") String answer
    ) {}

    private record ReviewGenerateResponse(
            @JsonProperty("generatedContent") String generatedContent
    ) {}
}
