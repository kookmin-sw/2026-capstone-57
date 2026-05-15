package com.ilgiyebo.domain.mission.service;

import com.ilgiyebo.domain.mission.dto.MissionGenerateRequestMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 미션 생성 요청을 SQS 큐에 발행하는 퍼블리셔.
 */
@Slf4j
@Component
public class MissionRequestPublisher {

    @Value("${cloud.aws.sqs.mission-request-queue:}")
    private String missionRequestQueue;

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    public MissionRequestPublisher(@Autowired(required = false) SqsTemplate sqsTemplate,
                                   ObjectMapper objectMapper) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * AI 서버에 미션 생성을 요청하는 SQS 메시지를 발행한다.
     */
    public void requestMissionGeneration(MissionGenerateRequestMessage message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            log.error("미션 생성 요청 직렬화 실패: 매칭ID={}", message.matchId(), e);
            throw new RuntimeException("미션 생성 요청 직렬화 실패", e);
        }

        if (sqsTemplate == null) {
            log.warn("SqsTemplate이 없습니다 (로컬 환경). 미션 생성 요청을 건너뜁니다: 매칭ID={}", message.matchId());
            return;
        }

        if (missionRequestQueue == null || missionRequestQueue.isBlank()) {
            log.warn("미션 요청 큐가 설정되지 않았습니다. 미션 생성 요청을 건너뜁니다: 매칭ID={}", message.matchId());
            return;
        }

        try {
            sqsTemplate.send(missionRequestQueue, json);
            log.info("AI 미션 생성 요청 SQS 발행 성공: 매칭ID={}, timeSlot={}", message.matchId(), message.timeSlot());
        } catch (Exception e) {
            log.error("미션 생성 요청 SQS 발행 실패: 매칭ID={}", message.matchId(), e);
            throw new RuntimeException("미션 생성 요청 SQS 발행 실패", e);
        }
    }
}
