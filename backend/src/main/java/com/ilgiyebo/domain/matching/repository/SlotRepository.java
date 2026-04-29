package com.ilgiyebo.repository;

import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<SlotEntity, UUID> {
    List<SlotEntity> findByUserId(UUID userId);
    List<SlotEntity> findByUserIdAndStatus(UUID userId, SlotStatus status);
}
