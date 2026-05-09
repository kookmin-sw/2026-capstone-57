package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage.AiQuizQuestion;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuizResponseListener {

    private final InteractionService interactionService;
    private final ObjectMapper objectMapper;

    @SqsListener("${cloud.aws.sqs.quiz-response-queue}")
    public void handleQuizResponse(String messageJson) {
        try {
            QuizGenerateResponseMessage response = objectMapper.readValue(
                    messageJson, QuizGenerateResponseMessage.class);

            if (!"SUCCESS".equals(response.status())) {
                log.warn("AI 퀴즈 생성 실패: 매칭ID={}, 상태={}",
                        response.matchId(), response.status());
                return;
            }

            if (response.quiz() == null || response.quiz().questions() == null
                    || response.quiz().questions().isEmpty()) {
                log.warn("AI 퀴즈 응답에 문항 데이터가 없습니다: 매칭ID={}", response.matchId());
                return;
            }

            List<AiQuizQuestion> aiQuestions = response.quiz().questions();
            List<QuizQuestionDto> quizData = new ArrayList<>();

            for (int i = 0; i < aiQuestions.size(); i++) {
                AiQuizQuestion aiQ = aiQuestions.get(i);
                quizData.add(new QuizQuestionDto(
                        i + 1,                // quizIndex: 1부터 시작
                        aiQ.questionText(),
                        aiQ.choices(),
                        aiQ.correctIndex(),
                        null                  // quizAnswer: 유저가 아직 풀기 전이므로 null
                ));
            }

            UUID matchId = UUID.fromString(response.matchId());
            UUID requesterId = UUID.fromString(response.requesterId());
            interactionService.storeQuizData(matchId, requesterId, quizData);

            log.info("AI 퀴즈 응답 수신 및 DB 저장 완료: 매칭ID={}, 문항수={}",
                    response.matchId(), quizData.size());

        } catch (Exception e) {
            log.error("AI 퀴즈 응답 처리 중 서버 에러 발생: {}", messageJson, e);
        }
    }
}