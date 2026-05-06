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

/**
 * AI가 생성한 퀴즈 결과를 SQS에서 수신하는 Consumer.
 * 수신된 퀴즈 데이터를 InteractionEntity에 저장한다.
 */
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
                log.warn("AI 퀴즈 생성 실패 응답: matchId={}, status={}",
                    response.matchId(), response.status());
                return;
            }

            if (response.quiz() == null || response.quiz().questions() == null
                    || response.quiz().questions().isEmpty()) {
                log.warn("AI 퀴즈 응답에 문항이 없음: matchId={}", response.matchId());
                return;
            }

            // AI 응답 형식 → 내부 QuizQuestionDto로 변환
            List<QuizQuestionDto> quizData = response.quiz().questions().stream()
                .map(this::toQuizQuestionDto)
                .toList();

            UUID matchId = UUID.fromString(response.matchId());
            interactionService.storeQuizData(matchId, quizData);

            log.info("AI 퀴즈 응답 수신 및 저장 완료: matchId={}, questionCount={}",
                response.matchId(), quizData.size());

        } catch (Exception e) {
            log.error("AI 퀴즈 응답 처리 실패: {}", messageJson, e);
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
