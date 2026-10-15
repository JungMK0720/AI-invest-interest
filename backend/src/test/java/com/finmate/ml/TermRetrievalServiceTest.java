package com.finmate.ml;

import static org.assertj.core.api.Assertions.assertThat;
import com.finmate.glossary.GlossaryTerm;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TermRetrievalServiceTest {

    private static MlModelCatalog catalog;

    @BeforeAll
    static void setUp() {
        catalog = new MlModelCatalog(new ObjectMapper());
        catalog.load();
    }

    @Test
    void 질문에서_관련_용어를_찾고_공개된_항목만_돌려준다() {
        GlossaryTerm etf = new GlossaryTerm("etf");
        etf.setTerm("ETF");
        etf.setAliases(List.of("상장지수펀드"));

        TermRetrievalService service = new TermRetrievalService(catalog, () -> List.of(etf));
        MlDtos.TermSearchResponse result = service.search(new MlDtos.TermSearchRequest(
                "여러 주식을 묶어 거래소에서 사고파는 ETF가 뭔가요?", 5));

        assertThat(result.results()).extracting(MlDtos.TermHit::slug).containsExactly("etf");
        assertThat(result.results().getFirst().term()).isEqualTo("ETF");
        assertThat(result.modelVersion()).isEqualTo(catalog.terms().version());
    }

    @Test
    void 공개된_용어가_없으면_모델에_있어도_검색에_노출하지_않는다() {
        TermRetrievalService service = new TermRetrievalService(catalog, List::of);
        assertThat(service.search(new MlDtos.TermSearchRequest("공매도가 뭔가요?", null)).results()).isEmpty();
    }
}
