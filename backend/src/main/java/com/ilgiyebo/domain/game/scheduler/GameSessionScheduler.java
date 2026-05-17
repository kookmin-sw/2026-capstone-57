package com.ilgiyebo.domain.game.scheduler;

import com.ilgiyebo.domain.game.engine.GameRoom;
import com.ilgiyebo.domain.game.entity.GameFailReason;
import com.ilgiyebo.domain.game.entity.GameSessionEntity;
import com.ilgiyebo.domain.game.entity.GameSessionStatus;
import com.ilgiyebo.domain.game.repository.GameSessionRepository;
import com.ilgiyebo.domain.game.service.GameRoomStore;
import com.ilgiyebo.domain.game.service.GameSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodic cleanup scheduler for game sessions and rooms.
 * Handles expired, stuck, and abandoned sessions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameSessionScheduler {

    private final GameSessionRepository gameSessionRepository;
    private final GameSessionService gameSessionService;
    private final GameRoomStore roomStore;

    /** Maximum game duration before forced timeout (10 minutes) */
    private static final int MAX_GAME_DURATION_MINUTES = 10;

    /** Maximum time a session can stay in WAITING before expiring (5 minutes) */
    private static final int WAITING_EXPIRY_MINUTES = 5;

    /**
     * Runs every 60 seconds to clean up abnormal sessions.
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
    public void cleanupSessions() {
        failStuckPlayingSessions();
        cleanupDisconnectedRooms();
        expireWaitingSessions();
    }

    /**
     * Task 1: Find PLAYING sessions exceeding max game duration → failGame(TIMEOUT)
     */
    private void failStuckPlayingSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(MAX_GAME_DURATION_MINUTES);
        List<GameSessionEntity> stuckSessions = gameSessionRepository
                .findByStatusAndCreatedAtBefore(GameSessionStatus.PLAYING, threshold);

        for (GameSessionEntity session : stuckSessions) {
            try {
                log.info("스케줄러: PLAYING 세션 강제 종료 (시간 초과): sessionId={}", session.getId());
                gameSessionService.failGame(session.getId(), GameFailReason.TIMEOUT, 0, "{}");
                roomStore.remove(session.getId());
            } catch (Exception e) {
                log.error("스케줄러: PLAYING 세션 정리 실패: sessionId={}, error={}",
                        session.getId(), e.getMessage());
            }
        }
    }

    /**
     * Task 2: Find GameRooms with all users disconnected → remove from store, failGame(DISCONNECTED)
     */
    private void cleanupDisconnectedRooms() {
        for (GameRoom room : roomStore.getActiveRooms()) {
            if (room.getConnectedUsers().isEmpty()) {
                try {
                    log.info("스케줄러: 모든 사용자 연결 해제된 방 정리: sessionId={}", room.getSessionId());
                    gameSessionService.failGame(room.getSessionId(), GameFailReason.DISCONNECTED, 0, "{}");
                    roomStore.remove(room.getSessionId());
                } catch (Exception e) {
                    log.error("스케줄러: 연결 해제 방 정리 실패: sessionId={}, error={}",
                            room.getSessionId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Task 3: Find WAITING sessions older than threshold → expireSession()
     */
    private void expireWaitingSessions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(WAITING_EXPIRY_MINUTES);
        List<GameSessionEntity> waitingSessions = gameSessionRepository
                .findByStatusAndCreatedAtBefore(GameSessionStatus.WAITING, threshold);

        for (GameSessionEntity session : waitingSessions) {
            try {
                log.info("스케줄러: WAITING 세션 만료 처리: sessionId={}", session.getId());
                gameSessionService.expireSession(session.getId());
                roomStore.remove(session.getId());
            } catch (Exception e) {
                log.error("스케줄러: WAITING 세션 만료 처리 실패: sessionId={}, error={}",
                        session.getId(), e.getMessage());
            }
        }
    }
}
