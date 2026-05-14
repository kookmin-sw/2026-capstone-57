package com.ilgiyebo.domain.diary.service;

import com.ilgiyebo.domain.diary.entity.EmotionTag;

import java.util.List;

/**
 * AI 서버와 통신하여 일기 관련 질문/내용을 생성하는 클라이언트 인터페이스.
 * 실제 AI 서버 HTTP 연동은 task 15.6에서 구현한다.
 * 현재는 스텁 구현체를 사용한다.
 */
public interface DiaryAiClient {

    /**
     * 첫 번째 질문 생성.
     * 당일 플래너 데이터와 전날 일기를 컨텍스트로 활용한다.
     */
    String generateFirstQuestion(FirstQuestionInput input);

    /**
     * 다음 질문 생성 또는 대화 완료 판단.
     */
    NextQuestionResult generateNextQuestion(NextQuestionInput input);

    /**
     * 전체 대화 내용 기반 일기 생성.
     */
    GeneratedDiaryResult generateDiaryContent(DiaryContentInput input);

    // --- Input/Output records ---

    record FirstQuestionInput(
            String userId,
            String targetDate,
            List<ScheduleContext> todaySchedule,
            String previousDiaryContent
    ) {}

    record ScheduleContext(
            String startTime,
            String endTime,
            String location,
            String activity
    ) {}

    record NextQuestionInput(
            String userId,
            String targetDate,
            List<ConversationTurn> conversationHistory,
            List<ScheduleContext> todaySchedule
    ) {}

    record ConversationTurn(
            int turnNumber,
            String question,
            String answer
    ) {}

    record DiaryContentInput(
            String sessionId,
            String userId,
            String targetDate,
            List<ConversationTurn> conversationHistory,
            List<ScheduleContext> todaySchedule
    ) {}

    record NextQuestionResult(
            boolean isConversationComplete,
            String nextQuestion,
            String completionReason
    ) {}

    record GeneratedDiaryResult(
            String generatedContent,
            EmotionTag suggestedEmotion,
            ProfileUpdate profileUpdate
    ) {}

    record ProfileUpdate(
            List<String> hobbies,
            List<String> interests
    ) {}
}
