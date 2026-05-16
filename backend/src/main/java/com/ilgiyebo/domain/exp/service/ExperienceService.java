package com.ilgiyebo.domain.exp.service;

import com.ilgiyebo.domain.exp.dto.ExpGrantDto;
import com.ilgiyebo.domain.exp.dto.ExpHistoryEntryDto;
import com.ilgiyebo.domain.exp.dto.ExperienceInfoDto;
import com.ilgiyebo.domain.exp.dto.LevelUpResultDto;
import com.ilgiyebo.domain.exp.entity.ExpActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ExperienceService {

    /** 활동별 경험치 부여 */
    ExpGrantDto grantExperience(UUID userId, ExpActivity activity);

    /** 누적 경험치, 현재 레벨, 다음 레벨까지 필요 경험치 조회 */
    ExperienceInfoDto getExperienceInfo(UUID userId);

    /** 경험치 획득 내역 조회 (Pageable) */
    Page<ExpHistoryEntryDto> getExpHistory(UUID userId, Pageable pageable);

    /** 레벨업 조건 확인 및 보상 처리 (슬롯 해금 등) */
    Optional<LevelUpResultDto> checkAndProcessLevelUp(UUID userId);
}
