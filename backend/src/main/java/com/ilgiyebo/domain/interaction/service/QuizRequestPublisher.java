package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage.TargetProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class QuizRequestPublisher {

    @Value("${cloud.aws.sqs.quiz-request-queue}")
    private String quizRequestQueue;

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    public QuizRequestPublisher(@Autowired(required=false) SqsTemplate sqsTemplate,
                                ObjectMapper objectMapper) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
    }

    public void requestQuizGeneration(UUID matchId, UUID requesterId, UUID targetUserId, TargetProfile targetProfile) {
        QuizGenerateRequestMessage message = QuizGenerateRequestMessage.of(
                matchId, requesterId, targetUserId, targetProfile);

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("퀴즈 생성 요청 직렬화 실패: 매칭ID={}", matchId, e);
            throw new RuntimeException("퀴즈 생성 요청 직렬화 실패", e);
        }

        if (sqsTemplate == null) {
            log.warn("SqsTemplate이 없습니다 (로컬 환경). 퀴즈 생성 요청을 건너뜁니다: 매칭ID={}, 대상유저ID={}", matchId, targetUserId);
            return;
        }

        try {
            sqsTemplate.send(quizRequestQueue, json);
            log.info("AI 퀴즈 생성 요청 SQS 발행 성공: 매칭ID={}, 대상유저ID={}", matchId, targetUserId);
        } catch (Exception e) {
            log.error("퀴즈 생성 요청 SQS 발행 실패: 매칭ID={}", matchId, e);
            throw new RuntimeException("퀴즈 생성 요청 SQS 발행 실패", e);
        }
    }
}