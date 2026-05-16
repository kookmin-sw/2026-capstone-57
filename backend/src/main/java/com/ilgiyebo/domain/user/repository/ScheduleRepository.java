package com.ilgiyebo.domain.user.repository;

import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {

    List<ScheduleEntity> findAllByUserId(UUID userId);

    List<ScheduleEntity> findAllByUserIdAndDayOfWeek(UUID userId, DayOfWeek dayOfWeek);

    List<ScheduleEntity> findAllByUserIdAndSemesterId(UUID userId, UUID semesterId);

    void deleteByUserIdAndSemesterId(UUID userId, UUID semesterId);

    void deleteByUserId(UUID userId);
}
