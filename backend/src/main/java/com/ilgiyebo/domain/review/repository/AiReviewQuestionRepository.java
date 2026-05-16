package com.ilgiyebo.repository;

import com.ilgiyebo.domain.AiReviewQuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AiReviewQuestionRepository extends JpaRepository<AiReviewQuestionEntity, UUID> {
    List<AiReviewQuestionEntity> findBySessionIdOrderByQuestionOrderAsc(UUID sessionId);
}
