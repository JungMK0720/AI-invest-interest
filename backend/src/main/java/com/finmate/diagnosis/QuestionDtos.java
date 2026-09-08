package com.finmate.diagnosis;

import java.util.List;

public final class QuestionDtos {

    private QuestionDtos() {
    }

    /**
     * 화면에 내려주는 문항. 배점(weight)과 선택지 점수(score)는 포함하지 않는다.
     * 점수 체계가 노출되면 사용자가 원하는 성향이 나오도록 답변을 맞출 수 있기 때문이다.
     */
    public record QuestionResponse(String id, String content, int orderNo, List<ChoiceResponse> choices) {
    }

    public record ChoiceResponse(String id, String label, int orderNo) {
    }

    public static QuestionResponse from(Question question) {
        List<ChoiceResponse> choices = question.getChoices().stream()
                .map(choice -> new ChoiceResponse(choice.getId(), choice.getLabel(), choice.getOrderNo()))
                .toList();
        return new QuestionResponse(question.getId(), question.getContent(), question.getOrderNo(), choices);
    }
}
