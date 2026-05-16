package com.ilgiyebo.repository;

import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
    List<MatchEntity> findByUserAIdAndStatus(UUID userAId, MatchStatus status);
    List<MatchEntity> findByUserBIdAndStatus(UUID userBId, MatchStatus status);
}
