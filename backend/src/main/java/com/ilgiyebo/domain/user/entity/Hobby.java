package com.ilgiyebo.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Hobby {
    READING("독서"),
    MOVIE("영화 감상"),
    MUSIC("음악 감상"),
    COOKING("요리"),
    BAKING("베이킹"),
    TRAVEL("여행"),
    PHOTOGRAPHY("사진 촬영"),
    DRAWING("그림 그리기"),
    GAMING("게임"),
    FITNESS("헬스/운동"),
    RUNNING("러닝"),
    HIKING("등산"),
    SWIMMING("수영"),
    YOGA("요가"),
    CYCLING("자전거"),
    DANCING("댄스"),
    SINGING("노래"),
    INSTRUMENT("악기 연주"),
    WRITING("글쓰기"),
    CRAFTING("공예/DIY"),
    GARDENING("원예"),
    CAMPING("캠핑"),
    FISHING("낚시"),
    BOARD_GAME("보드게임"),
    PUZZLE("퍼즐"),
    CAFE_HOPPING("카페 탐방"),
    FASHION("패션"),
    SKINCARE("스킨케어"),
    PET("반려동물"),
    VOLUNTEERING("봉사활동");

    private final String label;
}
