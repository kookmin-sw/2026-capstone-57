package com.ilgiyebo.controller;

import com.ilgiyebo.domain.SlotPriority;
import com.ilgiyebo.dto.SlotResponseDto;
import com.ilgiyebo.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "슬롯", description = "슬롯 관리 API")
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final MatchingService matchingService;

    @Operation(summary = "슬롯 목록 조회", description = "현재 사용자의 슬롯 목록을 조회한다")
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<SlotResponseDto>> getSlots(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(matchingService.getSlots(userId));
    }

    @Operation(summary = "슬롯 해금", description = "새로운 슬롯을 해금한다 (레벨업 보상)")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/unlock")
    public ResponseEntity<SlotResponseDto> unlockSlot(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(matchingService.unlockSlot(userId));
    }

    @Operation(summary = "슬롯 우선순위 변경", description = "슬롯의 매칭 우선순위(HOBBY, INTEREST, IDEAL_TYPE)를 변경한다")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{slotId}/priority/{priority}")
    public ResponseEntity<SlotResponseDto> updateSlotPriority(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID slotId,
            @PathVariable SlotPriority priority) {
        return ResponseEntity.ok(matchingService.updateSlotPriority(userId, slotId, priority));
    }
}
