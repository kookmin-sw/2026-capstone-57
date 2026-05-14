package com.ilgiyebo.domain.diary.service;

import com.ilgiyebo.domain.diary.dto.*;
import com.ilgiyebo.domain.diary.entity.*;
import com.ilgiyebo.domain.diary.exception.DiarySessionException;
import com.ilgiyebo.domain.diary.repository.DiaryConversationTurnRepository;
import com.ilgiyebo.domain.diary.repository.DiaryEntryRepository;
import com.ilgiyebo.domain.diary.repository.DiarySessionRepository;
import com.ilgiyebo.domain.planner.dto.PlanEntryResponse;
import com.ilgiyebo.domain.planner.service.PlannerService;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiarySessionServiceImpl implements DiarySessionService {

    private static final int DEFAULT_MAX_TURNS = 5;
    private static final int MAX_AI_INFERRED_ITEMS = 20;

    private final DiarySessionRepository diarySessionRepository;
    private final DiaryConversationTurnRepository conversationTurnRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;
    private final PlannerService plannerService;
    private final DiaryService diaryService;
    private final DiaryAiClient diaryAiClient;

    @Override
    @Transactional
    public DiarySessionResponse startAISession(UUID userId, LocalDate date) {
        UserEntity user = findUserOrThrow(userId);

        // 동일 날짜에 이미 완료된 세션이 있으면 새 세션 생성 방지
        boolean hasCompletedSession = diarySessionRepository.existsByUserIdAndTargetDateAndStatusIn(
                userId, date, List.of(DiarySessionStatus.COMPLETED));
        if (hasCompletedSession) {
            throw DiarySessionException.SESSION_ALREADY_COMPLETED.toException();
        }

        // 이미 진행 중인 세션이 있으면 해당 세션 반환
        List<DiarySessionStatus> activeStatuses = List.of(
                DiarySessionStatus.IN_PROGRESS,
                DiarySessionStatus.READY_TO_GENERATE,
                DiarySessionStatus.GENERATED
        );
        Optional<DiarySessionEntity> existingSession = diarySessionRepository
                .findByUserIdAndTargetDateAndStatusIn(userId, date, activeStatuses);
        if (existingSession.isPresent()) {
            DiarySessionEntity session = existingSession.get();
            String currentQuestion = getLatestUnansweredQuestion(session);
            return DiarySessionResponse.from(session, currentQuestion);
        }

        // 당일 플래너 데이터를 컨텍스트로 활용
        List<PlanEntryResponse> todayPlan = plannerService.getPlanEntries(userId, date);

        // 전날 일기 조회 (연속성 참고)
        Optional<DiaryEntryEntity> previousDiary = diaryEntryRepository
                .findByUserIdAndEntryDate(userId, date.minusDays(1));

        // AI에 첫 질문 생성 요청
        DiaryAiClient.FirstQuestionInput aiInput = new DiaryAiClient.FirstQuestionInput(
                userId.toString(),
                date.toString(),
                todayPlan.stream()
                        .map(p -> new DiaryAiClient.ScheduleContext(
                                p.startTime() != null ? p.startTime().toString() : null,
                                p.endTime() != null ? p.endTime().toString() : null,
                                p.location(),
                                p.name()))
                        .toList(),
                previousDiary.map(DiaryEntryEntity::getContent).orElse(null)
        );

        String firstQuestion = diaryAiClient.generateFirstQuestion(aiInput);

        // 세션 생성
        DiarySessionEntity session = DiarySessionEntity.builder()
                .user(user)
                .targetDate(date)
                .status(DiarySessionStatus.IN_PROGRESS)
                .maxTurns(DEFAULT_MAX_TURNS)
                .currentTurn(1)
                .build();
        session = diarySessionRepository.save(session);

        // 첫 번째 대화 턴 생성 (질문만 저장, 답변 대기)
        DiaryConversationTurnEntity firstTurn = DiaryConversationTurnEntity.builder()
                .session(session)
                .turnNumber(1)
                .question(firstQuestion)
                .askedAt(LocalDateTime.now())
                .build();
        conversationTurnRepository.save(firstTurn);
        session.getConversationTurns().add(firstTurn);

        log.debug("AI 일기 세션 시작: userId={}, date={}, sessionId={}", userId, date, session.getId());

        return DiarySessionResponse.from(session, firstQuestion);
    }

    @Override
    @Transactional
    public DiaryTurnResponse answerQuestion(UUID sessionId, UUID userId, String answer) {
        DiarySessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
        validateSessionStatus(session, DiarySessionStatus.IN_PROGRESS);

        // 현재 턴의 미답변 질문 찾기
        DiaryConversationTurnEntity currentTurn = session.getConversationTurns().stream()
                .filter(t -> t.getAnswer() == null)
                .findFirst()
                .orElseThrow(DiarySessionException.NO_PENDING_QUESTION::toException);

        // 답변 저장
        currentTurn.setAnswer(answer);
        currentTurn.setAnsweredAt(LocalDateTime.now());
        conversationTurnRepository.save(currentTurn);

        // AI에 다음 질문 생성 요청
        List<DiaryAiClient.ConversationTurn> history = session.getConversationTurns().stream()
                .filter(t -> t.getAnswer() != null)
                .map(t -> new DiaryAiClient.ConversationTurn(
                        t.getTurnNumber(), t.getQuestion(), t.getAnswer()))
                .toList();

        List<PlanEntryResponse> todayPlan = plannerService.getPlanEntries(userId, session.getTargetDate());

        DiaryAiClient.NextQuestionInput aiInput = new DiaryAiClient.NextQuestionInput(
                userId.toString(),
                session.getTargetDate().toString(),
                history,
                todayPlan.stream()
                        .map(p -> new DiaryAiClient.ScheduleContext(
                                p.startTime() != null ? p.startTime().toString() : null,
                                p.endTime() != null ? p.endTime().toString() : null,
                                p.location(),
                                p.name()))
                        .toList()
        );

        DiaryAiClient.NextQuestionResult aiResult = diaryAiClient.generateNextQuestion(aiInput);

        // maxTurns 도달 또는 AI 조기 종료
        boolean isCompleted = aiResult.isConversationComplete()
                || session.getCurrentTurn() >= session.getMaxTurns();

        if (isCompleted) {
            session.setStatus(DiarySessionStatus.READY_TO_GENERATE);
            diarySessionRepository.save(session);

            log.debug("AI 일기 대화 완료: sessionId={}, turns={}", sessionId, session.getCurrentTurn());

            return new DiaryTurnResponse(
                    sessionId, true, null,
                    session.getCurrentTurn(), session.getMaxTurns());
        }

        // 다음 턴 생성
        int nextTurnNumber = session.getCurrentTurn() + 1;
        session.setCurrentTurn(nextTurnNumber);
        diarySessionRepository.save(session);

        DiaryConversationTurnEntity nextTurn = DiaryConversationTurnEntity.builder()
                .session(session)
                .turnNumber(nextTurnNumber)
                .question(aiResult.nextQuestion())
                .askedAt(LocalDateTime.now())
                .build();
        conversationTurnRepository.save(nextTurn);
        session.getConversationTurns().add(nextTurn);

        return new DiaryTurnResponse(
                sessionId, false, aiResult.nextQuestion(),
                nextTurnNumber, session.getMaxTurns());
    }

    @Override
    @Transactional
    public GeneratedDiaryPreview generateDiary(UUID sessionId, UUID userId) {
        DiarySessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
        validateSessionStatus(session, DiarySessionStatus.READY_TO_GENERATE);

        // 전체 대화 내용 수집
        List<DiaryAiClient.ConversationTurn> history = session.getConversationTurns().stream()
                .filter(t -> t.getAnswer() != null)
                .map(t -> new DiaryAiClient.ConversationTurn(
                        t.getTurnNumber(), t.getQuestion(), t.getAnswer()))
                .toList();

        List<PlanEntryResponse> todayPlan = plannerService.getPlanEntries(userId, session.getTargetDate());

        DiaryAiClient.DiaryContentInput aiInput = new DiaryAiClient.DiaryContentInput(
                sessionId.toString(),
                userId.toString(),
                session.getTargetDate().toString(),
                history,
                todayPlan.stream()
                        .map(p -> new DiaryAiClient.ScheduleContext(
                                p.startTime() != null ? p.startTime().toString() : null,
                                p.endTime() != null ? p.endTime().toString() : null,
                                p.location(),
                                p.name()))
                        .toList()
        );

        DiaryAiClient.GeneratedDiaryResult aiResult = diaryAiClient.generateDiaryContent(aiInput);

        // AI 추론 프로필 데이터를 사용자 엔티티에 병합 (중복 제거, 최대 20개 제한)
        if (aiResult.profileUpdate() != null) {
            UserEntity user = findUserOrThrow(userId);
            mergeAiInferredProfile(user, aiResult.profileUpdate());
            userRepository.save(user);
        }

        // 세션 상태 업데이트
        session.setGeneratedContent(aiResult.generatedContent());
        session.setSuggestedEmotion(aiResult.suggestedEmotion());
        session.setStatus(DiarySessionStatus.GENERATED);
        diarySessionRepository.save(session);

        log.debug("AI 일기 생성 완료: sessionId={}", sessionId);

        return new GeneratedDiaryPreview(
                sessionId,
                aiResult.generatedContent(),
                aiResult.suggestedEmotion(),
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional
    public DiaryEntryResponse confirmDiary(UUID sessionId, UUID userId, String editedContent, EmotionTag emotionTag) {
        DiarySessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
        validateSessionStatus(session, DiarySessionStatus.GENERATED);

        // 최종 내용 결정 (수정본 우선, 없으면 AI 생성본)
        String finalContent = (editedContent != null && !editedContent.isBlank())
                ? editedContent
                : session.getGeneratedContent();

        // 감정 태그 결정 (사용자 선택 우선, 없으면 AI 추천)
        EmotionTag finalEmotion = emotionTag != null ? emotionTag : session.getSuggestedEmotion();

        // DiaryService를 통해 DiaryEntry 저장
        DiaryInputDto diaryInput = new DiaryInputDto(
                finalContent,
                finalEmotion,
                session.getTargetDate(),
                DiarySource.AI_GENERATED,
                session.getId()
        );

        DiaryEntryResponse entry = diaryService.createEntry(userId, diaryInput);

        // 세션 상태 COMPLETED로 전환
        session.setStatus(DiarySessionStatus.COMPLETED);
        diarySessionRepository.save(session);

        log.debug("AI 일기 확정 완료: sessionId={}, diaryEntryId={}", sessionId, entry.id());

        return entry;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiarySessionResponse> getActiveSession(UUID userId, LocalDate date) {
        List<DiarySessionStatus> activeStatuses = List.of(
                DiarySessionStatus.IN_PROGRESS,
                DiarySessionStatus.READY_TO_GENERATE,
                DiarySessionStatus.GENERATED
        );

        return diarySessionRepository.findByUserIdAndTargetDateAndStatusIn(userId, date, activeStatuses)
                .map(session -> {
                    String currentQuestion = getLatestUnansweredQuestion(session);
                    return DiarySessionResponse.from(session, currentQuestion);
                });
    }

    @Override
    @Transactional
    public void cancelSession(UUID sessionId, UUID userId) {
        DiarySessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        if (session.getStatus() == DiarySessionStatus.COMPLETED
                || session.getStatus() == DiarySessionStatus.CANCELLED) {
            throw DiarySessionException.SESSION_CANNOT_BE_CANCELLED.toException();
        }

        session.setStatus(DiarySessionStatus.CANCELLED);
        diarySessionRepository.save(session);

        log.debug("AI 일기 세션 취소: sessionId={}", sessionId);
    }

    // --- Private helpers ---

    private String getLatestUnansweredQuestion(DiarySessionEntity session) {
        return session.getConversationTurns().stream()
                .filter(t -> t.getAnswer() == null)
                .map(DiaryConversationTurnEntity::getQuestion)
                .findFirst()
                .orElse(null);
    }

    private DiarySessionEntity findSessionOrThrow(UUID sessionId) {
        return diarySessionRepository.findById(sessionId)
                .orElseThrow(DiarySessionException.SESSION_NOT_FOUND::toException);
    }

    private void validateSessionOwner(DiarySessionEntity session, UUID userId) {
        if (!session.getUser().getId().equals(userId)) {
            throw DiarySessionException.SESSION_NOT_OWNED.toException();
        }
    }

    private void validateSessionStatus(DiarySessionEntity session, DiarySessionStatus expectedStatus) {
        if (session.getStatus() != expectedStatus) {
            throw DiarySessionException.INVALID_SESSION_STATUS.toException();
        }
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(DiarySessionException.USER_NOT_FOUND::toException);
    }

    /**
     * AI가 추론한 취미/관심사를 사용자의 aiInferred 필드에 병합한다.
     * 최신 항목을 앞에 배치하고, 중복을 제거한 뒤 최대 20개까지만 유지한다.
     */
    private void mergeAiInferredProfile(UserEntity user, DiaryAiClient.ProfileUpdate profileUpdate) {
        if (profileUpdate.hobbies() != null && !profileUpdate.hobbies().isEmpty()) {
            List<String> current = user.getAiInferredHobbies() != null
                    ? user.getAiInferredHobbies()
                    : List.of();
            // 새 항목을 앞에 넣고, 기존 항목을 뒤에 붙여서 중복 제거
            java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(profileUpdate.hobbies());
            merged.addAll(current);
            user.setAiInferredHobbies(merged.stream().limit(MAX_AI_INFERRED_ITEMS).toList());
        }

        if (profileUpdate.interests() != null && !profileUpdate.interests().isEmpty()) {
            List<String> current = user.getAiInferredInterests() != null
                    ? user.getAiInferredInterests()
                    : List.of();
            java.util.LinkedHashSet<String> merged = new java.util.LinkedHashSet<>(profileUpdate.interests());
            merged.addAll(current);
            user.setAiInferredInterests(merged.stream().limit(MAX_AI_INFERRED_ITEMS).toList());
        }

        log.debug("AI 추론 프로필 업데이트: userId={}, hobbies={}, interests={}",
                user.getId(),
                user.getAiInferredHobbies() != null ? user.getAiInferredHobbies().size() : 0,
                user.getAiInferredInterests() != null ? user.getAiInferredInterests().size() : 0);
    }
}
