package com.ilgiyebo.domain.game.repository;

import com.ilgiyebo.domain.game.entity.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSessionEntity, UUID> {
    Optional<GameSessionEntity> findByMatchId(UUID matchId);
}
