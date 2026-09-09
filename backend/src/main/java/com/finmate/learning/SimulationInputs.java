package com.finmate.learning;

import com.finmate.learning.SimulationDtos.InputField;
import com.finmate.learning.SimulationDtos.ScenarioSpec;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 시나리오 입력값 해석기.
 *
 * <p>화면에서 들어온 문자열 맵을 명세(min/max/default)에 맞춰 보정한다. 잘못된 입력으로 400을 내는 대신
 * 허용 범위로 자르고 {@link #echo()}에 실제 적용값을 남긴다. 학습 화면에서 숫자 하나를 잘못 넣었다고
 * 결과가 사라지면 실험이 끊기기 때문이다.
 */
final class SimulationInputs {

    private final Map<String, Double> numbers = new LinkedHashMap<>();
    private final Map<String, String> texts = new LinkedHashMap<>();
    private final Map<String, String> echo = new LinkedHashMap<>();

    private SimulationInputs() {
    }

    static SimulationInputs of(ScenarioSpec spec, Map<String, String> raw) {
        SimulationInputs resolved = new SimulationInputs();
        Map<String, String> source = raw == null ? Map.of() : raw;
        for (InputField field : spec.inputs()) {
            String given = source.get(field.key());
            if (field.type() == SimulationDtos.FieldType.NUMBER) {
                double value = clamp(field, parse(given, Double.parseDouble(field.defaultValue())));
                resolved.numbers.put(field.key(), value);
                resolved.echo.put(field.key(), format(value));
            } else {
                String value = field.choices().stream()
                        .map(SimulationDtos.Choice::value)
                        .filter(candidate -> candidate.equals(given))
                        .findFirst()
                        .orElse(field.defaultValue());
                resolved.texts.put(field.key(), value);
                resolved.echo.put(field.key(), value);
            }
        }
        return resolved;
    }

    private static double parse(String given, double fallback) {
        if (given == null || given.isBlank()) {
            return fallback;
        }
        try {
            double parsed = Double.parseDouble(given.replace(",", "").trim());
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double clamp(InputField field, double value) {
        double lower = field.min() == null ? value : Math.max(field.min(), value);
        return field.max() == null ? lower : Math.min(field.max(), lower);
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    double num(String key) {
        Double value = numbers.get(key);
        if (value == null) {
            throw new IllegalStateException("정의되지 않은 숫자 입력입니다: " + key);
        }
        return value;
    }

    /** 퍼센트 입력을 소수 비율로 바꿔 읽는다. 7(%) -> 0.07 */
    double rate(String key) {
        return num(key) / 100.0;
    }

    /** bp 입력을 소수 비율로 바꿔 읽는다. 100(bp) -> 0.01 */
    double bp(String key) {
        return num(key) / 10_000.0;
    }

    String text(String key) {
        String value = texts.get(key);
        if (value == null) {
            throw new IllegalStateException("정의되지 않은 선택 입력입니다: " + key);
        }
        return value;
    }

    Map<String, String> echo() {
        return Map.copyOf(echo);
    }
}
