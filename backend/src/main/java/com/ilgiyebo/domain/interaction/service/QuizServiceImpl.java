package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage.UserProfile;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionResponse;
import com.ilgiyebo.domain.interaction.dto.QuizResponseDto;
import com.ilgiyebo.domain.interaction.dto.QuizSubmitRequest;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.entity.QuizAnswerEntity;
import com.ilgiyebo.domain.interaction.entity.QuizEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.interaction.repository.QuizAnswerRepository;
import com.ilgiyebo.domain.interaction.repository.QuizRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private static final long QUIZ_RETRY_TIMEOUT_MINUTES = 5;

    private final InteractionRepository interactionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final InteractionService interactionService;
    private final QuizRequestPublisher quizRequestPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> getQuiz(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = interactionRepository.findByMatchId(matchId)
                .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }

        // 상대방의 QuizEntity에서 문항을 가져온다
        UUID partnerId = match.getUserA().getId().equals(userId)
                ? match.getUserB().getId()
                : match.getUserA().getId();

        List<QuizEntity> quizzes = quizRepository.findByUserIdOrderByQuizIndex(partnerId);

        if (quizzes.isEmpty()) {
            // 퀴즈가 아직 없음 → 타임아웃 확인 후 재요청
            UserEntity partner = userRepository.findById(partnerId)
                    .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
            retryQuizGenerationIfNeeded(partner);
            throw InteractionException.QUIZ_GENERATING.toException();
        }

        return quizzes.stream()
                .map(q -> new QuizQuestionResponse(q.getQuizIndex(), q.getQuestion(), q.getOptions()))
                .toList();
    }

    @Override
    @Transactional
    public QuizResponseDto submitQuiz(UUID matchId, UUID userId, QuizSubmitRequest request) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = interactionRepository.findByMatchId(matchId)
                .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }

        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            throw InteractionException.ALREADY_TERMINATED.toException();
        }

        // 상대방의 퀴즈 문항 조회
        UUID partnerId = match.getUserA().getId().equals(userId)
                ? match.getUserB().getId()
                : match.getUserA().getId();

        List<QuizEntity> quizzes = quizRepository.findByUserIdOrderByQuizIndex(partnerId);
        if (quizzes.isEmpty()) {
            throw InteractionException.QUIZ_NOT_GENERATED.toException();
        }

        int quizIndex = request.quizIndex();
        int userAnswer = request.answer();

        // 해당 인덱스의 퀴즈 찾기
        QuizEntity targetQuiz = quizzes.stream()
                .filter(q -> q.getQuizIndex() == quizIndex)
                .findFirst()
                .orElseThrow(InteractionException.INVALID_QUIZ_INDEX::toException);

        // 중복 답안 방지
        if (quizAnswerRepository.existsByMatchIdAndUserIdAndQuizId(matchId, userId, targetQuiz.getId())) {
            throw InteractionException.INVALID_QUIZ_INDEX.toException();
        }

        // 답안 저장
        UserEntity answerUser = userRepository.findById(userId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);

        QuizAnswerEntity answer = QuizAnswerEntity.builder()
                .match(match)
                .user(answerUser)
                .quiz(targetQuiz)
                .userAnswer(userAnswer)
                .build();
        quizAnswerRepository.save(answer);

        // 현재까지의 답안 조회
        List<QuizAnswerEntity> myAnswers = quizAnswerRepository.findByMatchIdAndUserId(matchId, userId);

        int totalCount = quizzes.size();
        int correctCount = (int) myAnswers.stream()
                .filter(a -> a.getUserAnswer() == a.getQuiz().getCorrectAnswer())
                .count();

        boolean allCompleted = myAnswers.size() >= totalCount;

        // 모든 문항 완료 시 퀴즈 단계 완료 처리
        if (allCompleted) {
            interactionService.completeQuiz(matchId, userId);
        }

        return QuizResponseDto.from(
                matchId,
                quizIndex,
                targetQuiz.getCorrectAnswer(),
                userAnswer,
                correctCount,
                totalCount,
                allCompleted
        );
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw InteractionException.USER_NOT_IN_MATCH.toException();
        }
    }

    /**
     * 퀴즈가 없고 마지막 요청으로부터 일정 시간이 지났으면 재요청한다.
     * 요청 기록이 아예 없는 경우(회원가입 시 SQS 발행 실패)에도 즉시 재요청한다.
     */
    private void retryQuizGenerationIfNeeded(UserEntity user) {
        Instant requestedAt = user.getQuizRequestedAt();
        Instant now = Instant.now();

        if (requestedAt == null || requestedAt.plus(QUIZ_RETRY_TIMEOUT_MINUTES, ChronoUnit.MINUTES).isBefore(now)) {
            try {
                UserProfile profile = UserProfile.builder()
                        .name(user.getName())
                        .nickname(user.getNickname())
                        .university(user.getUniversity())
                        .major(user.getMajor())
                        .hobbies(user.getHobbies())
                        .interests(user.getInterests())
                        .personalityType(user.getPersonalityType() != null ? user.getPersonalityType().name() : null)
                        .build();

                QuizGenerateRequestMessage message = QuizGenerateRequestMessage.of(user.getId(), profile);
                quizRequestPublisher.requestQuizGeneration(message);

                user.setQuizRequestedAt(now);
                userRepository.save(user);

                log.info("퀴즈 재생성 요청 완료: userId={}", user.getId());
            } catch (Exception e) {
                log.warn("퀴즈 재생성 요청 실패: userId={}", user.getId(), e);
            }
        }
    }
}
