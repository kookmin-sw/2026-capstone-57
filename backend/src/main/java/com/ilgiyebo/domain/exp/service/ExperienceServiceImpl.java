package com.ilgiyebo.domain.exp.service;

import com.ilgiyebo.domain.exp.dto.*;
import com.ilgiyebo.domain.exp.entity.ExpActivity;
import com.ilgiyebo.domain.exp.entity.ExpHistoryEntity;
import com.ilgiyebo.domain.exp.entity.RewardType;
import com.ilgiyebo.domain.exp.exception.ExperienceException;
import com.ilgiyebo.domain.exp.repository.ExpHistoryRepository;
import com.ilgiyebo.domain.matching.service.MatchingService;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExperienceServiceImpl implements ExperienceService {

    /**
     * 활동별 기본 경험치 부여량.
     */
    private static final Map<ExpActivity, Integer> EXP_AMOUNTS = Map.ofEntries(
        Map.entry(ExpActivity.DIARY_WRITE, 10),
        Map.entry(ExpActivity.DIARY_STREAK_BONUS, 5),
        Map.entry(ExpActivity.PLANNER_WRITE, 5),
        Map.entry(ExpActivity.QUIZ_COMPLETE, 15),
        Map.entry(ExpActivity.CHAT_PARTICIPATE, 10),
        Map.entry(ExpActivity.GAME_COMPLETE, 20),
        Map.entry(ExpActivity.MISSION_COMPLETE, 30),
        Map.entry(ExpActivity.REVIEW_WRITE, 20),
        Map.entry(ExpActivity.HINT_ANSWER, 5)
    );

    /**
     * 활동별 일일 최대 지급 횟수.
     * 이 맵에 포함되지 않은 활동은 제한 없이 지급된다.
     */
    private static final Map<ExpActivity, Integer> DAILY_LIMITS = Map.of(
        ExpActivity.DIARY_WRITE, 1,         // 일기: 하루 1회
        ExpActivity.DIARY_STREAK_BONUS, 1,  // 연속 보너스: 하루 1회
        ExpActivity.PLANNER_WRITE, 1,       // 플래너: 하루 최초 작성만
        ExpActivity.HINT_ANSWER, 3          // 힌트 질문 답변: 1일 최대 3회
    );

    /**
     * 레벨별 누적 경험치 임계값.
     * 인덱스 = 레벨 (레벨 1은 0부터 시작, 레벨 2는 100 이상, ...).
     * 레벨 N에 도달하려면 LEVEL_THRESHOLDS[N] 이상의 누적 경험치가 필요하다.
     */
    private static final int[] LEVEL_THRESHOLDS = {
        0,     // 레벨 1: 0 이상
        100,   // 레벨 2: 100 이상
        250,   // 레벨 3: 250 이상
        450,   // 레벨 4: 450 이상
        700,   // 레벨 5: 700 이상
        1000,  // 레벨 6: 1000 이상
        1400,  // 레벨 7: 1400 이상
        1900,  // 레벨 8: 1900 이상
        2500,  // 레벨 9: 2500 이상
        3200   // 레벨 10: 3200 이상
    };

    /**
     * 슬롯 해금이 발생하는 레벨 목록.
     */
    private static final Set<Integer> SLOT_UNLOCK_LEVELS = Set.of(3, 5, 7, 10);

    private final ExpHistoryRepository expHistoryRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;

    @Override
    @Transactional
    public ExpGrantDto grantExperience(UUID userId, ExpActivity activity) {
        UserEntity user = findUserOrThrow(userId);

        int amount = EXP_AMOUNTS.getOrDefault(activity, 0);
        if (amount <= 0) {
            throw ExperienceException.INVALID_ACTIVITY.toException();
        }

        // 일일 제한 검증
        checkDailyLimit(userId, activity);

        ExpHistoryEntity expHistory = ExpHistoryEntity.builder()
                .user(user)
                .activity(activity)
                .amount(amount)
                .bonusAmount(0)
                .build();
        expHistoryRepository.save(expHistory);

        int newTotal = user.getTotalExp() + amount;
        user.setTotalExp(newTotal);
        userRepository.save(user);

        log.info("경험치 부여: userId={}, activity={}, amount={}, newTotal={}",
                userId, activity, amount, newTotal);

        return new ExpGrantDto(activity, amount, 0, newTotal);
    }

    @Override
    @Transactional(readOnly = true)
    public ExperienceInfoDto getExperienceInfo(UUID userId) {
        UserEntity user = findUserOrThrow(userId);

        int totalExp = user.getTotalExp();
        int currentLevel = user.getCurrentLevel();
        int expToNextLevel = calculateExpToNextLevel(totalExp, currentLevel);
        int currentLevelExp = calculateCurrentLevelExp(totalExp, currentLevel);

        return new ExperienceInfoDto(userId, totalExp, currentLevel, expToNextLevel, currentLevelExp);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpHistoryEntryDto> getExpHistory(UUID userId, Pageable pageable) {
        findUserOrThrow(userId);
        return expHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(ExpHistoryEntryDto::from);
    }

    @Override
    @Transactional
    public Optional<LevelUpResultDto> checkAndProcessLevelUp(UUID userId) {
        UserEntity user = findUserOrThrow(userId);

        int totalExp = user.getTotalExp();
        int currentLevel = user.getCurrentLevel();
        int newLevel = calculateLevel(totalExp);

        if (newLevel <= currentLevel) {
            return Optional.empty();
        }

        // 레벨업 처리
        user.setCurrentLevel(newLevel);
        userRepository.save(user);

        // 보상 처리
        List<LevelUpResultDto.RewardDto> rewards = processRewards(userId, currentLevel, newLevel);

        log.info("레벨업: userId={}, {} → {}, rewards={}", userId, currentLevel, newLevel, rewards.size());

        return Optional.of(new LevelUpResultDto(newLevel, rewards));
    }

    /**
     * 누적 경험치로부터 현재 레벨을 계산한다.
     */
    int calculateLevel(int totalExp) {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (totalExp >= LEVEL_THRESHOLDS[i]) {
                return i + 1; // 레벨은 1부터 시작
            }
        }
        return 1;
    }

    /**
     * 다음 레벨까지 필요한 경험치를 계산한다.
     * 최대 레벨이면 0을 반환한다.
     */
    private int calculateExpToNextLevel(int totalExp, int currentLevel) {
        if (currentLevel >= LEVEL_THRESHOLDS.length) {
            return 0; // 최대 레벨
        }
        return LEVEL_THRESHOLDS[currentLevel] - totalExp;
    }

    /**
     * 현재 레벨 내에서 획득한 경험치를 계산한다.
     */
    private int calculateCurrentLevelExp(int totalExp, int currentLevel) {
        if (currentLevel <= 1) {
            return totalExp;
        }
        return totalExp - LEVEL_THRESHOLDS[currentLevel - 1];
    }

    /**
     * 레벨업 보상을 처리한다.
     * 특정 레벨에서 슬롯 해금 등의 보상을 부여한다.
     */
    private List<LevelUpResultDto.RewardDto> processRewards(UUID userId, int fromLevel, int toLevel) {
        List<LevelUpResultDto.RewardDto> rewards = new ArrayList<>();

        for (int level = fromLevel + 1; level <= toLevel; level++) {
            if (SLOT_UNLOCK_LEVELS.contains(level)) {
                try {
                    matchingService.unlockSlot(userId);
                    rewards.add(new LevelUpResultDto.RewardDto(
                        RewardType.SLOT_UNLOCK,
                        "레벨 " + level + " 달성! 새로운 슬롯이 해금되었습니다."
                    ));
                    log.info("슬롯 해금 보상: userId={}, level={}", userId, level);
                } catch (Exception e) {
                    log.warn("슬롯 해금 실패: userId={}, level={}, error={}", userId, level, e.getMessage());
                }
            }
        }

        return rewards;
    }

    /**
     * 일일 경험치 지급 제한을 검증한다.
     * DAILY_LIMITS에 정의된 활동에 대해 오늘 자정 이후 지급 횟수를 확인한다.
     */
    private void checkDailyLimit(UUID userId, ExpActivity activity) {
        Integer dailyLimit = DAILY_LIMITS.get(activity);
        if (dailyLimit == null) {
            return; // 제한 없는 활동
        }

        LocalDateTime todayStart = LocalDate.now().atTime(LocalTime.MIDNIGHT);
        long todayCount = expHistoryRepository.countByUserIdAndActivitySince(userId, activity, todayStart);

        if (todayCount >= dailyLimit) {
            log.debug("일일 제한 초과: userId={}, activity={}, todayCount={}, limit={}",
                    userId, activity, todayCount, dailyLimit);
            throw ExperienceException.DAILY_LIMIT_EXCEEDED.toException();
        }
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(ExperienceException.USER_NOT_FOUND::toException);
    }
}
