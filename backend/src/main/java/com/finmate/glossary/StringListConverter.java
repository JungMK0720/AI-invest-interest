package com.finmate.glossary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/** Keeps the authored order of aliases and related slugs in one database column. */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<String> value) {
        try {
            return MAPPER.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("용어의 목록 필드를 저장하지 못했습니다.", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return List.copyOf(MAPPER.readValue(value, STRING_LIST));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("저장된 용어의 목록 필드를 읽지 못했습니다.", e);
        }
    }
}
