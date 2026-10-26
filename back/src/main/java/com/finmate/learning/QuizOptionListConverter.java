package com.finmate.learning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/** 확인 문제의 선택지(라벨·정답 여부·해설)를 작성 순서 그대로 JSON 배열 하나의 컬럼에 저장한다. */
@Converter
public class QuizOptionListConverter implements AttributeConverter<List<LearningContent.Option>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<LearningContent.Option>> OPTIONS = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<LearningContent.Option> value) {
        try {
            return MAPPER.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("확인 문제 선택지를 저장하지 못했습니다.", e);
        }
    }

    @Override
    public List<LearningContent.Option> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return List.copyOf(MAPPER.readValue(value, OPTIONS));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("저장된 확인 문제 선택지를 읽지 못했습니다.", e);
        }
    }
}
