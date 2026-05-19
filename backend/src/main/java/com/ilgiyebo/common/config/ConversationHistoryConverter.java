package com.ilgiyebo.common.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ilgiyebo.domain.review.service.ReviewAiClient.ConversationTurn;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Converter
public class ConversationHistoryConverter implements AttributeConverter<List<ConversationTurn>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<ConversationTurn> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting conversation history to JSON", e);
        }
    }

    @Override
    public List<ConversationTurn> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<List<ConversationTurn>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Error converting JSON to conversation history", e);
        }
    }
}
