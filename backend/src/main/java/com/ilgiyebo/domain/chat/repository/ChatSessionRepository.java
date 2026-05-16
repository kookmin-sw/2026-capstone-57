package com.ilgiyebo.repository;

import com.ilgiyebo.domain.ChatSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {
    Optional<ChatSessionEntity> findByMatchId(UUID matchId);
}
