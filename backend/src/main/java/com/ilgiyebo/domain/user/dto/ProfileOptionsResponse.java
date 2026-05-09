package com.ilgiyebo.domain.user.dto;

import java.util.List;

public record ProfileOptionsResponse(
    List<ProfileOptionDto> hobbies,
    List<ProfileOptionDto> interests,
    List<ProfileOptionDto> personalityTypes,
    List<ProfileOptionDto> idealTypes
) {}
