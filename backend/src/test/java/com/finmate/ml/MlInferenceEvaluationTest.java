package com.finmate.ml;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.glossary.GlossaryTerm;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Python 평가셋을 Java 추론에도 그대로 적용해 두 구현의 드리프트를 잡는다. */
class MlInferenceEvaluationTest {

    private static MlModelCatalog catalog;
    private static JsonNode cases;

    @BeforeAll
    static void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        catalog = new MlModelCatalog(mapper);
        catalog.load();
        try (InputStream stream = MlInferenceEvaluationTest.class.getResourceAsStream("/ml/evaluation-cases.json")) {
            cases = mapper.readTree(stream);
        }
    }

    @Test
    void 자유_서술_별도_평가셋에서도_대부분을_맞히고_설문_등급은_유지한다() {
        StyleInferenceService inference = new StyleInferenceService(catalog);
        int correct = 0;
        for (JsonNode item : cases.get("style")) {
            MlDtos.StyleCheckResponse result = inference.check(new MlDtos.StyleCheckRequest(
                    item.get("text").asText(), 3));
            if (item.get("label").asText().equals(result.estimatedProfileType())) correct++;
            assertThat(result.questionnaireGrade()).isEqualTo(3);
        }
        assertThat(cases.get("style").size()).isEqualTo(15);
        assertThat(correct).isGreaterThanOrEqualTo(12);
    }

    @Test
    void 용어_별도_평가셋의_상위_검색_재현율을_검증한다() {
        List<GlossaryTerm> published = new ArrayList<>();
        for (MlModelCatalog.TermDocument doc : catalog.terms().documents()) {
            GlossaryTerm term = new GlossaryTerm(doc.slug());
            term.setTerm(doc.term());
            term.setAliases(doc.aliases());
            published.add(term);
        }
        TermRetrievalService inference = new TermRetrievalService(catalog, () -> published);
        int top1 = 0;
        int top5 = 0;
        for (JsonNode item : cases.get("terms")) {
            List<String> slugs = inference.search(new MlDtos.TermSearchRequest(
                    item.get("question").asText(), 5)).results().stream()
                    .map(MlDtos.TermHit::slug).toList();
            String expected = item.get("slug").asText();
            if (!slugs.isEmpty() && expected.equals(slugs.getFirst())) top1++;
            if (slugs.contains(expected)) top5++;
        }
        assertThat(cases.get("terms").size()).isEqualTo(47);
        assertThat(top1).isGreaterThanOrEqualTo(42);
        assertThat(top5).isEqualTo(47);
    }
}
