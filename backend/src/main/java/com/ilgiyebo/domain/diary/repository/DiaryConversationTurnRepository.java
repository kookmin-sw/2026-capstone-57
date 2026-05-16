package com.ilgiyebo.domain.diary.repository;

import com.ilgiyebo.domain.diary.entity.DiaryConversationTurnEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiaryConversationTurnRepository extends JpaRepository<DiaryConversationTurnEntity, UUID> {

    /**
     * 세션별 대화 턴 목록 조회 (턴 번호 순)
     */
    List<DiaryConversationTurnEntity> findBySessionIdOrderByTurnNumberAsc(UUID sessionId);
}
