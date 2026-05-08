package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.TerminationReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsInteractionNotificationPublisher implements InteractionNotificationPublisher {

    @Value("${sqs.queue.stage:interaction-stage-queue}")
    private String stageQueue;

    @Value("${sqs.queue.match:interaction-match-queue}")
    private String matchQueue;

    @Value("${sqs.queue.hint.question:interaction-hint-question-queue}")
    private String hintQuestionQueue;

    @Value("${sqs.queue.hint.answer:interaction-hint-answer-queue}")
    private String hintAnswerQueue;

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishStageCompleted(UUID matchId, UUID userId, int completedStage, int nextStage) {
        Map<String, Object> message = Map.of(
                "type", "STAGE_COMPLETED",
                "matchId", matchId.toString(),
                "userId", userId.toString(),
                "completedStage", completedStage,
                "nextStage", nextStage
        );
        publish(stageQueue, message);
    }

    @Override
    public void publishMatchTerminated(UUID matchId, UUID userAId, UUID userBId, TerminationReason reason) {
        Map<String, Object> message = Map.of(
                "type", "MATCH_TERMINATED",
                "matchId", matchId.toString(),
                "userAId", userAId.toString(),
                "userBId", userBId.toString(),
                "reason", reason.name()
        );
        publish(matchQueue, message);
    }

    @Override
    public void publishHintQuestionReceived(UUID responderId, UUID matchId, UUID questionId) {
        Map<String, Object> message = Map.of(
                "type", "HINT_QUESTION_RECEIVED",
                "responderId", responderId.toString(),
                "matchId", matchId.toString(),
                "questionId", questionId.toString()
        );
        publish(hintQuestionQueue, message);
    }

    @Override
    public void publishHintAnswerReceived(UUID senderId, UUID matchId, UUID questionId) {
        Map<String, Object> message = Map.of(
                "type", "HINT_ANSWER_RECEIVED",
                "senderId", senderId.toString(),
                "matchId", matchId.toString(),
                "questionId", questionId.toString()
        );
        publish(hintAnswerQueue, message);
    }

    private void publish(String queueName, Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sqsTemplate.send(queueName, json);
            log.debug("SQS 알림 발행 성공: 큐={}", queueName);
        } catch (JsonProcessingException e) {
            log.error("알림 메시지 직렬화 실패: {}", message, e);
        } catch (Exception e) {
            log.error("SQS 알림 발행 실패: 큐={}", queueName, e);
        }
    }
}