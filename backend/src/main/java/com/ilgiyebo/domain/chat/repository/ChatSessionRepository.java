package com.ilgiyebo.domain.chat.repository;

import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {

    Optional<ChatSessionEntity> findTopByMatchIdOrderByCreatedAtDesc(UUID matchId);

    Optional<ChatSessionEntity> findByMatchIdAndStatus(UUID matchId, ChatSessionStatus status);
}
