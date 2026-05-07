package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.HintQuestionEntity;
import com.ilgiyebo.domain.interaction.entity.HintQuestionStatus;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.UserEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.dto.HintQuestionDto;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.repository.HintQuestionRepository;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
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
    private final InteractionNotificationPublisher notificationPublisher;

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

        // 💡 연관관계 저장을 위해 발신자/수신자 UserEntity 객체 가져오기
        UserEntity sender = match.getUserA().getId().equals(senderId) ? match.getUserA() : match.getUserB();
        UserEntity responder = match.getUserA().getId().equals(senderId) ? match.getUserB() : match.getUserA();

        HintQuestionEntity entity = HintQuestionEntity.builder()
                .match(match)       // 👈 객체 주입
                .sender(sender)     // 👈 객체 주입
                .responder(responder) // 👈 객체 주입
                .question(question)
                .status(HintQuestionStatus.PENDING)
                .build();

        entity = hintQuestionRepository.save(entity);

        notificationPublisher.publishHintQuestionReceived(responder.getId(), matchId, entity.getId());
        log.info("Hint question sent: matchId={}, senderId={}, questionId={}", matchId, senderId, entity.getId());

        return toDto(entity);
    }

    @Override
    @Transactional
    public HintQuestionDto answerHintQuestion(UUID questionId, UUID responderId, String answer) {
        HintQuestionEntity entity = hintQuestionRepository.findById(questionId)
                .orElseThrow(InteractionException.HINT_QUESTION_NOT_FOUND::toException);

        if (!entity.getResponder().getId().equals(responderId)) { // 👈 Getter 변경
            throw InteractionException.NOT_RESPONDER.toException();
        }
        if (entity.getStatus() == HintQuestionStatus.ANSWERED) {
            throw InteractionException.HINT_ALREADY_ANSWERED.toException();
        }

        entity.setAnswer(answer);
        entity.setStatus(HintQuestionStatus.ANSWERED);
        // ❌ entity.setAnsweredAt(Instant.now()); 삭제됨

        entity = hintQuestionRepository.save(entity);

        notificationPublisher.publishHintAnswerReceived(entity.getSender().getId(), entity.getMatch().getId(), questionId); // 👈 Getter 변경
        log.info("Hint question answered: questionId={}, responderId={}", questionId, responderId);

        return toDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HintQuestionDto> getHintQuestions(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        List<HintQuestionEntity> entities = hintQuestionRepository.findByMatchId(matchId);
        return entities.stream()
                .map(this::toDto)
                .toList();
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw InteractionException.USER_NOT_IN_MATCH.toException();
        }
    }

    private HintQuestionDto toDto(HintQuestionEntity entity) {
        return new HintQuestionDto(
                entity.getId(),
                entity.getMatch().getId(), // 👈 Getter 변경
                entity.getSender().getId(), // 👈 Getter 변경
                entity.getResponder().getId(), // 👈 Getter 변경
                entity.getQuestion(),
                entity.getAnswer(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}