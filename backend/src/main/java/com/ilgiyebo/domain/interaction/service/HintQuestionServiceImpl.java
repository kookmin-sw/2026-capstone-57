package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.HintQuestionEntity;
import com.ilgiyebo.domain.interaction.entity.HintQuestionStatus;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.dto.HintQuestionDto;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.repository.HintQuestionRepository;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class HintQuestionServiceImpl implements HintQuestionService {

    private final HintQuestionRepository hintQuestionRepository;
    private final InteractionRepository interactionRepository;
    private final MatchRepository matchRepository;

    @Override
    @Transactional
    public HintQuestionDto sendHintQuestion(UUID matchId, UUID senderId, String question) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, senderId);

        InteractionEntity interaction = interactionRepository.findByMatchId(matchId)
                .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }
        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            throw InteractionException.ALREADY_TERMINATED.toException();
        }

        UserEntity sender = match.getUserA().getId().equals(senderId) ? match.getUserA() : match.getUserB();
        UserEntity responder = match.getUserA().getId().equals(senderId) ? match.getUserB() : match.getUserA();

        HintQuestionEntity entity = HintQuestionEntity.builder()
                .match(match)
                .sender(sender)
                .responder(responder)
                .question(question)
                .quizIndex(interaction.getCurrentStage())
                .status(HintQuestionStatus.PENDING)
                .build();

        entity = hintQuestionRepository.save(entity);

        log.info("힌트 질문 전송 완료: 매칭ID={}, 발신자ID={}, 질문ID={}", matchId, senderId, entity.getId());

        return HintQuestionDto.from(entity);
    }

    @Override
    @Transactional
    public HintQuestionDto answerHintQuestion(UUID matchId, UUID questionId, UUID responderId, String answer) {
        HintQuestionEntity entity = hintQuestionRepository.findById(questionId)
                .orElseThrow(InteractionException.HINT_QUESTION_NOT_FOUND::toException);

        // 해당 힌트 질문이 요청된 matchId에 속하는지 검증
        if (!entity.getMatch().getId().equals(matchId)) {
            throw InteractionException.HINT_QUESTION_NOT_FOUND.toException();
        }

        // 매칭 상태 검증 (종료된 매칭에서는 답변 불가)
        InteractionEntity interaction = interactionRepository.findByMatchId(matchId)
                .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);

        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            throw InteractionException.ALREADY_TERMINATED.toException();
        }

        if (!entity.getResponder().getId().equals(responderId)) {
            throw InteractionException.NOT_RESPONDER.toException();
        }
        if (entity.getStatus() == HintQuestionStatus.ANSWERED) {
            throw InteractionException.HINT_ALREADY_ANSWERED.toException();
        }

        entity.setAnswer(answer);
        entity.setStatus(HintQuestionStatus.ANSWERED);

        entity = hintQuestionRepository.save(entity);

        log.info("힌트 질문 답변 완료: 질문ID={}, 응답자ID={}", questionId, responderId);

        return HintQuestionDto.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HintQuestionDto> getHintQuestions(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        List<HintQuestionEntity> entities = hintQuestionRepository.findByMatchId(matchId);
        return entities.stream()
                .map(HintQuestionDto::from) // map 안에서 from 메서드 참조
                .toList();
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw InteractionException.USER_NOT_IN_MATCH.toException();
        }
    }
}