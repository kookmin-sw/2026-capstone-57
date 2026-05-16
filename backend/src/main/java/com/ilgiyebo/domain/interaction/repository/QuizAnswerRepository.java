package com.ilgiyebo.domain.interaction.repository;

import com.ilgiyebo.domain.interaction.entity.QuizAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswerEntity, UUID> {

    List<QuizAnswerEntity> findByMatchIdAndUserId(UUID matchId, UUID userId);

    boolean existsByMatchIdAndUserIdAndQuizId(UUID matchId, UUID userId, UUID quizId);
}
