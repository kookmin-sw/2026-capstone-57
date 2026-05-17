package com.ilgiyebo.domain.interaction.repository;

import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<InteractionEntity, UUID> {
    Optional<InteractionEntity> findByMatchId(UUID matchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InteractionEntity i WHERE i.match.id = :matchId")
    Optional<InteractionEntity> findByMatchIdForUpdate(@Param("matchId") UUID matchId);
}
