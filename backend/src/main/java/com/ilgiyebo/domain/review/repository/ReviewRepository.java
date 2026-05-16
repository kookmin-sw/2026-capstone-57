package com.ilgiyebo.domain.review.repository;

import com.ilgiyebo.domain.review.entity.ReviewEntity;
import com.ilgiyebo.domain.review.entity.ReviewMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    Optional<ReviewEntity> findByInteractionIdAndUserId(UUID interactionId, UUID userId);

    /** 사용자가 작성한 모든 회고를 최신순으로 페이징 조회 */
    Page<ReviewEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * 사용자가 작성한 회고를 필터링 옵션과 함께 페이징 조회한다.
     * 모든 필터 파라미터는 nullable이며, null일 경우 해당 조건은 무시된다.
     * N+1 방지를 위해 interaction → match → userA/userB를 fetch join 한다.
     */
    @Query(value = "SELECT r FROM ReviewEntity r " +
            "JOIN FETCH r.interaction i " +
            "JOIN FETCH i.match m " +
            "JOIN FETCH m.userA " +
            "JOIN FETCH m.userB " +
            "JOIN FETCH r.user " +
            "WHERE r.user.id = :userId " +
            "AND (:mode IS NULL OR r.mode = :mode) " +
            "AND (:minSatisfaction IS NULL OR r.satisfaction >= :minSatisfaction) " +
            "AND (:maxSatisfaction IS NULL OR r.satisfaction <= :maxSatisfaction) " +
            "AND (:fromDate IS NULL OR r.createdAt >= :fromDate) " +
            "AND (:toDate IS NULL OR r.createdAt <= :toDate) " +
            "ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM ReviewEntity r " +
                    "WHERE r.user.id = :userId " +
                    "AND (:mode IS NULL OR r.mode = :mode) " +
                    "AND (:minSatisfaction IS NULL OR r.satisfaction >= :minSatisfaction) " +
                    "AND (:maxSatisfaction IS NULL OR r.satisfaction <= :maxSatisfaction) " +
                    "AND (:fromDate IS NULL OR r.createdAt >= :fromDate) " +
                    "AND (:toDate IS NULL OR r.createdAt <= :toDate)")
    Page<ReviewEntity> findMyReviewsWithFilters(
            @Param("userId") UUID userId,
            @Param("mode") ReviewMode mode,
            @Param("minSatisfaction") Integer minSatisfaction,
            @Param("maxSatisfaction") Integer maxSatisfaction,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}
