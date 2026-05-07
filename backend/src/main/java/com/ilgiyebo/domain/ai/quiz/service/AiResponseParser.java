package com.ilgiyebo.domain.ai.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.domain.ai.quiz.dto.BedrockQuizResponse;
import com.ilgiyebo.domain.ai.quiz.exception.AiResponseParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public BedrockQuizResponse parseToResponse(String jsonResponse) {
        try {
            String cleanedJson = stripMarkdownCodeBlock(jsonResponse);
            BedrockQuizResponse response = objectMapper.readValue(cleanedJson, BedrockQuizResponse.class);

            if (response == null || response.questions() == null || response.questions().isEmpty()) {
                throw new AiResponseParseException(jsonResponse, "응답에 questions 필드가 없거나 비어있습니다");
            }

            for (int i = 0; i < response.questions().size(); i++) {
                BedrockQuizResponse.QuestionItem item = response.questions().get(i);
                if (item.choices() == null || item.choices().size() != 4) {
                    throw new AiResponseParseException(jsonResponse,
                        "문제 " + (i + 1) + "의 선택지가 4개가 아닙니다");
                }
                if (item.correctIndex() < 0 || item.correctIndex() > 3) {
                    throw new AiResponseParseException(jsonResponse,
                        "문제 " + (i + 1) + "의 정답 인덱스가 범위를 벗어났습니다");
                }
            }

            return response;
        } catch (AiResponseParseException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패: {}", e.getMessage(), e);
            throw new AiResponseParseException(jsonResponse, "JSON 파싱 실패: " + e.getMessage());
        }
    }

    private String stripMarkdownCodeBlock(String response) {
        if (response == null) return "";
        String trimmed = response.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
