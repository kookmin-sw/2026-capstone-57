package com.ilgiyebo.repository;

import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {

    Optional<ChatSessionEntity> findByMatchId(UUID matchId);

    Optional<ChatSessionEntity> findByMatchIdAndStatus(UUID matchId, ChatSessionStatus status);

    List<ChatSessionEntity> findAllByStatusAndEndTimeBefore(ChatSessionStatus status, Instant now);
}
