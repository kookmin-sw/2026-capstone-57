package com.ilgiyebo.domain.user.dto;

/**
 * 대학 이메일 주소에서 파싱된 이름과 전공 정보.
 */
public record ParsedEmailInfo(
    String name,
    String major
) {}
