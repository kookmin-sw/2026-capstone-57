package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.InteractionStateDto;
import com.ilgiyebo.domain.interaction.dto.InteractionStateDto.*;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import com.ilgiyebo.domain.interaction.entity.TerminationReason;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
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
public class InteractionServiceImpl implements InteractionService {

    private final InteractionRepository interactionRepository;
    private final MatchRepository matchRepository;
    private final InteractionNotificationPublisher notificationPublisher;

    @Override
    @Transactional(readOnly = true)
    public InteractionStateDto getInteractionState(UUID matchId, UUID userId) {
        MatchEntity match = findMatch(matchId);
        validateUserInMatch(match, userId);
        InteractionEntity interaction = findInteraction(matchId);
        return toDto(interaction, match);
    }

    @Override
    @Transactional
    public void terminateMatch(UUID matchId, TerminationReason reason) {
        MatchEntity match = findMatch(matchId);
        InteractionEntity interaction = findInteraction(matchId);

        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            return;
        }

        interaction.setStageStatus(StageStatus.TERMINATED);
        interaction.setTerminationReason(reason.name());
        match.setStatus(MatchStatus.TERMINATED);

        matchRepository.save(match);
        interactionRepository.save(interaction);

        notificationPublisher.publishMatchTerminated(
            matchId, match.getUserA().getId(), match.getUserB().getId(), reason);
        log.info("Match terminated: matchId={}, reason={}", matchId, reason);
    }

    @Override
    @Transactional
    public InteractionStateDto completeQuiz(UUID matchId, UUID userId) {
        MatchEntity match = findMatch(matchId);
        validateUserInMatch(match, userId);
        InteractionEntity interaction = findInteraction(matchId);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.QUIZ_NOT_COMPLETED.toException();
        }

        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            throw InteractionException.ALREADY_TERMINATED.toException();
        }

        List<String> completedBy = interaction.getQuizCompletedBy();
        if (completedBy == null) {
            completedBy = new ArrayList<>();
        }

        String userIdStr = userId.toString();
        if (!completedBy.contains(userIdStr)) {
            completedBy = new ArrayList<>(completedBy);
            completedBy.add(userIdStr);
            interaction.setQuizCompletedBy(completedBy);
        }

        if (completedBy.size() >= 2) {
            int completedStage = interaction.getCurrentStage();
            interaction.setCurrentStage(2);
            interaction.setStageStatus(StageStatus.IN_PROGRESS);
            interactionRepository.save(interaction);

            notificationPublisher.publishStageCompleted(
                matchId, userId, completedStage, 2);
        } else {
            interaction.setStageStatus(StageStatus.WAITING);
            interactionRepository.save(interaction);
        }

        return toDto(interaction, match);
    }

    @Override
    @Transactional
    public void storeQuizData(UUID matchId, List<com.ilgiyebo.domain.interaction.dto.QuizQuestionDto> quizData) {
        InteractionEntity interaction = findInteraction(matchId);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }

        interaction.setQuizData(quizData);
        interactionRepository.save(interaction);
        log.info("Quiz data stored via SQS: matchId={}, questionCount={}", matchId, quizData.size());
    }

    // --- Private helpers ---
    private MatchEntity findMatch(UUID matchId) {
        return matchRepository.findById(matchId)
            .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
    }

    private InteractionEntity findInteraction(UUID matchId) {
        return interactionRepository.findByMatchId(matchId)
            .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw InteractionException.USER_NOT_IN_MATCH.toException();
        }
    }

    private InteractionStateDto toDto(InteractionEntity interaction, MatchEntity match) {
        InteractionStateDto.StageDataDto stageData = buildStageData(interaction);
        return new InteractionStateDto(
            interaction.getId(),
            interaction.getMatch().getId(),
            interaction.getCurrentStage(),
            interaction.getStageStatus(),
            match.getCycleStartDate(),
            match.getCycleEndDate(),
            stageData
        );
    }

    private InteractionStateDto.StageDataDto buildStageData(InteractionEntity interaction) {
        return switch (interaction.getCurrentStage()) {
            case 1 -> new QuizData(
                interaction.getQuizCompletedBy() != null ? interaction.getQuizCompletedBy() : List.of());
            case 2 -> new ChatData(
                interaction.getChatStartTime(), interaction.getChatEndTime());
            case 3 -> new GameData(
                interaction.getGameType(), interaction.isGameCompleted());
            case 4 -> new MissionData(
                interaction.getMissionId(),
                interaction.getMissionConfirmedBy() != null ? interaction.getMissionConfirmedBy() : List.of(),
                interaction.isMissionExtended());
            case 5 -> new ReviewData(
                interaction.getReviewCompletedBy() != null ? interaction.getReviewCompletedBy() : List.of());
            default -> new QuizData(List.of());
        };
    }
}
