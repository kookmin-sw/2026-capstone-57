package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.UserEntity;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage.TargetProfile;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.ilgiyebo.domain.interaction.dto.QuizResponseDto;
import com.ilgiyebo.domain.interaction.dto.QuizSubmitRequest;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
import com.ilgiyebo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 1. DB has quiz -> return immediately
        if (interaction.getQuizData() != null && !interaction.getQuizData().isEmpty()) {
            return interaction.getQuizData();
        }

        // 2. No quiz in DB -> request AI generation via SQS
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
        log.info("AI quiz generation requested: matchId={}, targetUserId={}", matchId, partnerId);

        // 3. Quiz not ready yet -> 202 Accepted
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

        List<QuizQuestionDto> questions = interaction.getQuizData();
        if (questions == null || questions.isEmpty()) {
            throw InteractionException.QUIZ_NOT_GENERATED.toException();
        }

        List<Integer> userAnswers = request.answers();
        int totalCount = questions.size();

        if (userAnswers.size() != totalCount) {
            throw InteractionException.QUIZ_ANSWER_COUNT_MISMATCH.toException();
        }

        int correctCount = 0;
        for (int i = 0; i < totalCount; i++) {
            if (userAnswers.get(i) == questions.get(i).correctAnswer()) {
                correctCount++;
            }
        }

        interactionService.completeQuiz(matchId, userId);

        String partnerSummary = String.format("Score: %d/%d", correctCount, totalCount);

        return new QuizResponseDto(
            matchId,
            questions,
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
