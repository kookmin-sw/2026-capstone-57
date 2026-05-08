package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage.TargetProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizRequestPublisher {

    @Value("${cloud.aws.sqs.quiz-request-queue}")
    private String quizRequestQueue;

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    public void requestQuizGeneration(UUID matchId, UUID requesterId, UUID targetUserId, TargetProfile targetProfile) {
        QuizGenerateRequestMessage message = QuizGenerateRequestMessage.of(
                matchId, requesterId, targetUserId, targetProfile);

        try {
            String json = objectMapper.writeValueAsString(message);
            sqsTemplate.send(quizRequestQueue, json);
            log.info("AI 퀴즈 생성 요청 SQS 발행 성공: 매칭ID={}, 대상유저ID={}", matchId, targetUserId);
        } catch (JsonProcessingException e) {
            log.error("퀴즈 생성 요청 직렬화 실패: 매칭ID={}", matchId, e);
        } catch (Exception e) {
            log.error("퀴즈 생성 요청 SQS 발행 실패: 매칭ID={}", matchId, e);
        }
    }
}