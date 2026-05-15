package com.ilgiyebo.domain.review.service;

import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int REVIEW_STAGE = 5;
    private static final int AI_QUESTION_COUNT = 3;

    private final ReviewRepository reviewRepository;
    private final ReviewSessionRepository reviewSessionRepository;
    private final AiReviewQuestionRepository aiReviewQuestionRepository;
    private final MatchRepository matchRepository;
    private final InteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final ReviewAiClient reviewAiClient;
    private final ExperienceGrantPort experienceGrantPort;

    @Override
    @Transactional
    public ReviewSessionResponse selectReviewMode(UUID matchId, UUID userId, ReviewMode mode) {
        MatchEntity match = findMatchOrThrow(matchId);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = findInteractionOrThrow(matchId);
        validateReviewStage(interaction);

        // 이미 회고가 작성된 경우
        if (reviewRepository.findByInteractionIdAndUserId(interaction.getId(), userId).isPresent()) {
            throw ReviewException.REVIEW_ALREADY_EXISTS.toException();
        }

        // 이미 세션이 존재하는 경우
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

        // AI 모드인 경우 질문 생성 (추후 AI 연동)
        if (mode == ReviewMode.AI_ASSISTED) {
            generateAiQuestions(session.getId());
        }

        log.debug("회고 모드 선택 완료: matchId={}, userId={}, mode={}, sessionId={}",
                matchId, userId, mode, session.getId());

        return ReviewSessionResponse.from(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiReviewQuestionDto> getAIQuestions(UUID sessionId, UUID userId) {
        ReviewSessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);

        List<AiReviewQuestionEntity> questions =
                aiReviewQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        log.debug("AI 회고 질문 조회: sessionId={}, 질문 수={}", sessionId, questions.size());

        return questions.stream()
                .map(AiReviewQuestionDto::from)
                .toList();
    }

    @Override
    @Transactional
    public AiReviewQuestionDto answerAIQuestion(UUID sessionId, UUID questionId, UUID userId, String answer) {
        ReviewSessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
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
    @Transactional
    public GeneratedReviewPreview generateReview(UUID sessionId, UUID userId) {
        ReviewSessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
        validateSessionStatus(session, ReviewSessionStatus.IN_PROGRESS);

        // DIRECT 모드 세션에서는 AI 생성 불가
        if (session.getMode() != ReviewMode.AI_ASSISTED) {
            throw ReviewException.INVALID_SESSION_STATUS.toException();
        }

        // 모든 질문에 답변했는지 확인
        List<AiReviewQuestionEntity> questions =
                aiReviewQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);
        boolean allAnswered = questions.stream().allMatch(q -> q.getAnswer() != null);
        if (!allAnswered) {
            throw ReviewException.NOT_ALL_QUESTIONS_ANSWERED.toException();
        }

        // AI 회고 내용 생성
        String generatedContent = generateAiReviewContent(questions);
        int suggestedSatisfaction = 4;

        // 세션 상태 업데이트
        session.setStatus(ReviewSessionStatus.GENERATED);
        reviewSessionRepository.save(session);

        log.debug("AI 회고 생성 완료: sessionId={}", sessionId);

        return new GeneratedReviewPreview(sessionId, generatedContent, suggestedSatisfaction);
    }

    @Override
    @Transactional
    public ReviewResponse editGeneratedReview(UUID sessionId, UUID userId, EditReviewRequest request) {
        ReviewSessionEntity session = findSessionOrThrow(sessionId);
        validateSessionOwner(session, userId);
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

        // 세션 상태 완료 처리
        session.setStatus(ReviewSessionStatus.COMPLETED);
        reviewSessionRepository.save(session);

        // 상호작용에 회고 완료 기록
        markReviewCompleted(interaction, userId);

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

        // 이미 회고가 작성된 경우
        if (reviewRepository.findByInteractionIdAndUserId(interaction.getId(), userId).isPresent()) {
            throw ReviewException.REVIEW_ALREADY_EXISTS.toException();
        }

        validateSatisfaction(review.satisfaction());

        UserEntity user = findUserOrThrow(userId);

        // 이미 AI 모드로 세션이 생성된 경우 직접 작성 불가
        Optional<ReviewSessionEntity> existingSession = reviewSessionRepository
                .findByInteractionIdAndUserId(interaction.getId(), userId);
        if (existingSession.isPresent() && existingSession.get().getMode() == ReviewMode.AI_ASSISTED) {
            throw ReviewException.SESSION_MODE_MISMATCH.toException();
        }

        // 직접 작성 세션이 없으면 생성
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

        // 세션 상태 완료 처리
        session.setStatus(ReviewSessionStatus.COMPLETED);
        reviewSessionRepository.save(session);

        // 상호작용에 회고 완료 기록
        markReviewCompleted(interaction, userId);

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
        boolean isParticipant = match.getUserA().getId().equals(userId)
                || match.getUserB().getId().equals(userId);
        if (!isParticipant) {
            throw ReviewException.USER_NOT_IN_MATCH.toException();
        }
    }

    private void validateReviewStage(InteractionEntity interaction) {
        if (interaction.getCurrentStage() != REVIEW_STAGE) {
            throw ReviewException.NOT_IN_REVIEW_STAGE.toException();
        }
    }

    private void validateSessionOwner(ReviewSessionEntity session, UUID userId) {
        if (!session.getUser().getId().equals(userId)) {
            throw ReviewException.SESSION_NOT_OWNED.toException();
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

    private void generateAiQuestions(UUID sessionId) {
        // 세션에서 interaction → mission 정보를 가져와 AI에 전달
        ReviewSessionEntity session = findSessionOrThrow(sessionId);
        InteractionEntity interaction = session.getInteraction();

        String missionDescription = "만남 미션";
        String missionLocation = "캠퍼스";

        // 실제 미션 정보가 있으면 사용
        if (interaction.getMission() != null) {
            missionDescription = interaction.getMission().getDescription() != null
                    ? interaction.getMission().getDescription() : missionDescription;
            missionLocation = interaction.getMission().getLocation() != null
                    ? interaction.getMission().getLocation() : missionLocation;
        }

        List<String> questions = reviewAiClient.generateReviewQuestions(missionDescription, missionLocation);

        for (int i = 0; i < questions.size(); i++) {
            AiReviewQuestionEntity question = AiReviewQuestionEntity.builder()
                    .sessionId(sessionId)
                    .question(questions.get(i))
                    .questionOrder(i + 1)
                    .build();
            aiReviewQuestionRepository.save(question);
        }

        log.debug("AI 회고 질문 생성 완료: sessionId={}, 질문 수={}, 미션장소={}", sessionId, questions.size(), missionLocation);
    }

    private String generateAiReviewContent(List<AiReviewQuestionEntity> questions) {
        List<ReviewAiClient.QuestionAnswer> answers = questions.stream()
                .map(q -> new ReviewAiClient.QuestionAnswer(q.getQuestion(), q.getAnswer()))
                .toList();
        return reviewAiClient.generateReviewContent(answers);
    }

    private void markReviewCompleted(InteractionEntity interaction, UUID userId) {
        List<String> completedBy = interaction.getReviewCompletedBy();
        if (completedBy == null) {
            completedBy = new ArrayList<>();
        }
        String userIdStr = userId.toString();
        if (!completedBy.contains(userIdStr)) {
            completedBy = new ArrayList<>(completedBy);
            completedBy.add(userIdStr);
            interaction.setReviewCompletedBy(completedBy);

            // 경험치 부여 (요구사항 9.7) — 중복 방지: 최초 완료 시에만 부여
            experienceGrantPort.grantReviewWriteExp(userId);
        }

        // 양쪽 모두 회고 완료 시 상호작용 완료 처리
        if (completedBy.size() >= 2) {
            interaction.setStageStatus(com.ilgiyebo.domain.interaction.entity.StageStatus.COMPLETED);
            log.info("양쪽 회고 모두 완료 - 상호작용 완료 처리: interactionId={}", interaction.getId());
        }

        interactionRepository.save(interaction);
        log.debug("회고 완료 기록: interactionId={}, userId={}, 완료 인원={}",
                interaction.getId(), userId, completedBy.size());
    }
}
