package com.ilgiyebo.service;

import com.ilgiyebo.common.exception.BusinessException;
import com.ilgiyebo.config.AuthProperties;
import com.ilgiyebo.config.JwtTokenProvider;
import com.ilgiyebo.domain.Gender;
import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.UserEntity;
import com.ilgiyebo.dto.AuthTokenResponse;
import com.ilgiyebo.dto.SignupRequest;
import com.ilgiyebo.dto.VerificationConfirmResponse;
import com.ilgiyebo.dto.VerificationResponse;
import com.ilgiyebo.repository.SlotRepository;
import com.ilgiyebo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private EmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        AuthProperties props = new AuthProperties();
        props.setAllowedDomains(List.of("ac.kr", "edu"));
        props.setVerificationExpiryMinutes(10);
        props.setUniversityDomainMap(Map.of(
                "kookmin.ac.kr", "국민대학교",
                "korea.ac.kr", "고려대학교",
                "snu.ac.kr", "서울대학교"
        ));
        authService = new AuthServiceImpl(
                userRepository, slotRepository, passwordEncoder, jwtTokenProvider,
                emailService, props);
    }

    // --- sendVerification ---

    @Test
    void sendVerification_validDomain_returnsVerificationIdAndSendsEmail() {
        when(userRepository.existsByEmail("test@kookmin.ac.kr")).thenReturn(false);
        VerificationResponse response = authService.sendVerification("test@kookmin.ac.kr");
        assertNotNull(response.verificationId());
        verify(emailService).sendVerificationEmail(eq("test@kookmin.ac.kr"), anyString());
    }

    @Test
    void sendVerification_invalidDomain_throwsBadRequest() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendVerification("test@gmail.com"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void sendVerification_existingEmail_throwsConflict() {
        when(userRepository.existsByEmail("test@kookmin.ac.kr")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendVerification("test@kookmin.ac.kr"));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void sendVerification_nullEmail_throwsBadRequest() {
        assertThrows(BusinessException.class, () -> authService.sendVerification(null));
    }

    // --- confirmVerification ---

    @Test
    void confirmVerification_validCode_returnsVerified() {
        when(userRepository.existsByEmail("test@kookmin.ac.kr")).thenReturn(false);
        VerificationResponse vr = authService.sendVerification("test@kookmin.ac.kr");
        String code = authService.getVerificationStore().get(vr.verificationId()).getCode();

        VerificationConfirmResponse response = authService.confirmVerification(vr.verificationId(), code);

        assertTrue(response.verified());
        assertEquals("test@kookmin.ac.kr", response.email());
    }

    @Test
    void confirmVerification_wrongCode_throwsUnauthorized() {
        when(userRepository.existsByEmail("test@kookmin.ac.kr")).thenReturn(false);
        VerificationResponse vr = authService.sendVerification("test@kookmin.ac.kr");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.confirmVerification(vr.verificationId(), "000000"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void confirmVerification_nonExistent_throwsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.confirmVerification("non-existent", "123456"));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void confirmVerification_exceededAttempts_throwsTooManyRequests() {
        when(userRepository.existsByEmail("test@kookmin.ac.kr")).thenReturn(false);
        VerificationResponse vr = authService.sendVerification("test@kookmin.ac.kr");

        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class,
                    () -> authService.confirmVerification(vr.verificationId(), "000000"));
        }

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.confirmVerification(vr.verificationId(), "000000"));
        assertEquals(429, ex.getStatus().value());
    }

    // --- signup ---

    @Test
    void signup_afterVerification_createsUserWithProfileAndSlot() {
        when(userRepository.existsByEmail("kimari@kookmin.ac.kr")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            if (u.getId() == null) return u.toBuilder().id(UUID.randomUUID()).build();
            return u;
        });
        when(slotRepository.save(any(SlotEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");

        // Step 1: send
        VerificationResponse vr = authService.sendVerification("kimari@kookmin.ac.kr");
        String code = authService.getVerificationStore().get(vr.verificationId()).getCode();

        // Step 2: confirm
        authService.confirmVerification(vr.verificationId(), code);

        // Step 3: signup with profile
        SignupRequest request = new SignupRequest(
                vr.verificationId(), "password123", "김아리", "김아리",
                "소프트웨어전공", "20210001",
                LocalDate.of(2001, 3, 15), Gender.FEMALE,
                List.of("READING", "MOVIE"), List.of("TECHNOLOGY", "MUSIC_GENRE"),
                List.of("INTROVERTED", "CREATIVE"), List.of("KIND", "FUNNY")
        );
        AuthTokenResponse response = authService.signup(request);

        assertNotNull(response.userId());
        assertEquals("access-token", response.token());

        // 유저 생성 검증
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();
        assertEquals("kimari@kookmin.ac.kr", saved.getEmail());
        assertEquals("국민대학교", saved.getUniversity());
        assertEquals("김아리", saved.getName());
        assertEquals("소프트웨어전공", saved.getMajor());
        assertEquals(LocalDate.of(2001, 3, 15), saved.getBirthDate());
        assertEquals(Gender.FEMALE, saved.getGender());
        assertEquals(List.of("READING", "MOVIE"), saved.getHobbies());
        assertEquals(List.of("INTROVERTED", "CREATIVE"), saved.getPersonalityTypes());
        assertEquals(List.of("KIND", "FUNNY"), saved.getIdealTypes());

        // 초기 슬롯 부여 검증
        verify(slotRepository).save(any(SlotEntity.class));
    }

    @Test
    void signup_withoutVerification_throwsForbidden() {
        when(userRepository.existsByEmail("test@kookmin.ac.kr")).thenReturn(false);
        VerificationResponse vr = authService.sendVerification("test@kookmin.ac.kr");

        // Skip confirm, go straight to signup
        SignupRequest request = new SignupRequest(
                vr.verificationId(), "pw", "nick", "이름",
                "전공", "12345",
                LocalDate.of(2000, 1, 1), Gender.MALE,
                null, null, null, null
        );
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(request));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void signup_unknownDomain_usesRawDomainAsUniversity() {
        when(userRepository.existsByEmail("test@unknown.ac.kr")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            if (u.getId() == null) return u.toBuilder().id(UUID.randomUUID()).build();
            return u;
        });
        when(slotRepository.save(any(SlotEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("t");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("r");

        VerificationResponse vr = authService.sendVerification("test@unknown.ac.kr");
        String code = authService.getVerificationStore().get(vr.verificationId()).getCode();
        authService.confirmVerification(vr.verificationId(), code);

        SignupRequest request = new SignupRequest(
                vr.verificationId(), "pw", "닉네임", "이름",
                "전공", "12345",
                LocalDate.of(2000, 1, 1), Gender.MALE,
                null, null, null, null
        );
        authService.signup(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertEquals("unknown.ac.kr", captor.getValue().getUniversity());
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsToken() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(userId).email("test@kookmin.ac.kr").passwordHash("hashed")
                .nickname("test").university("국민대학교").isSuspended(false).build();

        when(userRepository.findByEmail("test@kookmin.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(userId)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(userId)).thenReturn("refresh-token");

        AuthTokenResponse response = authService.login("test@kookmin.ac.kr", "password123");
        assertEquals(userId.toString(), response.userId());
    }

    @Test
    void login_suspendedAccount_throwsForbidden() {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID()).email("test@kookmin.ac.kr").passwordHash("hashed")
                .nickname("test").university("국민대학교").isSuspended(true).build();

        when(userRepository.findByEmail("test@kookmin.ac.kr")).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("test@kookmin.ac.kr", "password123"));
        assertEquals(403, ex.getStatus().value());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID()).email("test@kookmin.ac.kr").passwordHash("hashed")
                .nickname("test").university("국민대학교").isSuspended(false).build();

        when(userRepository.findByEmail("test@kookmin.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("test@kookmin.ac.kr", "wrong"));
        assertEquals(401, ex.getStatus().value());
    }

    @Test
    void login_nonExistentEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("nobody@kookmin.ac.kr")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("nobody@kookmin.ac.kr", "password"));
        assertEquals(401, ex.getStatus().value());
    }

    // --- resolveUniversity ---

    @Test
    void resolveUniversity_kookminDomain_returns국민대학교() {
        assertEquals("국민대학교", authService.resolveUniversity("kimari@kookmin.ac.kr"));
    }

    @Test
    void resolveUniversity_subDomain_matchesParent() {
        assertEquals("국민대학교", authService.resolveUniversity("test@mail.kookmin.ac.kr"));
    }

    // --- validateEmailDomain ---

    @Test
    void validateEmailDomain_acKrSubdomain_allowed() {
        assertDoesNotThrow(() -> authService.validateEmailDomain("user@snu.ac.kr"));
    }

    @Test
    void validateEmailDomain_eduDomain_allowed() {
        assertDoesNotThrow(() -> authService.validateEmailDomain("user@stanford.edu"));
    }
}
