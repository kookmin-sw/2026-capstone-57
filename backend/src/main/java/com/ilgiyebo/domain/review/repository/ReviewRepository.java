package com.ilgiyebo.repository;

import com.ilgiyebo.domain.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    Optional<ReviewEntity> findByInteractionIdAndUserId(UUID interactionId, UUID userId);
}
