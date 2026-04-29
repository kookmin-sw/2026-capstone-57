package com.ilgiyebo.repository;

import com.ilgiyebo.domain.NotificationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingRepository extends JpaRepository<NotificationSettingEntity, UUID> {
    Optional<NotificationSettingEntity> findByUserId(UUID userId);
}
