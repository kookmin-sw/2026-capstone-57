package com.ilgiyebo.domain.auth.service;

import com.ilgiyebo.common.config.AuthProperties;
import com.ilgiyebo.common.config.JwtTokenProvider;
import com.ilgiyebo.domain.matching.entity.SlotEntity;
import com.ilgiyebo.domain.matching.entity.SlotStatus;
import com.ilgiyebo.domain.user.entity.PersonalityType;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.auth.exception.AuthException;
import com.ilgiyebo.domain.auth.dto.*;
import com.ilgiyebo.domain.auth.entity.VerificationEntry;
import com.ilgiyebo.domain.matching.repository.SlotRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final SlotRepository slotRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final AuthProperties authProperties;

    private final ConcurrentHashMap<String, VerificationEntry> verificationStore = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public VerificationResponse sendVerification(String email) {
        validateEmailDomain(email);

        if (userRepository.existsByEmail(email)) {
            throw AuthException.EMAIL_ALREADY_EXISTS.toException();
        }

        String verificationId = UUID.randomUUID().toString();
        String code = generateVerificationCode();
        Instant expiresAt = Instant.now().plus(authProperties.getVerificationExpiryMinutes(), ChronoUnit.MINUTES);

        VerificationEntry entry = new VerificationEntry(verificationId, email, code, expiresAt);
        verificationStore.put(verificationId, entry);

        emailService.sendVerificationEmail(email, code);

        return new VerificationResponse(verificationId);
    }

    @Override
    public VerificationConfirmResponse confirmVerification(String verificationId, String code) {
        VerificationEntry entry = getValidEntry(verificationId);

        entry.incrementAttemptCount();

        if (!entry.getCode().equals(code)) {
            throw AuthException.VERIFICATION_CODE_MISMATCH.toException();
        }

        entry.setVerified(true);

        return VerificationConfirmResponse.from(entry);
    }

    @Override
    @Transactional
    public AuthTokenResponse signup(SignupRequest request) {
        VerificationEntry entry = verificationStore.get(request.verificationId());
        if (entry == null) {
            throw AuthException.VERIFICATION_NOT_FOUND.toException();
        }

        if (!entry.isVerified()) {
            throw AuthException.VERIFICATION_NOT_VERIFIED.toException();
        }

        if (entry.isExpired()) {
            verificationStore.remove(request.verificationId());
            throw AuthException.VERIFICATION_EXPIRED.toException();
        }

        verificationStore.remove(request.verificationId());

        String university = resolveUniversity(entry.getEmail());

        UserEntity user = UserEntity.builder()
                .email(entry.getEmail())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .name(request.name())
                .university(university)
                .major(request.major())
                .studentId(request.studentId())
                .birthDate(request.birthDate())
                .gender(request.gender())
                .hobbies(request.hobbies())
                .interests(request.interests())
                .personalityType(
                        request.personalityType() != null
                                ? PersonalityType.valueOf(request.personalityType())
                                : null
                )
                .idealTypes(request.idealTypes())
                .build();
        user = userRepository.save(user);

        // 초기 슬롯 1개 자동 부여
        SlotEntity slot = SlotEntity.builder()
                .user(user)
                .status(SlotStatus.EMPTY)
                .build();
        slotRepository.save(slot);
        log.info("회원가입 완료 및 초기 슬롯 부여: userId={}", user.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthTokenResponse.from(user.getId(), accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResponse login(String email, String password) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(AuthException.AUTHENTICATION_FAILED::toException);

        if (user.isSuspended()) {
            throw AuthException.ACCOUNT_SUSPENDED.toException();
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw AuthException.AUTHENTICATION_FAILED.toException();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return AuthTokenResponse.from(user.getId(), accessToken, refreshToken);
    }

    public String resolveUniversity(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        Map<String, String> domainMap = authProperties.getUniversityDomainMap();
        if (domainMap.containsKey(domain)) {
            return domainMap.get(domain);
        }
        for (Map.Entry<String, String> e : domainMap.entrySet()) {
            if (domain.endsWith("." + e.getKey())) {
                return e.getValue();
            }
        }
        return domain;
    }

    public void validateEmailDomain(String email) {
        if (email == null || !email.contains("@")) {
            throw AuthException.INVALID_EMAIL_FORMAT.toException();
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        boolean allowed = authProperties.getAllowedDomains().stream()
                .anyMatch(d -> domain.endsWith(d.toLowerCase()));
        if (!allowed) {
            throw AuthException.INVALID_EMAIL_DOMAIN.toException();
        }
    }

    private VerificationEntry getValidEntry(String verificationId) {
        VerificationEntry entry = verificationStore.get(verificationId);
        if (entry == null) {
            throw AuthException.VERIFICATION_NOT_FOUND.toException();
        }
        if (entry.isExpired()) {
            verificationStore.remove(verificationId);
            throw AuthException.VERIFICATION_EXPIRED.toException();
        }
        if (entry.getAttemptCount() >= MAX_VERIFICATION_ATTEMPTS) {
            verificationStore.remove(verificationId);
            throw AuthException.VERIFICATION_ATTEMPTS_EXCEEDED.toException();
        }
        return entry;
    }

    private String generateVerificationCode() {
        int code = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    ConcurrentHashMap<String, VerificationEntry> getVerificationStore() {
        return verificationStore;
    }
}
