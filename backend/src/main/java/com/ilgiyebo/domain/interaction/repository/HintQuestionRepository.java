package com.ilgiyebo.domain.interaction.repository;

import com.ilgiyebo.domain.interaction.entity.HintQuestionEntity;
import com.ilgiyebo.domain.interaction.entity.HintQuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HintQuestionRepository extends JpaRepository<HintQuestionEntity, UUID> {
    List<HintQuestionEntity> findByMatchId(UUID matchId);
    List<HintQuestionEntity> findByMatchIdAndStatus(UUID matchId, HintQuestionStatus status);
    List<HintQuestionEntity> findByResponderIdAndStatus(UUID responderId, HintQuestionStatus status);
}
