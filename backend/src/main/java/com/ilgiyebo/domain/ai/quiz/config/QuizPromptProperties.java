package com.ilgiyebo.domain.ai.quiz.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "quiz.generation")
public class QuizPromptProperties {
    private int questionCount = 5;
    private String promptTemplate;
}
