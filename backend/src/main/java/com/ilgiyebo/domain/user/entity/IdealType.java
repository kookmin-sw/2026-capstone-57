package com.ilgiyebo.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IdealType {
    KIND("다정한"),
    FUNNY("재밌는"),
    SMART("똑똑한"),
    HONEST("솔직한"),
    CONFIDENT("자신감 있는"),
    GENTLE("젠틀한"),
    ACTIVE("활동적인"),
    QUIET("조용한"),
    ROMANTIC("로맨틱한"),
    RESPONSIBLE("책임감 있는"),
    GOOD_LISTENER("경청을 잘하는"),
    AMBITIOUS("야망 있는"),
    ARTISTIC("예술적인"),
    SPORTY("운동을 좋아하는"),
    FOODIE("맛집을 좋아하는"),
    TRAVELER("여행을 좋아하는"),
    BOOKWORM("책을 좋아하는"),
    ANIMAL_LOVER("동물을 좋아하는"),
    FASHIONABLE("패션 감각 있는"),
    POSITIVE("긍정적인");

    private final String label;
}
