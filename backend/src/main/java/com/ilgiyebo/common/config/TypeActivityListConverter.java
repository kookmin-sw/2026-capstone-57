package com.ilgiyebo.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.domain.campus.entity.TypeActivity;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Converter
public class TypeActivityListConverter implements AttributeConverter<List<TypeActivity>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<TypeActivity> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting TypeActivity list to JSON", e);
        }
    }

    @Override
    public List<TypeActivity> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<TypeActivity>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to TypeActivity list", e);
        }
    }
}
