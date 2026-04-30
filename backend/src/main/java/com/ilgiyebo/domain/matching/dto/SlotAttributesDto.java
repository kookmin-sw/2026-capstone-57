package com.ilgiyebo.dto;

import java.util.List;

public record SlotAttributesDto(
    List<String> hobbies,
    List<String> interests,
    List<String> idealTypes
) {}
