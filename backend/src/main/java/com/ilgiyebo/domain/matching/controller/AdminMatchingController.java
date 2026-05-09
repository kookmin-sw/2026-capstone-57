package com.ilgiyebo.controller;

import com.ilgiyebo.dto.AdminMatchDetailDto;
import com.ilgiyebo.dto.BatchMatchingResultDto;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MissionEntity;
import com.ilgiyebo.repository.MatchRepository;
import com.ilgiyebo.repository.MissionRepository;
import com.ilgiyebo.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "관리자 - 매칭", description = "매칭 관리 API")
@RestController
@RequestMapping("/admin/matching")
@RequiredArgsConstructor
public class AdminMatchingController {

    private final MatchingService matchingService;
    private final MatchRepository matchRepository;
    private final MissionRepository missionRepository;

    @Operation(summary = "전체 매칭 목록 조회", description = "모든 매칭과 해당 미션 정보를 조회한다")
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminMatchDetailDto>> getAllMatches() {
        List<MatchEntity> matches = matchRepository.findAll();

        List<AdminMatchDetailDto> result = matches.stream()
                .map(match -> {
                    MissionEntity mission = missionRepository.findByMatchId(match.getId()).orElse(null);
                    return AdminMatchDetailDto.from(match, mission);
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "배치 매칭 수동 실행", description = "배치 매칭을 수동으로 실행한다")
    @PostMapping("/run")
    public ResponseEntity<BatchMatchingResultDto> runBatchMatching() {
        BatchMatchingResultDto result = matchingService.executeBatchMatching();
        return ResponseEntity.ok(result);
    }
}
