package com.ilgiyebo.domain.mission.repository;

import com.ilgiyebo.domain.mission.entity.MissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<MissionEntity, UUID> {
    Optional<MissionEntity> findByMatchId(UUID matchId);
}
