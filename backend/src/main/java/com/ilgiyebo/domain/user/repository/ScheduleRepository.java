package com.ilgiyebo.domain.user.repository;

import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, UUID> {

    List<ScheduleEntity> findAllByUser_Id(UUID userId);

    List<ScheduleEntity> findAllByUser_IdAndDayOfWeek(UUID userId, DayOfWeek dayOfWeek);

    void deleteByUser_Id(UUID userId);
}
