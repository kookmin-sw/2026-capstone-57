package com.ilgiyebo.domain.game.service;

import com.ilgiyebo.domain.game.dto.response.GameSessionResponse;
import com.ilgiyebo.domain.game.engine.ScoreEngine;
import com.ilgiyebo.domain.game.entity.GameFailReason;
import com.ilgiyebo.domain.game.entity.GameSessionEntity;
import com.ilgiyebo.domain.game.entity.GameSessionStatus;
import com.ilgiyebo.domain.game.event.GameCompletedEvent;
import com.ilgiyebo.domain.game.exception.GameException;
import com.ilgiyebo.domain.game.repository.GameSessionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionServiceImpl implements GameSessionService {

    private static final String DEFAULT_GAME_TYPE = "COOP_SWITCH";

    private final GameSessionRepository gameSessionRepository;
    private final MatchRepository matchRepository;
    private final ScoreEngine scoreEngine;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public GameSessionResponse createSession(UUID matchId, UUID requesterId) {
        // 1. Validate Match exists
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(GameException.MATCH_NOT_FOUND::toException);

        // 2. Validate Match is ACTIVE
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw GameException.MATCH_NOT_ACTIVE.toException();
        }

        // 3. Validate requester is participant
        validateParticipant(match, requesterId);

        // 4. Idempotent: return existing WAITING or PLAYING session
        List<GameSessionStatus> activeStatuses = List.of(
                GameSessionStatus.WAITING, GameSessionStatus.PLAYING);
        var existingSession = gameSessionRepository.findByMatchIdAndStatusIn(matchId, activeStatuses);
        if (existingSession.isPresent()) {
            return GameSessionResponse.from(existingSession.get());
        }

        // 5. Create new session with WAITING status
        GameSessionEntity session = GameSessionEntity.builder()
                .matchId(matchId)
                .gameType(DEFAULT_GAME_TYPE)
                .status(GameSessionStatus.WAITING)
                .build();

        GameSessionEntity saved = gameSessionRepository.save(session);
        log.info("게임 세션 생성: sessionId={}, matchId={}", saved.getId(), matchId);

        return GameSessionResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GameSessionResponse getSession(UUID gameSessionId, UUID requesterId) {
        GameSessionEntity session = findById(gameSessionId);

        // Validate requester is participant via Match
        MatchEntity match = matchRepository.findById(session.getMatchId())
                .orElseThrow(GameException.MATCH_NOT_FOUND::toException);
        validateParticipant(match, requesterId);

        return GameSessionResponse.from(session);
    }

    @Override
    @Transactional
    public void startGame(UUID gameSessionId) {
        GameSessionEntity session = findById(gameSessionId);
        session.setStatus(GameSessionStatus.PLAYING);
        session.setStartedAt(Instant.now());
        gameSessionRepository.save(session);
        log.info("게임 시작: sessionId={}", gameSessionId);
    }

    @Override
    @Transactional
    public void completeGame(UUID gameSessionId, int score, long clearTimeMs, String finalStateJson) {
        GameSessionEntity session = findById(gameSessionId);
        int intimacyPoints = scoreEngine.calculateIntimacyPoints(score, clearTimeMs);

        session.setStatus(GameSessionStatus.COMPLETED);
        session.setFailReason(null);
        session.setScore(score);
        session.setClearTimeMs(clearTimeMs);
        session.setIntimacyPoints(intimacyPoints);
        session.setFinalState(finalStateJson);
        session.setCompletedAt(Instant.now());
        gameSessionRepository.save(session);

        log.info("게임 클리어: sessionId={}, score={}, intimacyPoints={}", gameSessionId, score, intimacyPoints);

        // Publish domain event (processed after transaction commit by @TransactionalEventListener)
        eventPublisher.publishEvent(new GameCompletedEvent(
                session.getMatchId(),
                gameSessionId,
                session.getGameType(),
                score,
                intimacyPoints,
                clearTimeMs,
                true
        ));
    }

    @Override
    @Transactional
    public void failGame(UUID gameSessionId, GameFailReason reason, int partialScore, String finalStateJson) {
        GameSessionEntity session = findById(gameSessionId);

        session.setStatus(GameSessionStatus.FAILED);
        session.setFailReason(reason);
        session.setScore(partialScore);
        session.setFinalState(finalStateJson);
        session.setCompletedAt(Instant.now());
        gameSessionRepository.save(session);

        log.info("게임 실패: sessionId={}, reason={}, partialScore={}", gameSessionId, reason, partialScore);

        // Publish domain event (cleared=false, no intimacy points)
        eventPublisher.publishEvent(new GameCompletedEvent(
                session.getMatchId(),
                gameSessionId,
                session.getGameType(),
                partialScore,
                0,
                0,
                false
        ));
    }

    @Override
    @Transactional
    public void expireSession(UUID gameSessionId) {
        GameSessionEntity session = findById(gameSessionId);
        session.setStatus(GameSessionStatus.EXPIRED);
        session.setCompletedAt(Instant.now());
        gameSessionRepository.save(session);
        log.info("게임 세션 만료: sessionId={}", gameSessionId);
    }

    private GameSessionEntity findById(UUID gameSessionId) {
        return gameSessionRepository.findById(gameSessionId)
                .orElseThrow(GameException.GAME_SESSION_NOT_FOUND::toException);
    }

    private void validateParticipant(MatchEntity match, UUID userId) {
        UUID userAId = match.getUserA().getId();
        UUID userBId = match.getUserB().getId();
        if (!userId.equals(userAId) && !userId.equals(userBId)) {
            throw GameException.NOT_GAME_PARTICIPANT.toException();
        }
    }
}
