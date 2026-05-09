package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.InteractionStateDto;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.matching.entity.SlotEntity;
import com.ilgiyebo.domain.matching.entity.SlotStatus;
import com.ilgiyebo.domain.interaction.entity.TerminationReason;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.matching.repository.SlotRepository;
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
    private final SlotRepository slotRepository;

    @Override
    @Transactional(readOnly = true)
    public InteractionStateDto getInteractionState(UUID matchId, UUID userId) {
        MatchEntity match = findMatch(matchId);
        validateUserInMatch(match, userId);
        InteractionEntity interaction = findInteraction(matchId);

        // DTO의 from 메서드 사용
        return InteractionStateDto.from(interaction, match);
    }

    @Override
    @Transactional
    public void terminateMatch(UUID matchId, UUID userId, TerminationReason reason) {
        MatchEntity match = findMatch(matchId);
        validateUserInMatch(match, userId);
        InteractionEntity interaction = findInteraction(matchId);

        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            return;
        }

        interaction.setStageStatus(StageStatus.TERMINATED);
        interaction.setTerminationReason(reason.name());
        match.setStatus(MatchStatus.TERMINATED);

        // 슬롯 초기화: 양쪽 유저의 슬롯을 EMPTY로 되돌리고 매치 참조 제거
        SlotEntity slotA = match.getSlotA();
        SlotEntity slotB = match.getSlotB();

        slotA.setStatus(SlotStatus.EMPTY);
        slotA.setCurrentMatch(null);
        slotB.setStatus(SlotStatus.EMPTY);
        slotB.setCurrentMatch(null);

        slotRepository.save(slotA);
        slotRepository.save(slotB);
        matchRepository.save(match);
        interactionRepository.save(interaction);

        log.info("매칭 종료 완료: 매칭ID={}, 사유={}", matchId, reason);
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
            interaction.setCurrentStage(2);
            interaction.setStageStatus(StageStatus.IN_PROGRESS);
            interactionRepository.save(interaction);
        } else {
            interaction.setStageStatus(StageStatus.WAITING);
            interactionRepository.save(interaction);
        }

        // DTO의 from 메서드 사용
        return InteractionStateDto.from(interaction, match);
    }

    @Override
    @Transactional
    public void storeQuizData(UUID matchId, UUID requesterId, List<QuizQuestionDto> quizData) {
        MatchEntity match = findMatch(matchId);
        InteractionEntity interaction = findInteraction(matchId);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }

        boolean isUserA = match.getUserA().getId().equals(requesterId);
        if (isUserA) {
            interaction.setQuizDataA(quizData);
        } else {
            interaction.setQuizDataB(quizData);
        }

        interactionRepository.save(interaction);
        log.info("SQS를 통해 퀴즈 데이터 저장 완료: 매칭ID={}, 요청유저ID={}, 문항수={}",
                matchId, requesterId, quizData.size());
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
}