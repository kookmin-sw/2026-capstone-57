package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.TerminationReason;

import java.util.UUID;

/**
 * 상호작용 관련 알림을 SQS로 발행하는 인터페이스.
 * 실제 SQS 전송은 구현체에서 처리한다.
 */
public interface InteractionNotificationPublisher {

    /** 단계 완료 알림 발행 */
    void publishStageCompleted(UUID matchId, UUID userId, int completedStage, int nextStage);

    /** 매칭 종료 알림 발행 */
    void publishMatchTerminated(UUID matchId, UUID userAId, UUID userBId, TerminationReason reason);

    /** 힌트 질문 수신 알림 발행 */
    void publishHintQuestionReceived(UUID responderId, UUID matchId, UUID questionId);

    /** 힌트 답변 수신 알림 발행 */
    void publishHintAnswerReceived(UUID senderId, UUID matchId, UUID questionId);
}
