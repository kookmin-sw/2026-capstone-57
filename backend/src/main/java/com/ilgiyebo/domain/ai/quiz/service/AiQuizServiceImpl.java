package com.ilgiyebo.domain.ai.quiz.service;

import com.ilgiyebo.domain.ai.quiz.dto.BedrockQuizResponse;
import com.ilgiyebo.domain.ai.quiz.dto.QuizRequestMessage;
import com.ilgiyebo.domain.ai.quiz.dto.QuizResponseMessage;
import com.ilgiyebo.domain.ai.quiz.exception.AiResponseParseException;
import com.ilgiyebo.domain.ai.exception.BedrockInvocationException;
import com.ilgiyebo.domain.ai.service.BedrockClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuizServiceImpl implements AiQuizService {

    private final PromptTemplateManager promptTemplateManager;
    private final BedrockClientService bedrockClientService;
    private final AiResponseParser aiResponseParser;

    @Override
    public QuizResponseMessage generateQuiz(QuizRequestMessage request) {
        // 1. 요청에 포함된 프로필로 프롬프트 생성
        String prompt = promptTemplateManager.buildPrompt(request.targetProfile());

        // 2. Bedrock 호출 (1회 재시도)
        BedrockQuizResponse parsedResponse;
        String status;

        try {
            String response = invokeWithRetry(prompt);
            parsedResponse = aiResponseParser.parseToResponse(response);
            status = "SUCCESS";
        } catch (BedrockInvocationException | AiResponseParseException e) {
            log.warn("Bedrock 호출/파싱 실패, 폴백 퀴즈 사용: {}", e.getMessage());
            parsedResponse = promptTemplateManager.getFallbackQuizResponse(request.targetProfile());
            status = "FALLBACK";
        }

        // 3. 응답 구성
        Instant now = Instant.now();
        List<QuizResponseMessage.QuestionItem> questions = parsedResponse.questions().stream()
                .map(item -> new QuizResponseMessage.QuestionItem(
                        item.questionText(),
                        item.choices(),
                        item.correctIndex(),
                        item.explanation()
                ))
                .toList();

        QuizResponseMessage.QuizData quizData = new QuizResponseMessage.QuizData(
                request.matchId(),
                request.targetUserId(),
                questions,
                now
        );

        return new QuizResponseMessage(
                "QUIZ_GENERATED",
                status,
                request.matchId(),
                request.requesterId(),
                request.targetUserId(),
                quizData,
                questions.size(),
                now
        );
    }

    private String invokeWithRetry(String prompt) {
        try {
            return bedrockClientService.invokeModel(prompt);
        } catch (BedrockInvocationException e) {
            log.warn("Bedrock 첫 번째 호출 실패, 재시도: {}", e.getMessage());
            return bedrockClientService.invokeModel(prompt);
        }
    }
}
