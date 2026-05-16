package com.ilgiyebo.domain.interaction.repository;

import com.ilgiyebo.domain.interaction.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<QuizEntity, UUID> {

    List<QuizEntity> findByUserIdOrderByQuizIndex(UUID userId);

    boolean existsByUserId(UUID userId);
}
