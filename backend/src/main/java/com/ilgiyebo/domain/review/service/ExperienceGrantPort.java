package com.ilgiyebo.domain.review.service;

import java.util.UUID;

/**
 * 경험치 부여를 위한 포트 인터페이스.
 * ExperienceService가 구현되면 이 인터페이스를 구현하여 연동한다.
 * review 도메인은 exp 도메인에 직접 의존하지 않는다.
 */
public interface ExperienceGrantPort {

    /** 회고 작성 완료 시 경험치 부여 */
    void grantReviewWriteExp(UUID userId);
}
