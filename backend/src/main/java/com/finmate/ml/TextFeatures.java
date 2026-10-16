package com.finmate.ml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ml/finmate_ml/features.py와 같은 문자 정규화·2~4그램·벡터화 규칙.
 *
 * <p>학습(Python)과 추론(Java)이 다른 특징을 만들면 모델이 조용히 틀린다.
 * MlParityTest가 Python이 저장한 정답 예측과 비교해 이 클래스의 드리프트를 잡는다.
 */
final class TextFeatures {

    private static final Pattern NON_TOKEN = Pattern.compile("[^a-z0-9가-힣]");
    private static final Pattern ASCII_SPLIT = Pattern.compile("[^a-z0-9]+");
    private static final Pattern HANGUL = Pattern.compile("[가-힣]");

    private TextFeatures() {}

    static String normalize(String text) {
        return text == null ? "" : NON_TOKEN.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    static Map<String, Integer> grams(String text) {
        String value = normalize(text);
        Map<String, Integer> result = new HashMap<>();
        for (int width = 2; width <= 4; width++) {
            for (int i = 0; i + width <= value.length(); i++) {
                result.merge(value.substring(i, i + width), 1, Integer::sum);
            }
        }
        return result;
    }

    /** 1 + log(tf)로 줄인 뒤 L2 정규화한다. */
    static Map<String, Double> unitVector(Map<String, Integer> counts) {
        Map<String, Double> weights = new HashMap<>();
        double sumSquares = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double weight = 1 + Math.log(entry.getValue());
            weights.put(entry.getKey(), weight);
            sumSquares += weight * weight;
        }
        if (sumSquares == 0) {
            return Map.of();
        }
        double norm = Math.sqrt(sumSquares);
        weights.replaceAll((gram, weight) -> weight / norm);
        return weights;
    }

    static Set<String> asciiTokens(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) {
            return tokens;
        }
        for (String token : ASCII_SPLIT.split(text.toLowerCase(Locale.ROOT))) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /** 용어명이 질문에 그대로 들어 있는지. 영문 약어는 단어 단위로만 비교한다. */
    static boolean nameMatches(String name, String query) {
        String compact = normalize(name);
        if (compact.length() < 2) {
            return false;
        }
        if (HANGUL.matcher(compact).find()) {
            return normalize(query).contains(compact);
        }
        return asciiTokens(query).contains(compact);
    }
}
