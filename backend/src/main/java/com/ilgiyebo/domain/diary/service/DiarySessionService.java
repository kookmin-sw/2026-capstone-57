package com.ilgiyebo.domain.diary.service;

import com.ilgiyebo.domain.diary.dto.*;
import com.ilgiyebo.domain.diary.entity.EmotionTag;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 일기 멀티턴 대화 세션을 관리하는 서비스.
 */
public interface DiarySessionService {

    /** AI 일기 세션 시작 - 첫 번째 질문 생성 */
    DiarySessionResponse startAISession(UUID userId, LocalDate date);

    /** 질문에 답변 - 다음 질문 또는 대화 완료 반환 */
    DiaryTurnResponse answerQuestion(UUID sessionId, UUID userId, String answer);

    /** 답변 기반 일기 생성 요청 */
    GeneratedDiaryPreview generateDiary(UUID sessionId, UUID userId);

    /** 생성된 일기 확정 (선택적 수정 포함) */
    DiaryEntryResponse confirmDiary(UUID sessionId, UUID userId, String editedContent, EmotionTag emotionTag);

    /** 세션 조회 (진행 중인 세션 확인) */
    Optional<DiarySessionResponse> getActiveSession(UUID userId, LocalDate date);

    /** 세션 취소 */
    void cancelSession(UUID sessionId, UUID userId);
}
