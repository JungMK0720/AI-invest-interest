package com.finmate.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * ml/train.py가 내보낸 모델 산출물을 서버 기동 시 한 번 읽는다.
 *
 * <p>산출물이 없거나 형식이 맞지 않으면 기동을 멈춘다. 모델 없이 조용히 동작하는 것보다
 * 배포 시점에 드러나는 편이 안전하다.
 */
@Component
public class MlModelCatalog {

    private final ObjectMapper mapper;
    private StyleModel style;
    private TermModel terms;
    private JsonNode modelCard;

    public MlModelCatalog(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void load() {
        style = read("ml/style-model.json", StyleModel.class);
        terms = read("ml/term-model.json", TermModel.class);
        modelCard = read("ml/model-card.json", JsonNode.class);
        if (style.labels().size() != 5 || style.weights().isEmpty() || style.temperature() <= 0) {
            throw new IllegalStateException("자유 서술 성향 모델 산출물이 올바르지 않습니다.");
        }
        if (terms.idf().isEmpty() || terms.fieldWeights().isEmpty()) {
            throw new IllegalStateException("용어 검색 모델 산출물이 올바르지 않습니다.");
        }
    }

    private <T> T read(String path, Class<T> type) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(input, type);
        } catch (IOException e) {
            throw new IllegalStateException("ML 모델 산출물을 읽지 못했습니다(" + path + "). python3 ml/train.py를 실행하세요.", e);
        }
    }

    public StyleModel style() { return style; }

    public TermModel terms() { return terms; }

    public JsonNode modelCard() { return modelCard; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StyleModel(String version, String algorithm, List<String> labels,
                             int minCharacters, int minRecognized, double informativeWeight,
                             double temperature, double confidenceFloor,
                             int mismatchMinGap, double mismatchMinConfidence, int vocabularySize,
                             Map<String, Double> bias, Map<String, Map<String, Double>> weights,
                             double tagThreshold, Map<String, TagModel> tags, String notice) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TagModel(String label, double bias, Map<String, Double> weights) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TermModel(String version, String algorithm, Map<String, Double> fieldWeights,
                            double termBoost, double aliasFactor, int documentCount,
                            String sourceSha256, double idfMax, Map<String, Double> idf) {}
}
