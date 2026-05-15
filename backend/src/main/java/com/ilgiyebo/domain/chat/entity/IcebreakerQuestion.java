package com.ilgiyebo.domain.chat.entity;

import java.util.Random;

public enum IcebreakerQuestion {

    MOVIE("요즘 가장 재밌게 본 영화나 드라마에 대해서 얘기해볼까요?"),
    FOOD("좋아하는 음식이나 최근에 맛있게 먹은 거 있어요?"),
    TRAVEL("가장 가보고 싶은 여행지가 어디예요?"),
    MUSIC("요즘 자주 듣는 노래나 아티스트 있어요?"),
    HOBBY("쉬는 날에는 보통 뭐 하면서 시간 보내요?"),
    SEASON("가장 좋아하는 계절은 언제예요? 이유도 궁금해요!"),
    BUCKET_LIST("올해 꼭 해보고 싶은 거 하나만 말해본다면?"),
    CHILDHOOD("어릴 때 꿈이 뭐였어요?"),
    SUPERPOWER("하루만 초능력을 쓸 수 있다면 뭘 하고 싶어요?"),
    MORNING("아침형 인간이에요, 저녁형 인간이에요?"),
    PET("반려동물 키우고 있어요? 아니면 키워보고 싶은 동물 있어요?"),
    BOOK("최근에 읽은 책이나 인상 깊었던 책 있어요?"),
    STRESS("스트레스 받을 때 주로 어떻게 풀어요?"),
    COMFORT_FOOD("기분이 안 좋을 때 꼭 먹는 위로 음식 있어요?"),
    WEEKEND("이번 주말 계획 있어요?");

    private final String question;

    private static final IcebreakerQuestion[] VALUES = values();
    private static final Random RANDOM = new Random();

    IcebreakerQuestion(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }

    public static IcebreakerQuestion random() {
        return VALUES[RANDOM.nextInt(VALUES.length)];
    }
}
