package com.ilgiyebo.repository;

import com.ilgiyebo.domain.InteractionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<InteractionEntity, UUID> {
    Optional<InteractionEntity> findByMatchId(UUID matchId);
}
