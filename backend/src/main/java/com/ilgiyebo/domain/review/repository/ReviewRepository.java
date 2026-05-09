package com.ilgiyebo.domain.review.repository;

import com.ilgiyebo.domain.review.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    Optional<ReviewEntity> findByInteraction_IdAndUser_Id(UUID interactionId, UUID userId);
}
