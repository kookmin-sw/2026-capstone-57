package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.TerminationReason;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * SQS 방식으로 알림을 발행하는 구현체.
 * Spring AMQP(RabbitMQ)를 Amazon SQS 호환 브로커로 사용하거나,
 * 추후 AWS SDK SQS Client로 교체 가능하도록 추상화한다.
 * 현재는 Spring AMQP의 RabbitTemplate을 통해 메시지를 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SqsInteractionNotificationPublisher implements InteractionNotificationPublisher {

    private static final String EXCHANGE = "interaction.notifications";
    private static final String ROUTING_KEY_STAGE = "interaction.stage.completed";
    private static final String ROUTING_KEY_TERMINATED = "interaction.match.terminated";
    private static final String ROUTING_KEY_HINT_QUESTION = "interaction.hint.question.received";
    private static final String ROUTING_KEY_HINT_ANSWER = "interaction.hint.answer.received";

    private final RabbitTemplate rabbitTemplate;
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
        publish(EXCHANGE, ROUTING_KEY_STAGE, message);
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
        publish(EXCHANGE, ROUTING_KEY_TERMINATED, message);
    }

    @Override
    public void publishHintQuestionReceived(UUID responderId, UUID matchId, UUID questionId) {
        Map<String, Object> message = Map.of(
            "type", "HINT_QUESTION_RECEIVED",
            "responderId", responderId.toString(),
            "matchId", matchId.toString(),
            "questionId", questionId.toString()
        );
        publish(EXCHANGE, ROUTING_KEY_HINT_QUESTION, message);
    }

    @Override
    public void publishHintAnswerReceived(UUID senderId, UUID matchId, UUID questionId) {
        Map<String, Object> message = Map.of(
            "type", "HINT_ANSWER_RECEIVED",
            "senderId", senderId.toString(),
            "matchId", matchId.toString(),
            "questionId", questionId.toString()
        );
        publish(EXCHANGE, ROUTING_KEY_HINT_ANSWER, message);
    }

    private void publish(String exchange, String routingKey, Map<String, Object> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(exchange, routingKey, json);
            log.debug("알림 발행 완료: exchange={}, routingKey={}, message={}", exchange, routingKey, json);
        } catch (JsonProcessingException e) {
            log.error("알림 메시지 직렬화 실패: {}", message, e);
        } catch (Exception e) {
            log.error("알림 발행 실패: exchange={}, routingKey={}", exchange, routingKey, e);
        }
    }
}
