package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.UserEntity;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage.TargetProfile;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.ilgiyebo.domain.interaction.dto.QuizResponseDto;
import com.ilgiyebo.domain.interaction.dto.QuizSubmitRequest;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
import com.ilgiyebo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final InteractionRepository interactionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final InteractionService interactionService;
    private final QuizRequestPublisher quizRequestPublisher;

    @Override
    @Transactional
    public List<QuizQuestionDto> getQuiz(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = interactionRepository.findByMatchId(matchId)
                .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }

        // 1. DB에 퀴즈가 이미 존재하면 즉시 반환
        if (interaction.getQuizData() != null && !interaction.getQuizData().isEmpty()) {
            return interaction.getQuizData();
        }

        // 2. DB에 퀴즈가 없으면 SQS를 통해 AI 생성 요청
        UUID partnerId = match.getUserA().getId().equals(userId)
                ? match.getUserB().getId()
                : match.getUserA().getId();

        UserEntity partner = userRepository.findById(partnerId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);

        // 파라미터가 많은 객체 생성을 빌더 패턴으로 가독성 있게 수정
        TargetProfile targetProfile = TargetProfile.builder()
                .name(partner.getName())
                .nickname(partner.getNickname())
                .university(partner.getUniversity())
                .major(partner.getMajor())
                .hobbies(partner.getHobbies())
                .interests(partner.getInterests())
                .personalityType(partner.getPersonalityTypes())
                .build();

        quizRequestPublisher.requestQuizGeneration(matchId, userId, partnerId, targetProfile);
        log.info("AI 퀴즈 생성 요청 완료: 매칭ID={}, 대상유저ID={}", matchId, partnerId);

        // 3. 퀴즈가 아직 준비되지 않음 (생성 중 예외 발생)
        throw InteractionException.QUIZ_GENERATING.toException();
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

        List<QuizQuestionDto> originalQuestions = interaction.getQuizData();
        if (originalQuestions == null || originalQuestions.isEmpty()) {
            throw InteractionException.QUIZ_NOT_GENERATED.toException();
        }

        List<Integer> userAnswers = request.answers();
        int totalCount = originalQuestions.size();

        if (userAnswers.size() != totalCount) {
            throw InteractionException.QUIZ_ANSWER_COUNT_MISMATCH.toException();
        }

        List<QuizQuestionDto> updatedQuestions = new ArrayList<>();
        int correctCount = 0;

        for (int i = 0; i < totalCount; i++) {
            QuizQuestionDto oldQ = originalQuestions.get(i);
            Integer userAnswer = userAnswers.get(i);

            QuizQuestionDto updatedQ = new QuizQuestionDto(
                    oldQ.quizIndex(),
                    oldQ.question(),
                    oldQ.options(),
                    oldQ.correctAnswer(),
                    userAnswer
            );
            updatedQuestions.add(updatedQ);

            if (userAnswer == oldQ.correctAnswer()) {
                correctCount++;
            }
        }

        interaction.setQuizData(updatedQuestions);
        interactionRepository.save(interaction);

        interactionService.completeQuiz(matchId, userId);

        String partnerSummary = String.format("정답 개수: %d/%d", correctCount, totalCount);

        // from 정적 팩토리 메서드 사용
        return QuizResponseDto.from(
                matchId,
                updatedQuestions,
                correctCount,
                totalCount,
                partnerSummary
        );
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw InteractionException.USER_NOT_IN_MATCH.toException();
        }
    }
}