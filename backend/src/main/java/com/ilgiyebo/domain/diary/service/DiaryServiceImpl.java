package com.ilgiyebo.domain.diary.service;

import com.ilgiyebo.domain.diary.dto.*;
import com.ilgiyebo.domain.diary.entity.DiaryEntryEntity;
import com.ilgiyebo.domain.diary.entity.DiarySource;
import com.ilgiyebo.domain.diary.exception.DiaryException;
import com.ilgiyebo.domain.diary.repository.DiaryEntryRepository;
import com.ilgiyebo.domain.exp.entity.ExpActivity;
import com.ilgiyebo.domain.exp.entity.ExpHistoryEntity;
import com.ilgiyebo.domain.exp.repository.ExpHistoryRepository;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryServiceImpl implements DiaryService {

    private static final int DIARY_WRITE_EXP = 10;
    private static final int DIARY_STREAK_BONUS_EXP = 5;
    private static final int STREAK_BONUS_THRESHOLD = 3;

    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;
    private final ExpHistoryRepository expHistoryRepository;

    @Override
    @Transactional
    public DiaryEntryResponse createEntry(UUID userId, DiaryInputDto input) {
        UserEntity user = findUserOrThrow(userId);
        validateContent(input.content());

        DiarySource source = input.source() != null ? input.source() : DiarySource.MANUAL;
        LocalDate entryDate = input.date();

        // upsert: 같은 날짜에 이미 존재하면 업데이트
        Optional<DiaryEntryEntity> existing = diaryEntryRepository
                .findByUserIdAndEntryDate(userId, entryDate);

        DiaryEntryEntity entity;
        boolean isNewEntry;

        if (existing.isPresent()) {
            entity = existing.get();
            entity.setContent(input.content());
            entity.setEmotionTag(input.emotionTag());
            entity.setSource(source);
            entity.setAiSessionId(input.aiSessionId());
            isNewEntry = false;
        } else {
            int currentStreak = calculateCurrentStreak(userId, entryDate);
            entity = DiaryEntryEntity.builder()
                    .user(user)
                    .entryDate(entryDate)
                    .content(input.content())
                    .emotionTag(input.emotionTag())
                    .source(source)
                    .aiSessionId(input.aiSessionId())
                    .streakCount(currentStreak + 1)
                    .build();
            isNewEntry = true;
        }

        entity = diaryEntryRepository.save(entity);
        log.debug("일기 저장: userId={}, date={}, source={}, isNew={}",
                userId, entryDate, source, isNewEntry);

        // 새 일기 작성 시에만 경험치 부여
        if (isNewEntry) {
            grantExperience(user, entity.getStreakCount());
        }

        return DiaryEntryResponse.from(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiaryEntryResponse> getEntries(UUID userId, Pageable pageable) {
        findUserOrThrow(userId);
        return diaryEntryRepository.findByUserIdOrderByEntryDateDesc(userId, pageable)
                .map(DiaryEntryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public StreakInfoDto getStreak(UUID userId) {
        findUserOrThrow(userId);

        int currentStreak = calculateCurrentStreak(userId, LocalDate.now());
        int longestStreak = calculateLongestStreak(userId);

        return new StreakInfoDto(currentStreak, longestStreak);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmotionTrendDto> getEmotionTrend(UUID userId, String period) {
        findUserOrThrow(userId);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = calculateStartDate(endDate, period);

        List<DiaryEntryEntity> entries = diaryEntryRepository
                .findByUserIdAndEntryDateBetweenWithEmotion(userId, startDate, endDate);

        return entries.stream()
                .map(e -> new EmotionTrendDto(e.getEntryDate(), e.getEmotionTag()))
                .toList();
    }

    /**
     * 현재 연속 작성 일수를 계산한다.
     * 오늘(또는 지정 날짜)부터 역순으로 연속된 날짜를 센다.
     */
    private int calculateCurrentStreak(UUID userId, LocalDate fromDate) {
        List<LocalDate> dates = diaryEntryRepository
                .findEntryDatesByUserIdOrderByDateDesc(userId, fromDate);

        if (dates.isEmpty()) {
            return 0;
        }

        int streak = 0;
        LocalDate expectedDate = fromDate;

        // 오늘 이미 작성했으면 오늘부터, 아니면 어제부터 시작
        if (!dates.isEmpty() && dates.get(0).equals(fromDate)) {
            expectedDate = fromDate;
        } else {
            expectedDate = fromDate.minusDays(1);
        }

        for (LocalDate date : dates) {
            if (date.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (date.isBefore(expectedDate)) {
                break;
            }
        }

        return streak;
    }

    /**
     * 최장 연속 작성 일수를 계산한다.
     */
    private int calculateLongestStreak(UUID userId) {
        List<LocalDate> dates = diaryEntryRepository
                .findAllEntryDatesByUserIdOrderByDateAsc(userId);

        if (dates.isEmpty()) {
            return 0;
        }

        int longestStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).equals(dates.get(i - 1).plusDays(1))) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else if (!dates.get(i).equals(dates.get(i - 1))) {
                // 같은 날짜 중복은 무시, 연속이 끊기면 리셋
                currentStreak = 1;
            }
        }

        return longestStreak;
    }

    /**
     * 일기 작성 경험치를 부여한다.
     * - 기본 경험치: DIARY_WRITE_EXP
     * - 연속 작성 보너스: STREAK_BONUS_THRESHOLD일 이상 연속 시 추가 경험치
     */
    private void grantExperience(UserEntity user, int streakCount) {
        // 기본 일기 작성 경험치
        ExpHistoryEntity expHistory = ExpHistoryEntity.builder()
                .user(user)
                .activity(ExpActivity.DIARY_WRITE)
                .amount(DIARY_WRITE_EXP)
                .bonusAmount(0)
                .build();

        int bonusAmount = 0;
        if (streakCount >= STREAK_BONUS_THRESHOLD) {
            bonusAmount = DIARY_STREAK_BONUS_EXP;
            expHistory = expHistory.toBuilder()
                    .bonusAmount(bonusAmount)
                    .build();
        }

        expHistoryRepository.save(expHistory);

        // 사용자 총 경험치 업데이트
        int totalGained = DIARY_WRITE_EXP + bonusAmount;
        user.setTotalExp(user.getTotalExp() + totalGained);
        userRepository.save(user);

        log.debug("일기 경험치 부여: userId={}, base={}, bonus={}, streak={}",
                user.getId(), DIARY_WRITE_EXP, bonusAmount, streakCount);
    }

    private LocalDate calculateStartDate(LocalDate endDate, String period) {
        return switch (period != null ? period.toLowerCase() : "month") {
            case "week" -> endDate.minusWeeks(1);
            case "month" -> endDate.minusMonths(1);
            case "quarter" -> endDate.minusMonths(3);
            case "year" -> endDate.minusYears(1);
            default -> endDate.minusMonths(1);
        };
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw DiaryException.EMPTY_CONTENT.toException();
        }
    }

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(DiaryException.USER_NOT_FOUND::toException);
    }
}
