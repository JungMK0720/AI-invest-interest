package com.finmate.ml;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Python이 내보낸 모델 두 개를 서버 기동 시 한 번 읽는다. */
@Component
public class MlModelCatalog {

    private final ObjectMapper mapper;
    private StyleModel style;
    private TermModel terms;

    public MlModelCatalog(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void load() {
        try (InputStream styleFile = new ClassPathResource("ml/style-model.json").getInputStream();
             InputStream termFile = new ClassPathResource("ml/term-model.json").getInputStream()) {
            style = mapper.readValue(styleFile, StyleModel.class);
            terms = mapper.readValue(termFile, TermModel.class);
            if (style.labels().size() != 5 || style.vocabSize() <= 0 || terms.documents().isEmpty()) {
                throw new IllegalStateException("ML 모델 산출물이 비어 있습니다.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("ML 모델 산출물을 읽지 못했습니다. python3 ml/train.py를 실행하세요.", e);
        }
    }

    public StyleModel style() { return style; }

    public TermModel terms() { return terms; }

    public record StyleModel(String version, String algorithm, List<String> labels,
                             int sampleCount, int vocabSize, Map<String, Integer> classCounts,
                             Map<String, Integer> featureTotals,
                             Map<String, Map<String, Integer>> featureCounts, String notice) {}

    public record TermModel(String version, String algorithm, int documentCount,
                            String sourceSha256, Map<String, Double> idf,
                            List<TermDocument> documents) {}

    public record TermDocument(String slug, String term, List<String> aliases, String text) {}
}
