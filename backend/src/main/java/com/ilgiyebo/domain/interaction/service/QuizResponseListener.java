package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage.AiQuizQuestion;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizResponseListener {

    private final InteractionService interactionService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${quiz.response.queue:ai.quiz.response}")
    public void handleQuizResponse(String messageJson) {
        try {
            QuizGenerateResponseMessage response = objectMapper.readValue(
                messageJson, QuizGenerateResponseMessage.class);

            if (!"SUCCESS".equals(response.status())) {
                log.warn("AI quiz generation failed: matchId={}, status={}",
                    response.matchId(), response.status());
                return;
            }

            if (response.quiz() == null || response.quiz().questions() == null
                    || response.quiz().questions().isEmpty()) {
                log.warn("AI quiz response has no questions: matchId={}", response.matchId());
                return;
            }

            List<QuizQuestionDto> quizData = response.quiz().questions().stream()
                .map(this::toQuizQuestionDto)
                .toList();

            UUID matchId = UUID.fromString(response.matchId());
            interactionService.storeQuizData(matchId, quizData);

            log.info("AI quiz response received and stored: matchId={}, questionCount={}",
                response.matchId(), quizData.size());

        } catch (Exception e) {
            log.error("Failed to process AI quiz response: {}", messageJson, e);
        }
    }

    private QuizQuestionDto toQuizQuestionDto(AiQuizQuestion aiQuestion) {
        return new QuizQuestionDto(
            aiQuestion.questionText(),
            aiQuestion.choices(),
            aiQuestion.correctIndex(),
            aiQuestion.explanation()
        );
    }
}
