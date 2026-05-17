package com.ilgiyebo.domain.user.service;

import com.ilgiyebo.domain.user.entity.Hobby;
import com.ilgiyebo.domain.user.entity.IdealType;
import com.ilgiyebo.domain.user.entity.Interest;
import com.ilgiyebo.domain.user.entity.PersonalityType;
import com.ilgiyebo.domain.user.dto.ProfileOptionDto;
import com.ilgiyebo.domain.user.dto.ProfileOptionsResponse;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class ProfileOptionService {

    private static final int DEFAULT_COUNT = 10;

    public ProfileOptionsResponse getRandomOptions() {
        return getRandomOptions(DEFAULT_COUNT);
    }

    public ProfileOptionsResponse getRandomOptions(int count) {
        return new ProfileOptionsResponse(
                pickRandom(Hobby.values(), count),
                pickRandom(Interest.values(), count),
                allOptions(PersonalityType.values()),
                pickRandom(IdealType.values(), count)
        );
    }

    private <E extends Enum<E>> List<ProfileOptionDto> allOptions(E[] values) {
        return Arrays.stream(values)
                .map(e -> new ProfileOptionDto(e.name(), getLabel(e)))
                .toList();
    }

    private <E extends Enum<E>> List<ProfileOptionDto> pickRandom(E[] values, int count) {
        List<E> shuffled = new java.util.ArrayList<>(Arrays.asList(values));
        Collections.shuffle(shuffled);
        return shuffled.stream()
                .limit(count)
                .map(e -> new ProfileOptionDto(e.name(), getLabel(e)))
                .toList();
    }

    private <E extends Enum<E>> String getLabel(E value) {
        try {
            return (String) value.getClass().getMethod("getLabel").invoke(value);
        } catch (Exception e) {
            return value.name();
        }
    }
}
