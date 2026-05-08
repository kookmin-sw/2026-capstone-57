package com.ilgiyebo.domain.ai.quiz.service;

import com.ilgiyebo.domain.ai.quiz.config.QuizPromptProperties;
import com.ilgiyebo.domain.ai.quiz.dto.BedrockQuizResponse;
import com.ilgiyebo.domain.ai.quiz.dto.QuizRequestMessage.TargetProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PromptTemplateManager {

    private final QuizPromptProperties quizPromptProperties;

    public String buildPrompt(TargetProfile profile) {
        String template = quizPromptProperties.getPromptTemplate();

        template = template.replace("{{name}}", nullSafe(profile.name()));
        template = template.replace("{{nickname}}", nullSafe(profile.nickname()));
        template = template.replace("{{university}}", nullSafe(profile.university()));
        template = template.replace("{{major}}", nullSafe(profile.major()));
        template = template.replace("{{hobbies}}", listToString(profile.hobbies()));
        template = template.replace("{{interests}}", listToString(profile.interests()));
        template = template.replace("{{personalityType}}", listToString(profile.personalityType()));
        template = template.replace("{{questionCount}}", String.valueOf(quizPromptProperties.getQuestionCount()));

        return template;
    }

    public BedrockQuizResponse getFallbackQuizResponse(TargetProfile profile) {
        String name = nullSafe(profile.name());
        String major = nullSafe(profile.major());
        String university = nullSafe(profile.university());
        String hobbies = listToString(profile.hobbies());
        String interests = listToString(profile.interests());
        String personalityType = listToString(profile.personalityType());

        List<BedrockQuizResponse.QuestionItem> questions = List.of(
            new BedrockQuizResponse.QuestionItem(name + "님의 전공은 무엇일까요?",
                List.of(major, "경영학과", "심리학과", "컴퓨터공학과"), 0,
                name + "님은 " + university + "에서 " + major + "을(를) 전공하고 있습니다"),
            new BedrockQuizResponse.QuestionItem(name + "님의 취미로 올바른 것은?",
                List.of("등산", hobbies, "요리", "독서"), 1,
                name + "님의 취미는 " + hobbies + "입니다"),
            new BedrockQuizResponse.QuestionItem(name + "님이 관심을 가지고 있는 분야는?",
                List.of("패션", "스포츠", interests, "음악"), 2,
                name + "님은 " + interests + "에 관심이 있습니다"),
            new BedrockQuizResponse.QuestionItem(name + "님의 성격 유형은 무엇일까요?",
                List.of("ISTJ", "ENFP", personalityType, "INTP"), 2,
                name + "님의 성격 유형은 " + personalityType + "입니다"),
            new BedrockQuizResponse.QuestionItem(name + "님의 대학교는 어디일까요?",
                List.of("서울대학교", "연세대학교", "고려대학교", university), 3,
                name + "님은 " + university + "에 재학 중입니다")
        );

        return new BedrockQuizResponse(questions);
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "정보 없음";
    }

    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "정보 없음";
        }
        return String.join(", ", list);
    }
}
