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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 실제 AI 서버와 HTTP 통신하는 회고 AI 클라이언트.
 * AI 서버 엔드포인트:
 *   POST /api/retro/first-question
 *   POST /api/retro/next-question
 *   POST /api/retro/generate
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
    public FirstQuestionResponse getFirstQuestion(UUID userId, MeetingInfo meetingInfo) {
        String url = aiServerProperties.getBaseUrl() + "/api/retro/first-question";

        AiFirstQuestionRequest request = new AiFirstQuestionRequest(
                userId.toString(),
                new AiMeetingInfo(
                        meetingInfo.matchedUserName(),
                        meetingInfo.meetingDate(),
                        meetingInfo.meetingPlace(),
                        meetingInfo.missionActivity()
                )
        );

        AiFirstQuestionResponse response = executeWithRetry(url, request, AiFirstQuestionResponse.class);
        log.info("AI 첫 번째 회고 질문 수신: maxTurns={}", response.maxTurns());
        return new FirstQuestionResponse(response.question(), response.maxTurns());
    }

    @Override
    public NextQuestionResponse getNextQuestion(UUID userId, MeetingInfo meetingInfo, List<ConversationTurn> conversationHistory) {
        String url = aiServerProperties.getBaseUrl() + "/api/retro/next-question";

        List<AiConversationTurn> aiHistory = conversationHistory.stream()
                .map(t -> new AiConversationTurn(t.turnNumber(), t.question(), t.answer()))
                .toList();

        AiNextQuestionRequest request = new AiNextQuestionRequest(
                userId.toString(),
                new AiMeetingInfo(
                        meetingInfo.matchedUserName(),
                        meetingInfo.meetingDate(),
                        meetingInfo.meetingPlace(),
                        meetingInfo.missionActivity()
                ),
                aiHistory
        );

        AiNextQuestionResponse response = executeWithRetry(url, request, AiNextQuestionResponse.class);
        log.info("AI 다음 회고 질문 수신: currentTurn={}, isComplete={}", response.currentTurn(), response.isConversationComplete());
        return new NextQuestionResponse(response.question(), response.isConversationComplete(), response.currentTurn(), response.maxTurns());
    }

    @Override
    public GenerateResponse generateReview(UUID userId, MeetingInfo meetingInfo, List<ConversationTurn> conversationHistory) {
        String url = aiServerProperties.getBaseUrl() + "/api/retro/generate";

        List<AiConversationTurn> aiHistory = conversationHistory.stream()
                .map(t -> new AiConversationTurn(t.turnNumber(), t.question(), t.answer()))
                .toList();

        AiGenerateRequest request = new AiGenerateRequest(
                userId.toString(),
                new AiMeetingInfo(
                        meetingInfo.matchedUserName(),
                        meetingInfo.meetingDate(),
                        meetingInfo.meetingPlace(),
                        meetingInfo.missionActivity()
                ),
                aiHistory
        );

        AiGenerateResponse response = executeWithRetry(url, request, AiGenerateResponse.class);
        log.info("AI 회고 글 생성 완료: 글자 수={}", response.compiledContent().length());
        return new GenerateResponse(response.compiledContent(), response.generatedAt());
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

        throw new RuntimeException("AI 서버 요청 실패: 재시도 횟수 설정 오류");
    }

    // --- AI Server Request/Response DTOs ---

    private record AiMeetingInfo(
            @JsonProperty("matchedUserName") String matchedUserName,
            @JsonProperty("meetingDate") String meetingDate,
            @JsonProperty("meetingPlace") String meetingPlace,
            @JsonProperty("missionActivity") String missionActivity
    ) {}

    private record AiConversationTurn(
            @JsonProperty("turnNumber") int turnNumber,
            @JsonProperty("question") String question,
            @JsonProperty("answer") String answer
    ) {}

    private record AiFirstQuestionRequest(
            @JsonProperty("userId") String userId,
            @JsonProperty("meetingInfo") AiMeetingInfo meetingInfo
    ) {}

    private record AiFirstQuestionResponse(
            @JsonProperty("question") String question,
            @JsonProperty("maxTurns") int maxTurns
    ) {}

    private record AiNextQuestionRequest(
            @JsonProperty("userId") String userId,
            @JsonProperty("meetingInfo") AiMeetingInfo meetingInfo,
            @JsonProperty("conversationHistory") List<AiConversationTurn> conversationHistory
    ) {}

    private record AiNextQuestionResponse(
            @JsonProperty("question") String question,
            @JsonProperty("isConversationComplete") boolean isConversationComplete,
            @JsonProperty("currentTurn") int currentTurn,
            @JsonProperty("maxTurns") int maxTurns
    ) {}

    private record AiGenerateRequest(
            @JsonProperty("userId") String userId,
            @JsonProperty("meetingInfo") AiMeetingInfo meetingInfo,
            @JsonProperty("conversationHistory") List<AiConversationTurn> conversationHistory
    ) {}

    private record AiGenerateResponse(
            @JsonProperty("compiledContent") String compiledContent,
            @JsonProperty("generatedAt") Instant generatedAt
    ) {}
}
