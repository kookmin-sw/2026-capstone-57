package com.ilgiyebo.domain.mission.repository;

import com.ilgiyebo.domain.mission.entity.MissionEntity;
import com.ilgiyebo.domain.mission.entity.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<MissionEntity, UUID> {
    Optional<MissionEntity> findByMatchId(UUID matchId);

    List<MissionEntity> findByStatusAndDeadlineBefore(MissionStatus status, Instant deadline);
}
