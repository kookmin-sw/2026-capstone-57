package com.ilgiyebo.domain.diary.repository;

import com.ilgiyebo.domain.diary.entity.DiaryEntryEntity;
import com.ilgiyebo.domain.diary.entity.EmotionTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntryEntity, UUID> {

    Page<DiaryEntryEntity> findByUserIdOrderByEntryDateDesc(UUID userId, Pageable pageable);

    Optional<DiaryEntryEntity> findByUserIdAndEntryDate(UUID userId, LocalDate entryDate);

    /**
     * 특정 날짜 범위의 일기 목록 조회 (감정 추이 분석용)
     */
    @Query("SELECT d FROM DiaryEntryEntity d WHERE d.user.id = :userId " +
           "AND d.entryDate BETWEEN :startDate AND :endDate " +
           "AND d.emotionTag IS NOT NULL " +
           "ORDER BY d.entryDate ASC")
    List<DiaryEntryEntity> findByUserIdAndEntryDateBetweenWithEmotion(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 특정 날짜 이전의 일기를 날짜 역순으로 조회 (연속 작성 계산용)
     */
    @Query("SELECT d.entryDate FROM DiaryEntryEntity d WHERE d.user.id = :userId " +
           "AND d.entryDate <= :date ORDER BY d.entryDate DESC")
    List<LocalDate> findEntryDatesByUserIdOrderByDateDesc(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date);

    /**
     * 사용자의 전체 일기 작성 날짜 목록 (최장 연속 계산용)
     */
    @Query("SELECT d.entryDate FROM DiaryEntryEntity d WHERE d.user.id = :userId " +
           "ORDER BY d.entryDate ASC")
    List<LocalDate> findAllEntryDatesByUserIdOrderByDateAsc(@Param("userId") UUID userId);

    /**
     * 특정 날짜에 이미 경험치가 부여된 일기가 있는지 확인
     */
    boolean existsByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
}
