package com.ilgiyebo.domain.diary.service;

import com.ilgiyebo.domain.diary.entity.EmotionTag;

/**
 * AI 서버 연동 전 사용하는 스텁 구현체.
 * AI 서버 URL이 설정되지 않은 경우 DiaryAiClientConfig에서 이 구현체를 사용한다.
 */
public class StubDiaryAiClient implements DiaryAiClient {

    @Override
    public String generateFirstQuestion(FirstQuestionInput input) {
        return "오늘 하루는 어떠셨나요? 가장 기억에 남는 순간이 있다면 알려주세요.";
    }

    @Override
    public NextQuestionResult generateNextQuestion(NextQuestionInput input) {
        int turnCount = input.conversationHistory().size();

        if (turnCount >= 4) {
            return new NextQuestionResult(true, null, "충분한 답변 수집");
        }

        String question = switch (turnCount) {
            case 1 -> "그 순간에 어떤 감정을 느꼈나요?";
            case 2 -> "오늘 새롭게 배우거나 깨달은 것이 있나요?";
            case 3 -> "내일은 어떤 하루가 되었으면 좋겠나요?";
            default -> "오늘 하루를 한 문장으로 정리한다면?";
        };

        return new NextQuestionResult(false, question, null);
    }

    @Override
    public GeneratedDiaryResult generateDiaryContent(DiaryContentInput input) {
        StringBuilder content = new StringBuilder();
        content.append("오늘의 일기\n\n");

        for (ConversationTurn turn : input.conversationHistory()) {
            content.append(turn.answer()).append("\n\n");
        }

        ProfileUpdate profileUpdate = new ProfileUpdate(
                java.util.List.of("일기 작성"),
                java.util.List.of("자기 성찰")
        );

        return new GeneratedDiaryResult(content.toString().trim(), EmotionTag.CALM, profileUpdate);
    }
}
