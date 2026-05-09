package com.ilgiyebo.repository;

import com.ilgiyebo.domain.ReviewSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReviewSessionRepository extends JpaRepository<ReviewSessionEntity, UUID> {
    Optional<ReviewSessionEntity> findByInteractionIdAndUserId(UUID interactionId, UUID userId);
}
