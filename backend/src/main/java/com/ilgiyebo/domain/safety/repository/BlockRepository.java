package com.ilgiyebo.repository;

import com.ilgiyebo.domain.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<BlockEntity, UUID> {
    boolean existsByUserIdAndBlockedUserId(UUID userId, UUID blockedUserId);
    List<BlockEntity> findByUserId(UUID userId);
}
