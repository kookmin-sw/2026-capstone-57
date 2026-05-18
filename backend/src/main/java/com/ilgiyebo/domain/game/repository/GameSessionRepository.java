package com.ilgiyebo.domain.game.repository;

import com.ilgiyebo.domain.game.entity.GameSessionEntity;
import com.ilgiyebo.domain.game.entity.GameSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, UUID> {

    Optional<GameSessionEntity> findTopByMatchIdAndStatusInOrderByCreatedAtDesc(UUID matchId, List<GameSessionStatus> statuses);

    List<GameSessionEntity> findByStatusAndCreatedAtBefore(GameSessionStatus status, LocalDateTime before);
}
