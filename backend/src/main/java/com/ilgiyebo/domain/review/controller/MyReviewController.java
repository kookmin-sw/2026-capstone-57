package com.ilgiyebo.domain.review.controller;

import com.ilgiyebo.domain.review.dto.ReviewResponse;
import com.ilgiyebo.domain.review.entity.ReviewMode;
import com.ilgiyebo.domain.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "회고", description = "본인 회고 목록 조회 API")
@RestController
@RequestMapping("/api/reviews")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class MyReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "본인 회고 목록 조회",
            description = "본인이 작성한 모든 회고를 최신순으로 페이징 조회한다. " +
                    "필터 옵션: mode (AI_ASSISTED/DIRECT), 만족도 범위 (1~5), 작성 일시 범위.")
    @GetMapping("/my")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UUID userId,
            @Parameter(description = "회고 모드 필터", example = "AI_ASSISTED")
            @RequestParam(required = false) ReviewMode mode,
            @Parameter(description = "만족도 하한 (1~5)", example = "3")
            @RequestParam(required = false) Integer minSatisfaction,
            @Parameter(description = "만족도 상한 (1~5)", example = "5")
            @RequestParam(required = false) Integer maxSatisfaction,
            @Parameter(description = "작성 시작 일시 (ISO-8601)", example = "2025-01-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "작성 종료 일시 (ISO-8601)", example = "2025-12-31T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(reviewService.getMyReviews(
                userId, mode, minSatisfaction, maxSatisfaction, fromDate, toDate, pageable));
    }
}
