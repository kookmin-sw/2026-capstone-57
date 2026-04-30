package com.ilgiyebo.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Interest {
    TECHNOLOGY("IT/기술"),
    STARTUP("스타트업"),
    DESIGN("디자인"),
    ECONOMICS("경제/금융"),
    PSYCHOLOGY("심리학"),
    PHILOSOPHY("철학"),
    HISTORY("역사"),
    SCIENCE("과학"),
    ENVIRONMENT("환경/지속가능성"),
    POLITICS("시사/정치"),
    LITERATURE("문학"),
    ART("미술/예술"),
    FILM("영화/영상"),
    KPOP("K-POP"),
    ANIME("애니메이션"),
    SPORTS("스포츠"),
    FOOD("맛집/음식"),
    LANGUAGE("외국어"),
    SELF_DEVELOPMENT("자기계발"),
    MEDITATION("명상/마음챙김"),
    ASTROLOGY("별자리/MBTI"),
    SOCIAL_MEDIA("SNS/콘텐츠"),
    ENTREPRENEURSHIP("창업"),
    EDUCATION("교육"),
    HEALTH("건강/웰빙"),
    MUSIC_GENRE("작곡/작사"),
    ARCHITECTURE("건축"),
    ROBOTICS("로봇공학"),
    AI_ML("인공지능/머신러닝"),
    SPACE("우주/천문학");

    private final String label;
}
