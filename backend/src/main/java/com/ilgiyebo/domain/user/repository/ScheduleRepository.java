package com.ilgiyebo.repository;

import com.ilgiyebo.domain.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {

    List<ScheduleEntity> findAllByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
