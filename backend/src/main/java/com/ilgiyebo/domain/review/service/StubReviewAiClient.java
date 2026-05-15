package com.ilgiyebo.domain.review.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 로컬 개발용 스텁 AI 클라이언트.
 * 실제 AI 서버 없이 고정된 질문과 회고 내용을 반환한다.
 */
@Slf4j
@Component
@Profile("local")
public class StubReviewAiClient implements ReviewAiClient {

    @Override
    public List<String> generateReviewQuestions(String missionDescription, String missionLocation) {
        log.debug("스텁 AI 회고 질문 생성: 미션설명={}, 미션장소={}", missionDescription, missionLocation);
        return List.of(
                "이번 만남에서 가장 기억에 남는 순간은 무엇인가요?",
                "상대방과의 대화에서 새롭게 알게 된 점이 있나요?",
                "다음에 다시 만난다면 어떤 활동을 함께 하고 싶나요?"
        );
    }

    @Override
    public String generateReviewContent(List<QuestionAnswer> answers) {
        log.debug("스텁 AI 회고 글 생성: 답변 수={}", answers.size());
        StringBuilder sb = new StringBuilder();
        sb.append("이번 만남을 돌아보며:\n\n");
        for (QuestionAnswer qa : answers) {
            sb.append("Q: ").append(qa.question()).append("\n");
            sb.append("A: ").append(qa.answer()).append("\n\n");
        }
        sb.append("전반적으로 좋은 시간이었습니다.");
        return sb.toString();
    }
}
