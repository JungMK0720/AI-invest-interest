package com.finmate.ml;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Python ml/train.py와 동일한 문자 정규화 및 2~4그램. */
final class TextFeatures {

    private TextFeatures() {}

    static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9가-힣]", "");
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
}
