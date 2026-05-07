package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage.AiQuizQuestion;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizResponseListener {

    private final InteractionService interactionService;
    private final ObjectMapper objectMapper;

    @SqsListener("${quiz.response.queue:ai-quiz-response-queue}")
    public void handleQuizResponse(String messageJson) {
        try {
            QuizGenerateResponseMessage response = objectMapper.readValue(
                    messageJson, QuizGenerateResponseMessage.class);

            if (!"SUCCESS".equals(response.status())) {
                log.warn("AI 퀴즈 생성 실패: 매칭ID={}, 상태={}",
                        response.matchId(), response.status()); // 👈 한글 로그로 변경
                return;
            }

            if (response.quiz() == null || response.quiz().questions() == null
                    || response.quiz().questions().isEmpty()) {
                log.warn("AI 퀴즈 응답에 문항 데이터가 없습니다: 매칭ID={}", response.matchId()); // 👈 한글 로그로 변경
                return;
            }

            List<QuizQuestionDto> quizData = response.quiz().questions().stream()
                    .map(this::toQuizQuestionDto)
                    .toList();

            UUID matchId = UUID.fromString(response.matchId());
            interactionService.storeQuizData(matchId, quizData);

            log.info("AI 퀴즈 응답 수신 및 DB 저장 완료: 매칭ID={}, 문항수={}",
                    response.matchId(), quizData.size()); // 👈 한글 로그로 변경

        } catch (Exception e) {
            log.error("AI 퀴즈 응답 처리 중 서버 에러 발생: {}", messageJson, e);
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