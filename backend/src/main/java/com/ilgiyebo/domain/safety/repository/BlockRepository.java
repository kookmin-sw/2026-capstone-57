package com.ilgiyebo.domain.safety.repository;

import com.ilgiyebo.domain.safety.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<BlockEntity, UUID> {
    boolean existsByUser_IdAndBlockedUser_Id(UUID userId, UUID blockedUserId);
    List<BlockEntity> findByUser_Id(UUID userId);
}
