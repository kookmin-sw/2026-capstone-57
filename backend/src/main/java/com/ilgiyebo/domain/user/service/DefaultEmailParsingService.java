package com.ilgiyebo.service;

import com.ilgiyebo.dto.ParsedEmailInfo;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * MVP 단계 기본 이메일 파싱 서비스.
 * 비활성화 상태로, 사용자가 이름과 전공을 직접 입력한다.
 * SES 도입 후 대학별 파싱 규칙을 구현한 서비스로 교체 예정.
 */
@Service
public class DefaultEmailParsingService implements EmailParsingService {

    @Override
    public Optional<ParsedEmailInfo> parseEmail(String email) {
        return Optional.empty();
    }

    @Override
    public boolean hasParsingRule(String universityDomain) {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
