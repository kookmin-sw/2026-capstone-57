package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage.TargetProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AI에게 퀴즈 생성을 요청하는 SQS Producer.
 * 매칭 성사 시 또는 사용자가 최초 퀴즈 조회 시 호출된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizRequestPublisher {

    private static final String EXCHANGE = "ai.requests";
    private static final String ROUTING_KEY = "ai.quiz.generate";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * AI에게 퀴즈 생성 요청을 SQS로 발행한다.
     * DB에서 조회한 상대방 프로필 정보를 함께 전달한다.
     *
     * @param matchId       매칭 ID
     * @param requesterId   요청자 (퀴즈를 풀 사용자) ID
     * @param targetUserId  퀴즈 대상 (상대방) ID
     * @param targetProfile 상대방 프로필 정보
     */
    public void requestQuizGeneration(UUID matchId, UUID requesterId, UUID targetUserId, TargetProfile targetProfile) {
        QuizGenerateRequestMessage message = QuizGenerateRequestMessage.of(
            matchId, requesterId, targetUserId, targetProfile);

        try {
            String json = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, json);
            log.info("퀴즈 생성 요청 발행: matchId={}, targetUserId={}", matchId, targetUserId);
        } catch (JsonProcessingException e) {
            log.error("퀴즈 생성 요청 직렬화 실패: matchId={}", matchId, e);
        } catch (Exception e) {
            log.error("퀴즈 생성 요청 발행 실패: matchId={}", matchId, e);
        }
    }
}
