package com.finmate.ml;

import com.finmate.analysis.InvestorProfileType;
import com.finmate.common.ApiException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 사용자 자유 서술을 분류하되, 설문 등급은 읽기 전용으로 취급한다. */
@Service
public class StyleInferenceService {

    private static final String NOTICE = "자유 서술과 설문 결과의 차이를 살피는 교육용 보조 신호입니다. "
            + "설문 등급이나 금융상품 적합성 판정은 변경되지 않습니다.";
    private final MlModelCatalog models;

    public StyleInferenceService(MlModelCatalog models) {
        this.models = models;
    }

    public MlDtos.StyleCheckResponse check(MlDtos.StyleCheckRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()
                || request.text().length() > 2000 || request.questionnaireGrade() == null
                || request.questionnaireGrade() < 1 || request.questionnaireGrade() > 5) {
            throw ApiException.badRequest("text(1~2000자)와 questionnaireGrade(1~5)를 입력해 주세요.");
        }
        MlModelCatalog.StyleModel model = models.style();
        Map<String, Integer> features = TextFeatures.grams(request.text());
        int recognized = features.entrySet().stream()
                .filter(entry -> model.featureCounts().values().stream()
                        .anyMatch(counts -> counts.containsKey(entry.getKey())))
                .mapToInt(Map.Entry::getValue).sum();
        if (TextFeatures.normalize(request.text()).length() < 8 || recognized < 4) {
            return insufficient(request.questionnaireGrade(), model.version());
        }

        int totalExamples = model.classCounts().values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Double> scores = new HashMap<>();
        for (String label : model.labels()) {
            double score = Math.log((double) model.classCounts().get(label) / totalExamples);
            Map<String, Integer> counts = model.featureCounts().get(label);
            int denominator = model.featureTotals().get(label) + model.vocabSize();
            for (Map.Entry<String, Integer> feature : features.entrySet()) {
                if (!model.featureCounts().values().stream().anyMatch(value -> value.containsKey(feature.getKey()))) {
                    continue;
                }
                score += feature.getValue() * Math.log((counts.getOrDefault(feature.getKey(), 0) + 1.0) / denominator);
            }
            scores.put(label, score);
        }
        String best = model.labels().stream().max((left, right) ->
                Double.compare(scores.get(left), scores.get(right))).orElseThrow();
        double peak = scores.get(best);
        double totalWeight = scores.values().stream().mapToDouble(score -> Math.exp((score - peak) / 8)).sum();
        double confidence = 1.0 / totalWeight;
        if (confidence < 0.35) {
            return insufficient(request.questionnaireGrade(), model.version());
        }
        InvestorProfileType profile = InvestorProfileType.valueOf(best);
        boolean mismatch = Math.abs(profile.grade() - request.questionnaireGrade()) >= 2 && confidence >= 0.55;
        return new MlDtos.StyleCheckResponse("ESTIMATED", best, profile.label(), profile.grade(),
                round(confidence), request.questionnaireGrade(), mismatch, model.version(), NOTICE);
    }

    private static MlDtos.StyleCheckResponse insufficient(int grade, String version) {
        return new MlDtos.StyleCheckResponse("INSUFFICIENT", null, null, null,
                0, grade, false, version, NOTICE);
    }

    private static double round(double value) {
        return Math.round(value * 10000) / 10000.0;
    }
}
