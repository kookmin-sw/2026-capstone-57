package com.ilgiyebo.domain.review.service;

import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.review.dto.*;
import com.ilgiyebo.domain.review.entity.*;
import com.ilgiyebo.domain.review.exception.ReviewException;
import com.ilgiyebo.domain.review.repository.AiReviewQuestionRepository;
import com.ilgiyebo.domain.review.repository.ReviewRepository;
import com.ilgiyebo.domain.review.repository.ReviewSessionRepository;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int REVIEW_STAGE = 5;
    private static final int MAX_AI_QUESTION_COUNT = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewSessionRepository reviewSessionRepository;
    private final AiReviewQuestionRepository aiReviewQuestionRepository;
    private final MatchRepository matchRepository;
    private final InteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final ReviewAiClient reviewAiClient;
    private final ExperienceGrantPort experienceGrantPort;
    private final TransactionTemplate transactionTemplate;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public ReviewSessionResponse selectReviewMode(UUID matchId, UUID userId, ReviewMode mode) {
        MatchEntity match = findMatchOrThrow(matchId);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = findInteractionOrThrow(matchId);
        validateReviewStage(interaction);

        if (reviewRepository.findByInteractionIdAndUserId(interaction.getId(), userId).isPresent()) {
            throw ReviewException.REVIEW_ALREADY_EXISTS.toException();
        }

        if (reviewSessionRepository.findByInteractionIdAndUserId(interaction.getId(), userId).isPresent()) {
            throw ReviewException.SESSION_ALREADY_EXISTS.toException();
        }

        UserEntity user = findUserOrThrow(userId);

        ReviewSessionEntity session = ReviewSessionEntity.builder()
                .interaction(interaction)
                .user(user)
                .mode(mode)
                .status(ReviewSessionStatus.IN_PROGRESS)
                .build();
        session = reviewSessionRepository.save(session);

        // AI 모드: 질문은 getAIQuestions() 호출 시 lazy하게 생성 (트랜잭션 내 AI 호출 방지)
        log.debug("회고 모드 선택 완료: matchId={}, userId={}, mode={}, sessionId={}",
                matchId, userId, mode, session.getId());

        return ReviewSessionResponse.from(session);
    }

    @Override
    public List<AiReviewQuestionDto> getAIQuestions(UUID matchId, UUID sessionId, UUID userId) {
        // 1단계: DB 검증 + mission 프록시 강제 초기화 (트랜잭션)
        record MissionContext(String missionDesc, String missionLoc) {}

        MissionContext missionCtx = Objects.requireNonNull(
                transactionTemplate.execute(status -> {
                    ReviewSessionEntity s = findSessionOrThrow(sessionId);
                    validateSessionOwner(s, userId);
                    validateSessionMatchId(s, matchId);
                    validateSessionMode(s, ReviewMode.AI_ASSISTED);

                    // mission LAZY 프록시 강제 초기화 (LazyInitializationException 방지)
                    InteractionEntity interaction = s.getInteraction();
                    String desc = "만남 미션";
                    String loc = "캠퍼스";
                    if (interaction.getMission() != null) {
                        // activity도 AI 프롬프트에 활용
                        String activity = interaction.getMission().getActivity();
                        desc = interaction.getMission().getDescription() != null
                                ? interaction.getMission().getDescription()
                                : (activity != null ? activity : desc);
                        loc = interaction.getMission().getLocation() != null
                                ? interaction.getMission().getLocation() : loc;
                    }
                    return new MissionContext(desc, loc);
                }),
                "트랜잭션 실행 결과가 null입니다"
        );

        // 2단계: 질문 조회 (트랜잭션)
        List<AiReviewQuestionEntity> existingQuestions = Objects.requireNonNull(
                transactionTemplate.execute(status ->
                        aiReviewQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)),
                "트랜잭션 실행 결과가 null입니다"
        );

        // 3단계: 질문이 없으면 AI 호출 (트랜잭션 밖) + 저장 (별도 트랜잭션)
        List<AiReviewQuestionEntity> questions;
        if (existingQuestions.isEmpty()) {
            List<String> questionTexts = reviewAiClient.generateReviewQuestions(
                    missionCtx.missionDesc(), missionCtx.missionLoc());

            if (questionTexts.isEmpty()) {
                throw ReviewException.QUESTION_NOT_FOUND.toException();
            }
            if (questionTexts.size() > MAX_AI_QUESTION_COUNT) {
                questionTexts = questionTexts.subList(0, MAX_AI_QUESTION_COUNT);
            }

            List<String> finalQuestionTexts = questionTexts;
            questions = Objects.requireNonNull(
                    transactionTemplate.execute(status -> {
                        List<AiReviewQuestionEntity> entities = new ArrayList<>();
                        for (int i = 0; i < finalQuestionTexts.size(); i++) {
                            AiReviewQuestionEntity q = AiReviewQuestionEntity.builder()
                                    .sessionId(sessionId)
                                    .question(finalQuestionTexts.get(i))
                                    .questionOrder(i + 1)
                                    .build();
                            entities.add(aiReviewQuestionRepository.save(q));
                        }
                        return entities;
                    }),
                    "트랜잭션 실행 결과가 null입니다"
            );
            log.debug("AI 회고 질문 생성 완료: sessionId={}, 질문 수={}", sessionId, questions.size());
        } else {
            questions = existingQuestions;
        }

        log.debug("AI 회고 질문 조회: sessionId={}, 질문 수={}", sessionId, questions.size());

        return questions.stream()
                .map(AiReviewQuestionDto::from)
                .toList();
    }

    @Override
    @Transactional
    public AiReviewQuestionDto answerAIQuestion(UUID matchId, UUID sessionId, UUID questionId, UUID userId, String answer) {
        ReviewSessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
        validateSessionMatchId(session, matchId);
        validateSessionMode(session, ReviewMode.AI_ASSISTED);
        validateSessionStatus(session, ReviewSessionStatus.IN_PROGRESS);

        AiReviewQuestionEntity question = aiReviewQuestionRepository.findById(questionId)
                .orElseThrow(ReviewException.QUESTION_NOT_FOUND::toException);

        if (!question.getSessionId().equals(sessionId)) {
            throw ReviewException.QUESTION_NOT_FOUND.toException();
        }

        if (question.getAnswer() != null) {
            throw ReviewException.QUESTION_ALREADY_ANSWERED.toException();
        }

        question.setAnswer(answer);
        question.setAnsweredAt(Instant.now());
        aiReviewQuestionRepository.save(question);

        log.debug("AI 회고 질문 답변 완료: sessionId={}, questionId={}, order={}",
                sessionId, questionId, question.getQuestionOrder());

        return AiReviewQuestionDto.from(question);
    }

    @Override
    public GeneratedReviewPreview generateReview(UUID matchId, UUID sessionId, UUID userId) {
        // 1단계: DB 검증만 (상태 변경 없음)
        List<AiReviewQuestionEntity> questions = Objects.requireNonNull(
                transactionTemplate.execute(status -> {
                    ReviewSessionEntity session = findSessionOrThrow(sessionId);
                    validateSessionOwner(session, userId);
                    validateSessionMatchId(session, matchId);
                    validateSessionMode(session, ReviewMode.AI_ASSISTED);
                    validateSessionStatus(session, ReviewSessionStatus.IN_PROGRESS);

                    List<AiReviewQuestionEntity> qs =
                            aiReviewQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

                    if (qs.isEmpty()) {
                        throw ReviewException.NOT_ALL_QUESTIONS_ANSWERED.toException();
                    }

                    boolean allAnswered = qs.stream().allMatch(q -> q.getAnswer() != null);
                    if (!allAnswered) {
                        throw ReviewException.NOT_ALL_QUESTIONS_ANSWERED.toException();
                    }

                    return qs;
                }),
                "트랜잭션 실행 결과가 null입니다"
        );

        // 2단계: AI 호출 (트랜잭션 밖 — 실패해도 DB 상태 오염 없음)
        String generatedContent = generateAiReviewContent(questions);
        // TODO: AI 연동 시 동적 값으로 교체 예정
        int suggestedSatisfaction = 4;

        // 3단계: AI 성공 후에만 상태 변경 (별도 트랜잭션)
        transactionTemplate.executeWithoutResult(status -> {
            ReviewSessionEntity session = findSessionOrThrow(sessionId);
            session.setStatus(ReviewSessionStatus.GENERATED);
            reviewSessionRepository.save(session);
        });

        log.debug("AI 회고 생성 완료: sessionId={}", sessionId);

        return new GeneratedReviewPreview(sessionId, generatedContent, suggestedSatisfaction);
    }

    @Override
    @Transactional
    public ReviewResponse editGeneratedReview(UUID matchId, UUID sessionId, UUID userId, EditReviewRequest request) {
        ReviewSessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
        validateSessionMatchId(session, matchId);
        validateSessionMode(session, ReviewMode.AI_ASSISTED);
        validateSessionStatus(session, ReviewSessionStatus.GENERATED);

        validateSatisfaction(request.satisfaction());

        InteractionEntity interaction = session.getInteraction();
        UserEntity user = session.getUser();

        ReviewEntity review = ReviewEntity.builder()
                .interaction(interaction)
                .user(user)
                .mode(ReviewMode.AI_ASSISTED)
                .satisfaction(request.satisfaction())
                .reflection(request.reflection())
                .wantToMeetAgain(request.wantToMeetAgain())
                .aiGenerated(true)
                .build();
        review = reviewRepository.save(review);

        session.setStatus(ReviewSessionStatus.COMPLETED);
        reviewSessionRepository.save(session);

        markReviewCompleted(interaction.getMatch().getId(), userId);

        log.debug("AI 회고 확정 완료: sessionId={}, reviewId={}", sessionId, review.getId());

        return ReviewResponse.from(review);
    }

    @Override
    @Transactional
    public ReviewResponse submitDirectReview(UUID matchId, UUID userId, DirectReviewInputDto review) {
        MatchEntity match = findMatchOrThrow(matchId);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = findInteractionOrThrow(matchId);
        validateReviewStage(interaction);

        if (reviewRepository.findByInteractionIdAndUserId(interaction.getId(), userId).isPresent()) {
            throw ReviewException.REVIEW_ALREADY_EXISTS.toException();
        }

        validateSatisfaction(review.satisfaction());

        UserEntity user = findUserOrThrow(userId);

        Optional<ReviewSessionEntity> existingSession = reviewSessionRepository
                .findByInteractionIdAndUserId(interaction.getId(), userId);
        if (existingSession.isPresent() && existingSession.get().getMode() == ReviewMode.AI_ASSISTED) {
            throw ReviewException.SESSION_MODE_MISMATCH.toException();
        }

        ReviewSessionEntity session = existingSession
                .orElseGet(() -> {
                    ReviewSessionEntity newSession = ReviewSessionEntity.builder()
                            .interaction(interaction)
                            .user(user)
                            .mode(ReviewMode.DIRECT)
                            .status(ReviewSessionStatus.IN_PROGRESS)
                            .build();
                    return reviewSessionRepository.save(newSession);
                });

        ReviewEntity reviewEntity = ReviewEntity.builder()
                .interaction(interaction)
                .user(user)
                .mode(ReviewMode.DIRECT)
                .satisfaction(review.satisfaction())
                .reflection(review.reflection())
                .wantToMeetAgain(review.wantToMeetAgain())
                .aiGenerated(false)
                .build();
        reviewEntity = reviewRepository.save(reviewEntity);

        session.setStatus(ReviewSessionStatus.COMPLETED);
        reviewSessionRepository.save(session);

        markReviewCompleted(matchId, userId);

        log.debug("직접 회고 제출 완료: matchId={}, userId={}, reviewId={}",
                matchId, userId, reviewEntity.getId());

        return ReviewResponse.from(reviewEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReviewResponse> getReview(UUID matchId, UUID userId) {
        MatchEntity match = findMatchOrThrow(matchId);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = findInteractionOrThrow(matchId);

        return reviewRepository.findByInteractionIdAndUserId(interaction.getId(), userId)
                .map(ReviewResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(
            UUID userId,
            ReviewMode mode,
            Integer minSatisfaction,
            Integer maxSatisfaction,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        validateSatisfactionFilter(minSatisfaction, "minSatisfaction");
        validateSatisfactionFilter(maxSatisfaction, "maxSatisfaction");
        if (minSatisfaction != null && maxSatisfaction != null && minSatisfaction > maxSatisfaction) {
            throw ReviewException.INVALID_SATISFACTION.toException();
        }
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw ReviewException.INVALID_DATE_RANGE.toException();
        }

        log.debug("본인 회고 목록 조회: userId={}, mode={}, minSat={}, maxSat={}, from={}, to={}, page={}, size={}",
                userId, mode, minSatisfaction, maxSatisfaction, fromDate, toDate,
                pageable.getPageNumber(), pageable.getPageSize());

        return reviewRepository.findMyReviewsWithFilters(
                        userId, mode, minSatisfaction, maxSatisfaction, fromDate, toDate, pageable)
                .map(ReviewResponse::from);
    }

    private void validateSatisfactionFilter(Integer satisfaction, String paramName) {
        if (satisfaction != null && (satisfaction < 1 || satisfaction > 5)) {
            log.warn("잘못된 만족도 필터: {}={}", paramName, satisfaction);
            throw ReviewException.INVALID_SATISFACTION.toException();
        }
    }

    // --- Private helpers ---

    private MatchEntity findMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(ReviewException.MATCH_NOT_FOUND::toException);
    }

    private InteractionEntity findInteractionOrThrow(UUID matchId) {
        return interactionRepository.findByMatchId(matchId)
                .orElseThrow(ReviewException.INTERACTION_NOT_FOUND::toException);
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(ReviewException.USER_NOT_FOUND::toException);
    }

    private ReviewSessionEntity findSessionOrThrow(UUID sessionId) {
        return reviewSessionRepository.findById(sessionId)
                .orElseThrow(ReviewException.SESSION_NOT_FOUND::toException);
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw ReviewException.USER_NOT_IN_MATCH.toException();
        }
    }

    private void validateReviewStage(InteractionEntity interaction) {
        if (interaction.getCurrentStage() != REVIEW_STAGE) {
            throw ReviewException.NOT_IN_REVIEW_STAGE.toException();
        }
        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            throw ReviewException.NOT_IN_REVIEW_STAGE.toException();
        }
    }

    private void validateSessionOwner(ReviewSessionEntity session, UUID userId) {
        if (!session.getUser().getId().equals(userId)) {
            throw ReviewException.SESSION_NOT_OWNED.toException();
        }
    }

    private void validateSessionMatchId(ReviewSessionEntity session, UUID matchId) {
        if (!session.getInteraction().getMatch().getId().equals(matchId)) {
            throw ReviewException.MATCH_NOT_FOUND.toException();
        }
    }

    private void validateSessionMode(ReviewSessionEntity session, ReviewMode expectedMode) {
        if (session.getMode() != expectedMode) {
            throw ReviewException.SESSION_MODE_MISMATCH.toException();
        }
    }

    private void validateSessionStatus(ReviewSessionEntity session, ReviewSessionStatus expectedStatus) {
        if (session.getStatus() != expectedStatus) {
            throw ReviewException.INVALID_SESSION_STATUS.toException();
        }
    }

    private void validateSatisfaction(int satisfaction) {
        if (satisfaction < 1 || satisfaction > 5) {
            throw ReviewException.INVALID_SATISFACTION.toException();
        }
    }

    private String generateAiReviewContent(List<AiReviewQuestionEntity> questions) {
        List<ReviewAiClient.QuestionAnswer> answers = questions.stream()
                .map(q -> new ReviewAiClient.QuestionAnswer(q.getQuestion(), q.getAnswer()))
                .toList();
        return reviewAiClient.generateReviewContent(answers);
    }

    /**
     * 회고 완료 기록. 새 트랜잭션(REQUIRES_NEW)에서 실행하여
     * 호출자의 1L 캐시에 있는 stale interaction 객체와 격리한다.
     * 새 PersistenceContext + PESSIMISTIC_WRITE 락으로 최신 데이터를 읽어 Lost Update 방지.
     */
    private void markReviewCompleted(UUID matchId, UUID userId) {
        TransactionTemplate requiresNewTx = new TransactionTemplate(transactionManager);
        requiresNewTx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        requiresNewTx.executeWithoutResult(status -> {
            InteractionEntity interaction = interactionRepository.findByMatchIdForUpdate(matchId)
                    .orElseThrow(ReviewException.INTERACTION_NOT_FOUND::toException);

            List<String> completedBy = interaction.getReviewCompletedBy();
            if (completedBy == null) {
                completedBy = new ArrayList<>();
            }
            String userIdStr = userId.toString();
            if (!completedBy.contains(userIdStr)) {
                completedBy = new ArrayList<>(completedBy);
                completedBy.add(userIdStr);
                interaction.setReviewCompletedBy(completedBy);

                experienceGrantPort.grantReviewWriteExp(userId);
            }

            // 양쪽 모두 회고 완료 시 상호작용 + 매칭 완료 처리
            if (completedBy.size() >= 2) {
                interaction.setStageStatus(StageStatus.COMPLETED);

                MatchEntity match = interaction.getMatch();
                match.setStatus(MatchStatus.COMPLETED);
                matchRepository.save(match);

                log.info("양쪽 회고 모두 완료 - 상호작용 및 매칭 완료 처리: interactionId={}, matchId={}",
                        interaction.getId(), match.getId());
            }

            interactionRepository.save(interaction);
            log.debug("회고 완료 기록: interactionId={}, userId={}, 완료 인원={}",
                    interaction.getId(), userId, completedBy.size());
        });
    }
}
