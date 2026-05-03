package com.ilgiyebo.service;

import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.SlotPriority;
import com.ilgiyebo.domain.SlotStatus;
import com.ilgiyebo.domain.matching.exception.MatchingException;
import com.ilgiyebo.dto.SlotResponseDto;
import com.ilgiyebo.repository.SlotRepository;
import com.ilgiyebo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingServiceImpl.class);

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponseDto> getSlots(UUID userId) {
        return slotRepository.findByUserId(userId).stream()
                .map(SlotResponseDto::from)
                .toList();
    }

    @Override
    @Transactional
    public SlotResponseDto unlockSlot(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw MatchingException.USER_NOT_FOUND.toException();
        }

        // TODO: 슬롯 해금 시 경험치 조건 필요

        SlotEntity slot = SlotEntity.builder()
                .userId(userId)
                .priority(SlotPriority.HOBBY)
                .status(SlotStatus.EMPTY)
                .build();
        slot = slotRepository.save(slot);

        log.info("새 슬롯 해금: userId={}, slotId={}", userId, slot.getId());
        return SlotResponseDto.from(slot);
    }

    @Override
    @Transactional
    public SlotResponseDto updateSlotPriority(UUID userId, UUID slotId, SlotPriority priority) {
        SlotEntity slot = findSlotOrThrow(slotId);
        verifyOwnership(slot, userId);

        slot.setPriority(priority);
        slot = slotRepository.save(slot);

        return SlotResponseDto.from(slot);
    }

    private SlotEntity findSlotOrThrow(UUID slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(MatchingException.SLOT_NOT_FOUND::toException);
    }

    private void verifyOwnership(SlotEntity slot, UUID userId) {
        if (!slot.getUserId().equals(userId)) {
            throw MatchingException.SLOT_NOT_OWNED.toException();
        }
    }
}
