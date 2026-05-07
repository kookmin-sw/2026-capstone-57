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

        TargetProfile targetProfile = new TargetProfile(
                partner.getName(),
                partner.getNickname(),
                partner.getUniversity(),
                partner.getMajor(),
                partner.getHobbies(),
                partner.getInterests(),
                partner.getPersonalityTypes()
        );

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

        // 유저의 답안(quizAnswer)을 저장하기 위해 새로운 리스트 생성 및 채점
        List<QuizQuestionDto> updatedQuestions = new ArrayList<>();
        int correctCount = 0;

        for (int i = 0; i < totalCount; i++) {
            QuizQuestionDto oldQ = originalQuestions.get(i);
            Integer userAnswer = userAnswers.get(i);

            // Record는 불변이므로 새로 생성하여 quizAnswer 부분만 채움
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

        // 유저 답안이 포함된 퀴즈 데이터를 DB에 다시 업데이트
        interaction.setQuizData(updatedQuestions);
        interactionRepository.save(interaction);

        interactionService.completeQuiz(matchId, userId);

        String partnerSummary = String.format("정답 개수: %d/%d", correctCount, totalCount);

        return new QuizResponseDto(
                matchId,
                updatedQuestions, // 업데이트된 퀴즈 목록을 반환
                true,
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