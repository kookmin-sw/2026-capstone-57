package com.ilgiyebo.domain.interaction.handler;

import com.ilgiyebo.domain.game.event.GameCompletedEvent;
import com.ilgiyebo.domain.interaction.service.InteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Handles GameCompletedEvent from the game domain.
 * Processes after the GameSession transaction is committed to ensure DB consistency.
 * When game-server is separated later, this listener will be replaced by an SQS/Kafka consumer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionGameEventHandler {

    private final InteractionService interactionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGameCompleted(GameCompletedEvent event) {
        try {
            if (event.cleared()) {
                interactionService.completeGame(event.matchId(), event.gameType());
                log.info("게임 완료 처리 성공: matchId={}, score={}, intimacyPoints={}",
                        event.matchId(), event.score(), event.intimacyPoints());
            } else {
                log.info("게임 실패 이벤트 수신: matchId={}, gameType={}",
                        event.matchId(), event.gameType());
                // MVP에서는 실패 시 별도 처리 없음. 추후 재도전 로직 추가 가능.
            }
        } catch (Exception e) {
            log.error("게임 완료 이벤트 처리 실패: matchId={}, error={}",
                    event.matchId(), e.getMessage(), e);
            // GameSession은 이미 저장됨. 추후 재처리 메커니즘(DLQ 등) 추가 가능.
        }
    }
}
