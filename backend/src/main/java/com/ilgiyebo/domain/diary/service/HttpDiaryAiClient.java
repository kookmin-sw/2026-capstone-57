package com.ilgiyebo.domain.diary.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.common.config.AiServerProperties;
import com.ilgiyebo.domain.diary.entity.EmotionTag;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * AI 서버의 일기 관련 HTTP 엔드포인트를 호출하는 클라이언트 구현체.
 *
 * <p>엔드포인트:
 * <ul>
 *   <li>POST /api/diary/first-question - 첫 번째 질문 생성</li>
 *   <li>POST /api/diary/next-question - 다음 질문 생성</li>
 *   <li>POST /api/diary/generate - 일기 내용 생성</li>
 * </ul>
 *
 * <p>타임아웃 시 최대 3회 재시도한다.
 */
@Slf4j
@RequiredArgsConstructor
public class HttpDiaryAiClient implements DiaryAiClient {

    private final AiServerProperties aiServerProperties;
    private final ObjectMapper objectMapper;

    @Override
    public String generateFirstQuestion(FirstQuestionInput input) {
        String url = aiServerProperties.getBaseUrl() + "/api/diary/first-question";

        FirstQuestionRequest request = new FirstQuestionRequest(
                input.userId(),
                input.targetDate(),
                input.todaySchedule() != null
                        ? input.todaySchedule().stream()
                        .map(s -> new ScheduleItem(s.startTime(), s.endTime(), s.location(), s.activity()))
                        .toList()
                        : List.of(),
                input.previousDiaryContent()
        );

        FirstQuestionResponse response = executeWithRetry(url, request, FirstQuestionResponse.class);
        return response.question();
    }

    @Override
    public NextQuestionResult generateNextQuestion(NextQuestionInput input) {
        String url = aiServerProperties.getBaseUrl() + "/api/diary/next-question";

        NextQuestionRequest request = new NextQuestionRequest(
                input.userId(),
                input.targetDate(),
                input.conversationHistory() != null
                        ? input.conversationHistory().stream()
                        .map(t -> new ConversationItem(t.turnNumber(), t.question(), t.answer()))
                        .toList()
                        : List.of(),
                input.todaySchedule() != null
                        ? input.todaySchedule().stream()
                        .map(s -> new ScheduleItem(s.startTime(), s.endTime(), s.location(), s.activity()))
                        .toList()
                        : List.of()
        );

        NextQuestionResponse response = executeWithRetry(url, request, NextQuestionResponse.class);
        return new NextQuestionResult(
                response.isConversationComplete(),
                response.nextQuestion(),
                response.completionReason()
        );
    }

    @Override
    public GeneratedDiaryResult generateDiaryContent(DiaryContentInput input) {
        String url = aiServerProperties.getBaseUrl() + "/api/diary/generate";

        DiaryGenerateRequest request = new DiaryGenerateRequest(
                input.userId(),
                input.targetDate(),
                input.conversationHistory() != null
                        ? input.conversationHistory().stream()
                        .map(t -> new ConversationItem(t.turnNumber(), t.question(), t.answer()))
                        .toList()
                        : List.of(),
                input.todaySchedule() != null
                        ? input.todaySchedule().stream()
                        .map(s -> new ScheduleItem(s.startTime(), s.endTime(), s.location(), s.activity()))
                        .toList()
                        : List.of()
        );

        DiaryGenerateResponse response = executeWithRetry(url, request, DiaryGenerateResponse.class);

        EmotionTag suggestedEmotion = null;
        if (response.suggestedEmotion() != null) {
            try {
                suggestedEmotion = EmotionTag.valueOf(response.suggestedEmotion().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("AI 서버에서 알 수 없는 감정 태그 반환: {}", response.suggestedEmotion());
            }
        }

        return new GeneratedDiaryResult(response.generatedContent(), suggestedEmotion);
    }

    // --- Private helpers ---

    private <T> T executeWithRetry(String url, Object requestBody, Class<T> responseType) {
        int maxRetries = aiServerProperties.getMaxRetries();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String jsonBody = objectMapper.writeValueAsString(requestBody);

                log.debug("AI 서버 요청: url={}, attempt={}/{}", url, attempt, maxRetries);

                HttpResponse<String> response = Unirest.post(url)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .body(jsonBody)
                        .asString();

                if (!response.isSuccess()) {
                    String errorBody = response.getBody();
                    log.warn("AI 서버 응답 오류: url={}, status={}, body={}, attempt={}/{}",
                            url, response.getStatus(), errorBody, attempt, maxRetries);

                    if (attempt < maxRetries) {
                        lastException = new AiServerException(
                                "AI 서버 응답 오류: status=" + response.getStatus());
                        continue;
                    }
                    throw new AiServerException(
                            "AI 서버 요청 실패 (최대 재시도 초과): status=" + response.getStatus()
                                    + ", body=" + errorBody);
                }

                T result = objectMapper.readValue(response.getBody(), responseType);
                log.debug("AI 서버 응답 성공: url={}", url);
                return result;

            } catch (AiServerException e) {
                lastException = e;
            } catch (Exception e) {
                lastException = e;
                log.warn("AI 서버 통신 오류: url={}, attempt={}/{}, error={}",
                        url, attempt, maxRetries, e.getMessage());

                if (attempt >= maxRetries) {
                    throw new AiServerException(
                            "AI 서버 요청 실패 (최대 재시도 초과): " + e.getMessage(), e);
                }
            }
        }

        throw new AiServerException("AI 서버 요청 실패 (최대 재시도 초과)", lastException);
    }

    // --- Request/Response DTOs for AI server communication ---

    private record FirstQuestionRequest(
            @JsonProperty("userId") String userId,
            @JsonProperty("date") String date,
            @JsonProperty("todaySchedule") List<ScheduleItem> todaySchedule,
            @JsonProperty("previousDiaryContent") String previousDiaryContent
    ) {}

    private record NextQuestionRequest(
            @JsonProperty("userId") String userId,
            @JsonProperty("date") String date,
            @JsonProperty("conversationHistory") List<ConversationItem> conversationHistory,
            @JsonProperty("todaySchedule") List<ScheduleItem> todaySchedule
    ) {}

    private record DiaryGenerateRequest(
            @JsonProperty("userId") String userId,
            @JsonProperty("date") String date,
            @JsonProperty("conversationHistory") List<ConversationItem> conversationHistory,
            @JsonProperty("todaySchedule") List<ScheduleItem> todaySchedule
    ) {}

    private record ScheduleItem(
            @JsonProperty("startTime") String startTime,
            @JsonProperty("endTime") String endTime,
            @JsonProperty("location") String location,
            @JsonProperty("activity") String activity
    ) {}

    private record ConversationItem(
            @JsonProperty("turnNumber") int turnNumber,
            @JsonProperty("question") String question,
            @JsonProperty("answer") String answer
    ) {}

    private record FirstQuestionResponse(
            @JsonProperty("question") String question
    ) {}

    private record NextQuestionResponse(
            @JsonProperty("isConversationComplete") boolean isConversationComplete,
            @JsonProperty("nextQuestion") String nextQuestion,
            @JsonProperty("completionReason") String completionReason
    ) {}

    private record DiaryGenerateResponse(
            @JsonProperty("generatedContent") String generatedContent,
            @JsonProperty("suggestedEmotion") String suggestedEmotion
    ) {}

    // --- Exception ---

    public static class AiServerException extends RuntimeException {
        public AiServerException(String message) {
            super(message);
        }

        public AiServerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
