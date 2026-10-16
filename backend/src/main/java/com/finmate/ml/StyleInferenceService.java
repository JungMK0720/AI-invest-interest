package com.finmate.ml;

import com.finmate.analysis.InvestorProfileType;
import com.finmate.common.ApiException;
import com.finmate.learning.LearningSignalCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 자유 서술에서 성향 신호와 관심사 태그를 추정한다. 설문 등급은 읽기 전용으로만 쓴다.
 *
 * <p>판단 순서는 ml/finmate_ml/style.py의 predict와 같다.
 * (1) 글자 수·근거 조각 수가 부족하면 보류 → (2) softmax 확신도가 기준 미만이면 보류 → (3) 추정.
 * 주제와 무관한 문장을 억지로 분류하지 않도록 보류 기준은 주제 무관 문장으로 따로 맞췄다.
 */
@Service
public class StyleInferenceService {

    public static final int MAX_TEXT = 1000;

    private final MlModelCatalog.StyleModel model;
    private final LearningSignalCatalog signals;
    private final Set<String> informative;

    public StyleInferenceService(MlModelCatalog models, LearningSignalCatalog signals) {
        this.model = models.style();
        this.signals = signals;
        this.informative = informativeGrams(model);
    }

    private static Set<String> informativeGrams(MlModelCatalog.StyleModel model) {
        Map<String, Double> strongest = new HashMap<>();
        model.weights().values().forEach(row -> row.forEach((gram, weight) ->
                strongest.merge(gram, Math.abs(weight), Math::max)));
        Set<String> result = new HashSet<>();
        strongest.forEach((gram, weight) -> {
            if (weight >= model.informativeWeight()) {
                result.add(gram);
            }
        });
        return Set.copyOf(result);
    }

    /** 설문과 무관하게 문장만 보고 추정한다. 진단 제출 흐름과 모델 실험실이 함께 쓴다. */
    public Estimate estimate(String text) {
        Map<String, Integer> counts = TextFeatures.grams(text);
        int recognized = counts.entrySet().stream()
                .filter(entry -> informative.contains(entry.getKey()))
                .mapToInt(Map.Entry::getValue).sum();
        if (TextFeatures.normalize(text).length() < model.minCharacters() || recognized < model.minRecognized()) {
            return new Estimate("INSUFFICIENT", null, 0, Map.of(), List.of(), model.version());
        }
        Map<String, Double> x = TextFeatures.unitVector(counts);
        double[] scores = new double[model.labels().size()];
        for (int i = 0; i < scores.length; i++) {
            String label = model.labels().get(i);
            Map<String, Double> row = model.weights().get(label);
            double score = model.bias().get(label);
            for (Map.Entry<String, Double> feature : x.entrySet()) {
                score += row.getOrDefault(feature.getKey(), 0.0) * feature.getValue();
            }
            scores[i] = score / model.temperature();
        }
        double peak = Double.NEGATIVE_INFINITY;
        for (double score : scores) peak = Math.max(peak, score);
        double total = 0;
        for (int i = 0; i < scores.length; i++) {
            scores[i] = Math.exp(scores[i] - peak);
            total += scores[i];
        }
        Map<String, Double> probabilities = new LinkedHashMap<>();
        String best = null;
        double bestProbability = -1;
        for (int i = 0; i < scores.length; i++) {
            double probability = scores[i] / total;
            probabilities.put(model.labels().get(i), probability);
            if (probability > bestProbability) {
                best = model.labels().get(i);
                bestProbability = probability;
            }
        }
        List<TagScore> tags = tags(x);
        if (bestProbability < model.confidenceFloor()) {
            return new Estimate("INSUFFICIENT", null, bestProbability, probabilities, tags, model.version());
        }
        return new Estimate("ESTIMATED", best, bestProbability, probabilities, tags, model.version());
    }

    private List<TagScore> tags(Map<String, Double> x) {
        List<TagScore> result = new ArrayList<>();
        for (Map.Entry<String, MlModelCatalog.TagModel> entry : model.tags().entrySet()) {
            MlModelCatalog.TagModel tag = entry.getValue();
            double score = tag.bias();
            for (Map.Entry<String, Double> feature : x.entrySet()) {
                score += tag.weights().getOrDefault(feature.getKey(), 0.0) * feature.getValue();
            }
            double probability = sigmoid(score);
            if (probability >= model.tagThreshold()) {
                result.add(new TagScore(entry.getKey(), tag.label(), probability));
            }
        }
        result.sort((left, right) -> left.code().compareTo(right.code()));
        return List.copyOf(result);
    }

    private static double sigmoid(double value) {
        if (value >= 0) return 1.0 / (1.0 + Math.exp(-value));
        double exp = Math.exp(value);
        return exp / (1.0 + exp);
    }

    /** 설문 등급과 2단계 이상 다르고 확신도가 충분할 때만 불일치로 본다. */
    public boolean mismatches(Estimate estimate, int questionnaireGrade) {
        if (!"ESTIMATED".equals(estimate.status())) return false;
        int grade = InvestorProfileType.valueOf(estimate.profileType()).grade();
        return Math.abs(grade - questionnaireGrade) >= model.mismatchMinGap()
                && estimate.confidence() >= model.mismatchMinConfidence();
    }

    public List<MlDtos.LearningLink> learningLinks(List<TagScore> tags) {
        List<MlDtos.LearningLink> links = new ArrayList<>();
        for (TagScore tag : tags) {
            signals.tag(tag.code()).ifPresent(path -> links.add(new MlDtos.LearningLink(
                    tag.code(), path.label(), path.message(),
                    path.concepts().stream().filter(signals::conceptPublished)
                            .map(slug -> new MlDtos.LinkItem(slug, signals.conceptTitle(slug))).toList(),
                    path.terms().stream().map(slug -> new MlDtos.LinkItem(slug, signals.termTitle(slug))).toList())));
        }
        return List.copyOf(links);
    }

    public MlDtos.StyleCheckResponse check(MlDtos.StyleCheckRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()
                || request.text().length() > MAX_TEXT || request.questionnaireGrade() == null
                || request.questionnaireGrade() < 1 || request.questionnaireGrade() > 5) {
            throw ApiException.badRequest("text(1~1000자)와 questionnaireGrade(1~5)를 입력해 주세요.");
        }
        Estimate estimate = estimate(request.text());
        InvestorProfileType profile = estimate.profileType() == null ? null
                : InvestorProfileType.valueOf(estimate.profileType());
        List<MlDtos.TagSignal> tags = estimate.tags().stream()
                .map(tag -> new MlDtos.TagSignal(tag.code(), tag.label(), round(tag.score()))).toList();
        Map<String, Double> probabilities = new LinkedHashMap<>();
        estimate.probabilities().forEach((label, value) -> probabilities.put(label, round(value)));
        return new MlDtos.StyleCheckResponse(estimate.status(), estimate.profileType(),
                profile == null ? null : profile.label(), profile == null ? null : profile.grade(),
                round(estimate.confidence()), request.questionnaireGrade(),
                mismatches(estimate, request.questionnaireGrade()), probabilities, tags,
                learningLinks(estimate.tags()), estimate.modelVersion(), model.notice());
    }

    public String version() {
        return model.version();
    }

    private static double round(double value) {
        return Math.round(value * 10000) / 10000.0;
    }

    /** 모델의 원시 추정. 확률은 반올림하지 않는다(Python 정답과의 일치 검사용). */
    public record Estimate(String status, String profileType, double confidence,
                           Map<String, Double> probabilities, List<TagScore> tags, String modelVersion) {}

    public record TagScore(String code, String label, double score) {}
}
