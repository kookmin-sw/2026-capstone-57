package com.ilgiyebo.domain.exp.service;

import com.ilgiyebo.domain.exp.dto.ExpGrantDto;
import com.ilgiyebo.domain.exp.dto.ExpHistoryEntryDto;
import com.ilgiyebo.domain.exp.dto.ExperienceInfoDto;
import com.ilgiyebo.domain.exp.dto.LevelUpResultDto;
import com.ilgiyebo.domain.exp.entity.ExpActivity;
import com.ilgiyebo.domain.exp.entity.ExpHistoryEntity;
import com.ilgiyebo.domain.exp.entity.RewardType;
import com.ilgiyebo.domain.exp.repository.ExpHistoryRepository;
import com.ilgiyebo.domain.matching.service.MatchingService;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceImplTest {

    @Mock
    private ExpHistoryRepository expHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MatchingService matchingService;

    @InjectMocks
    private ExperienceServiceImpl experienceService;

    private UUID userId;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = UserEntity.builder()
                .email("test@university.ac.kr")
                .passwordHash("hash")
                .nickname("테스트")
                .university("테스트대학교")
                .totalExp(0)
                .currentLevel(1)
                .build();
        // BaseSchema의 id는 prePersist에서 설정되므로 리플렉션으로 설정
        try {
            var idField = user.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("grantExperience - 일기 작성 시 10 경험치를 부여한다")
    void grantExperience_diaryWrite() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(expHistoryRepository.countByUserIdAndActivitySince(eq(userId), eq(ExpActivity.DIARY_WRITE), any()))
                .thenReturn(0L);
        when(expHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExpGrantDto result = experienceService.grantExperience(userId, ExpActivity.DIARY_WRITE);

        assertThat(result.activity()).isEqualTo(ExpActivity.DIARY_WRITE);
        assertThat(result.amount()).isEqualTo(10);
        assertThat(result.bonusAmount()).isEqualTo(0);
        assertThat(result.newTotal()).isEqualTo(10);
        verify(expHistoryRepository).save(any(ExpHistoryEntity.class));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("grantExperience - 미션 완료 시 30 경험치를 부여한다")
    void grantExperience_missionComplete() {
        user.setTotalExp(50);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(expHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExpGrantDto result = experienceService.grantExperience(userId, ExpActivity.MISSION_COMPLETE);

        assertThat(result.activity()).isEqualTo(ExpActivity.MISSION_COMPLETE);
        assertThat(result.amount()).isEqualTo(30);
        assertThat(result.newTotal()).isEqualTo(80);
    }

    @Test
    @DisplayName("grantExperience - 일기 작성 일일 제한 초과 시 예외를 던진다")
    void grantExperience_dailyLimitExceeded() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(expHistoryRepository.countByUserIdAndActivitySince(eq(userId), eq(ExpActivity.DIARY_WRITE), any()))
                .thenReturn(1L); // 이미 1회 지급됨

        assertThatThrownBy(() -> experienceService.grantExperience(userId, ExpActivity.DIARY_WRITE))
                .isInstanceOf(com.ilgiyebo.common.exception.BusinessException.class)
                .hasMessageContaining("한도를 초과");

        verify(expHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("grantExperience - 플래너 작성 일일 제한 초과 시 예외를 던진다")
    void grantExperience_plannerDailyLimitExceeded() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(expHistoryRepository.countByUserIdAndActivitySince(eq(userId), eq(ExpActivity.PLANNER_WRITE), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> experienceService.grantExperience(userId, ExpActivity.PLANNER_WRITE))
                .isInstanceOf(com.ilgiyebo.common.exception.BusinessException.class);

        verify(expHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("grantExperience - 힌트 답변 3회 미만이면 정상 지급된다")
    void grantExperience_hintAnswerWithinLimit() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(expHistoryRepository.countByUserIdAndActivitySince(eq(userId), eq(ExpActivity.HINT_ANSWER), any()))
                .thenReturn(2L); // 3회 미만
        when(expHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExpGrantDto result = experienceService.grantExperience(userId, ExpActivity.HINT_ANSWER);

        assertThat(result.activity()).isEqualTo(ExpActivity.HINT_ANSWER);
        assertThat(result.amount()).isEqualTo(5);
    }

    @Test
    @DisplayName("grantExperience - 힌트 답변 3회 초과 시 예외를 던진다")
    void grantExperience_hintAnswerDailyLimitExceeded() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(expHistoryRepository.countByUserIdAndActivitySince(eq(userId), eq(ExpActivity.HINT_ANSWER), any()))
                .thenReturn(3L);

        assertThatThrownBy(() -> experienceService.grantExperience(userId, ExpActivity.HINT_ANSWER))
                .isInstanceOf(com.ilgiyebo.common.exception.BusinessException.class);
    }

    @Test
    @DisplayName("grantExperience - 제한 없는 활동(미션)은 여러 번 지급 가능하다")
    void grantExperience_noLimitActivity() {
        user.setTotalExp(50);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // MISSION_COMPLETE는 DAILY_LIMITS에 없으므로 countByUserIdAndActivitySince 호출 안 함
        when(expHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExpGrantDto result = experienceService.grantExperience(userId, ExpActivity.MISSION_COMPLETE);

        assertThat(result.amount()).isEqualTo(30);
        verify(expHistoryRepository, never()).countByUserIdAndActivitySince(any(), any(), any());
    }

    @Test
    @DisplayName("grantExperience - 존재하지 않는 사용자에 대해 예외를 던진다")
    void grantExperience_userNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> experienceService.grantExperience(userId, ExpActivity.DIARY_WRITE))
                .isInstanceOf(com.ilgiyebo.common.exception.BusinessException.class);
    }

    @Test
    @DisplayName("getExperienceInfo - 레벨 1 사용자의 경험치 정보를 조회한다")
    void getExperienceInfo_level1() {
        user.setTotalExp(50);
        user.setCurrentLevel(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ExperienceInfoDto result = experienceService.getExperienceInfo(userId);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.totalExp()).isEqualTo(50);
        assertThat(result.currentLevel()).isEqualTo(1);
        assertThat(result.expToNextLevel()).isEqualTo(50); // 100 - 50
        assertThat(result.currentLevelExp()).isEqualTo(50); // 50 - 0
    }

    @Test
    @DisplayName("getExperienceInfo - 레벨 3 사용자의 경험치 정보를 조회한다")
    void getExperienceInfo_level3() {
        user.setTotalExp(300);
        user.setCurrentLevel(3);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ExperienceInfoDto result = experienceService.getExperienceInfo(userId);

        assertThat(result.totalExp()).isEqualTo(300);
        assertThat(result.currentLevel()).isEqualTo(3);
        assertThat(result.expToNextLevel()).isEqualTo(150); // 450 - 300
        assertThat(result.currentLevelExp()).isEqualTo(50); // 300 - 250
    }

    @Test
    @DisplayName("getExpHistory - 경험치 내역을 페이지네이션으로 조회한다")
    void getExpHistory() {
        Pageable pageable = PageRequest.of(0, 10);
        ExpHistoryEntity entity = ExpHistoryEntity.builder()
                .user(user)
                .activity(ExpActivity.DIARY_WRITE)
                .amount(10)
                .bonusAmount(0)
                .build();
        try {
            var idField = entity.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, UUID.randomUUID());
            var createdAtField = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(entity, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Page<ExpHistoryEntity> page = new PageImpl<>(List.of(entity), pageable, 1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(expHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

        Page<ExpHistoryEntryDto> result = experienceService.getExpHistory(userId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).activity()).isEqualTo(ExpActivity.DIARY_WRITE);
        assertThat(result.getContent().get(0).amount()).isEqualTo(10);
    }

    @Test
    @DisplayName("checkAndProcessLevelUp - 레벨업 조건 미충족 시 빈 Optional을 반환한다")
    void checkAndProcessLevelUp_noLevelUp() {
        user.setTotalExp(50);
        user.setCurrentLevel(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Optional<LevelUpResultDto> result = experienceService.checkAndProcessLevelUp(userId);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("checkAndProcessLevelUp - 레벨업 시 레벨을 업데이트한다")
    void checkAndProcessLevelUp_levelUp() {
        user.setTotalExp(100);
        user.setCurrentLevel(1);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<LevelUpResultDto> result = experienceService.checkAndProcessLevelUp(userId);

        assertThat(result).isPresent();
        assertThat(result.get().newLevel()).isEqualTo(2);
        assertThat(user.getCurrentLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("checkAndProcessLevelUp - 레벨 3 달성 시 슬롯 해금 보상을 부여한다")
    void checkAndProcessLevelUp_slotUnlock() {
        user.setTotalExp(250);
        user.setCurrentLevel(2);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchingService.unlockSlot(userId)).thenReturn(null);

        Optional<LevelUpResultDto> result = experienceService.checkAndProcessLevelUp(userId);

        assertThat(result).isPresent();
        assertThat(result.get().newLevel()).isEqualTo(3);
        assertThat(result.get().rewards()).hasSize(1);
        assertThat(result.get().rewards().get(0).type()).isEqualTo(RewardType.SLOT_UNLOCK);
        verify(matchingService).unlockSlot(userId);
    }

    @Test
    @DisplayName("checkAndProcessLevelUp - 여러 레벨을 한번에 건너뛸 때 모든 보상을 처리한다")
    void checkAndProcessLevelUp_multiLevelJump() {
        user.setTotalExp(700); // 레벨 5 (슬롯 해금 레벨)
        user.setCurrentLevel(2);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(matchingService.unlockSlot(userId)).thenReturn(null);

        Optional<LevelUpResultDto> result = experienceService.checkAndProcessLevelUp(userId);

        assertThat(result).isPresent();
        assertThat(result.get().newLevel()).isEqualTo(5);
        // 레벨 3과 5에서 슬롯 해금 (2개)
        assertThat(result.get().rewards()).hasSize(2);
        verify(matchingService, times(2)).unlockSlot(userId);
    }

    @Test
    @DisplayName("calculateLevel - 경험치에 따른 레벨을 올바르게 계산한다")
    void calculateLevel() {
        assertThat(experienceService.calculateLevel(0)).isEqualTo(1);
        assertThat(experienceService.calculateLevel(50)).isEqualTo(1);
        assertThat(experienceService.calculateLevel(99)).isEqualTo(1);
        assertThat(experienceService.calculateLevel(100)).isEqualTo(2);
        assertThat(experienceService.calculateLevel(249)).isEqualTo(2);
        assertThat(experienceService.calculateLevel(250)).isEqualTo(3);
        assertThat(experienceService.calculateLevel(3200)).isEqualTo(10);
    }
}
