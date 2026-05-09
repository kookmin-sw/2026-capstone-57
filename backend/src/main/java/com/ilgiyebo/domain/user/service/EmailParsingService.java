package com.ilgiyebo.service;

import com.ilgiyebo.dto.ParsedEmailInfo;

import java.util.Optional;

/**
 * 대학 이메일 주소에서 이름과 전공을 파싱하는 서비스.
 * 대학별 이메일 형식이 다르므로 파싱 규칙을 대학별로 관리한다.
 * 예: "홍길동_컴퓨터공학과@university.ac.kr" → 이름: 홍길동, 전공: 컴퓨터공학과
 *
 * MVP 단계에서는 비활성화 상태이며, SES 도입 후 활성화 예정.
 */
public interface EmailParsingService {

    /** 이메일 주소에서 이름과 전공 파싱 시도 */
    Optional<ParsedEmailInfo> parseEmail(String email);

    /** 특정 대학 도메인에 대한 파싱 규칙 존재 여부 확인 */
    boolean hasParsingRule(String universityDomain);

    /** 이메일 파싱 서비스 활성화 여부 */
    boolean isEnabled();
}
