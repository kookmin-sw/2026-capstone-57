package com.ilgiyebo.domain.review.service;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * ExperienceService 미구현 시 사용되는 No-Op 폴백.
 * 경험치 부여 로직이 구현되면 이 빈은 자동으로 대체된다.
 * ReviewConfig에서 @ConditionalOnMissingBean으로 등록됨.
 */
@Slf4j
public class NoOpExperienceGrantPort implements ExperienceGrantPort {

    @Override
    public void grantReviewWriteExp(UUID userId) {
        log.info("경험치 부여 대기 중 (ExperienceService 미구현): userId={}, activity=REVIEW_WRITE", userId);
    }
}
