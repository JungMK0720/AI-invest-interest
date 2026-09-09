package com.finmate.learning;

import java.util.List;
import java.util.Map;

/**
 * 모의투자 DTO.
 *
 * <p>시나리오 정의(입력 필드 명세)를 서버가 내려주고 프론트가 폼을 생성한다. 계산은 전부 서버에서 하고
 * 화면은 표시만 한다. 같은 입력과 같은 seed면 항상 같은 결과가 나오도록 난수를 seed로 고정한다
 * (docs/09의 결과 재현성 원칙과 동일한 이유).
 */
public final class SimulationDtos {

    /** 모의투자 화면 상단에 항상 노출하는 고지. */
    public static final String SIMULATION_NOTICE =
            "실제 주문이 아닌 교육용 시뮬레이션입니다. 가정한 수익률·변동성·비용으로 계산한 예시일 뿐 "
                    + "미래 수익률을 예측하거나 보장하지 않으며, 특정 상품의 매매를 권유하지 않습니다.";

    private SimulationDtos() {
    }

    public enum FieldType { NUMBER, CHOICE }

    public record Choice(String value, String label) {
    }

    /**
     * 입력 필드 명세.
     *
     * @param defaultValue NUMBER면 숫자 문자열, CHOICE면 choices의 value
     */
    public record InputField(
            String key,
            String label,
            FieldType type,
            String unit,
            Double min,
            Double max,
            Double step,
            String defaultValue,
            List<Choice> choices,
            String help) {

        public static InputField number(String key, String label, String unit, double min, double max, double step, double defaultValue, String help) {
            return new InputField(key, label, FieldType.NUMBER, unit, min, max, step, trim(defaultValue), List.of(), help);
        }

        public static InputField choice(String key, String label, List<Choice> choices, String defaultValue, String help) {
            return new InputField(key, label, FieldType.CHOICE, null, null, null, null, defaultValue, choices, help);
        }

        private static String trim(double value) {
            return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
        }
    }

    /** 시나리오 정의. conceptSlugs는 이 시나리오로 실험할 수 있는 개념들이다. */
    public record ScenarioSpec(
            String id,
            String title,
            String question,
            String summary,
            String mechanism,
            List<String> conceptSlugs,
            List<InputField> inputs,
            boolean randomised,
            String notice) {
    }

    public record ScenarioListResponse(List<ScenarioSpec> scenarios, String notice) {
    }

    /** 실행 요청. seed를 바꾸면 "다른 시장 상황"을 본다. null이면 서버 기본 seed를 쓴다. */
    public record RunRequest(Map<String, String> inputs, Long seed) {
    }

    public enum Tone { GOOD, BAD, NEUTRAL }

    /** 결과 숫자 한 칸. rawValue는 정렬·비교용, value는 이미 포맷된 표시 문자열이다. */
    public record Metric(String label, String value, double rawValue, String unit, Tone tone, String note) {
    }

    public record Point(String label, double value) {
    }

    /** 차트 계열. kind는 프론트가 선/영역/막대를 고르는 힌트다. */
    public record Series(String name, String kind, String unit, List<Point> points) {
    }

    public record TableRow(String label, List<String> cells) {
    }

    public record Table(String title, List<String> columns, List<TableRow> rows, String note) {
    }

    /**
     * 실행 결과.
     *
     * @param headline  한 줄 결과. 숫자보다 해석을 먼저 읽게 한다.
     * @param lessons   이 실행에서 확인된 교훈. 입력값에 따라 문장이 달라진다.
     * @param echo      실제 적용된 입력값. 기본값 보정이 일어났을 때 사용자에게 보여준다.
     */
    public record RunResponse(
            String scenarioId,
            String title,
            String headline,
            List<Metric> metrics,
            List<Series> series,
            List<Table> tables,
            List<String> lessons,
            List<String> assumptions,
            Map<String, String> echo,
            long seed,
            String notice) {
    }
}
