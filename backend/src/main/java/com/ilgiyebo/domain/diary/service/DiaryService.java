package com.ilgiyebo.domain.diary.service;

import com.ilgiyebo.domain.diary.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DiaryService {

    /** 일기 작성 (upsert: 같은 날짜에 이미 존재하면 업데이트) */
    DiaryEntryResponse createEntry(UUID userId, DiaryInputDto input);

    /** 일기 목록 조회 (본인만 접근 가능) */
    Page<DiaryEntryResponse> getEntries(UUID userId, Pageable pageable);

    /** 연속 작성 일수 조회 */
    StreakInfoDto getStreak(UUID userId);

    /** 감정 변화 추이 조회 */
    List<EmotionTrendDto> getEmotionTrend(UUID userId, String period);
}
