package com.ilgiyebo.domain.ai.quiz.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.domain.ai.quiz.dto.QuizRequestMessage;
import com.ilgiyebo.domain.ai.quiz.dto.QuizResponseMessage;
import com.ilgiyebo.domain.ai.quiz.exception.InvalidQuizRequestException;
import com.ilgiyebo.domain.ai.quiz.service.AiQuizService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsQuizListener {

    private final AiQuizService aiQuizService;
    private final ObjectMapper objectMapper;
    private final SqsTemplate sqsTemplate;

    @Value("${aws.sqs.quiz-response-queue}")
    private String responseQueueName;

    @SqsListener("${aws.sqs.quiz-request-queue}")
    public void handleQuizRequest(String messageBody) {
        log.info("퀴즈 생성 요청 메시지 수신: {}", messageBody);

        QuizRequestMessage request;
        try {
            request = objectMapper.readValue(messageBody, QuizRequestMessage.class);
        } catch (Exception e) {
            log.error("메시지 역직렬화 실패: {}", e.getMessage());
            throw new InvalidQuizRequestException(List.of("message body (JSON 파싱 실패)"));
        }

        List<String> missingFields = validateRequiredFields(request);
        if (!missingFields.isEmpty()) {
            log.error("필수 필드 누락: {}", missingFields);
            throw new InvalidQuizRequestException(missingFields);
        }

        // Generate quiz
        QuizResponseMessage response = aiQuizService.generateQuiz(request);

        // Publish to response queue
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            sqsTemplate.send(responseQueueName, responseJson);
            log.info("퀴즈 생성 완료, 응답 큐 전송 - matchId: {}", request.matchId());
        } catch (Exception e) {
            log.error("응답 큐 전송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("응답 큐 전송 실패", e);
        }
    }

    private List<String> validateRequiredFields(QuizRequestMessage request) {
        List<String> missingFields = new ArrayList<>();
        if (request.matchId() == null) missingFields.add("matchId");
        if (request.requesterId() == null) missingFields.add("requesterId");
        if (request.targetUserId() == null) missingFields.add("targetUserId");
        if (request.targetProfile() == null) {
            missingFields.add("targetProfile");
        } else if (request.targetProfile().name() == null || request.targetProfile().name().isBlank()) {
            missingFields.add("targetProfile.name");
        }
        return missingFields;
    }
}
