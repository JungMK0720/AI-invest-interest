package com.finmate.ml;

import static org.assertj.core.api.Assertions.assertThat;

import com.finmate.glossary.GlossaryContentChangedEvent;
import com.finmate.glossary.GlossaryTerm;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TermRetrievalServiceTest {

    @Test
    void 용어명을_몰라도_설명형_질문으로_찾는다() {
        TermRetrievalService service = MlTestFixtures.seedTermSearch();
        MlDtos.TermSearchResponse result = service.search(new MlDtos.TermSearchRequest(
                "회사가 버는 돈에 비해 주가가 몇 배인지 보는 숫자", 5));

        assertThat(result.results()).extracting(MlDtos.TermHit::slug).contains("per");
        assertThat(result.indexedTerms()).isEqualTo(MlTestFixtures.GLOSSARY.terms().size());
        assertThat(result.modelVersion()).isEqualTo(MlTestFixtures.MODELS.terms().version());
    }

    @Test
    void 공개된_용어만_검색하고_공개_목록이_바뀌면_이벤트로_다시_만든다() {
        GlossaryTerm etf = term("etf", "ETF", "여러 자산을 담아 거래소에서 사고파는 펀드.");
        List<GlossaryTerm> published = new ArrayList<>(List.of(etf));
        TermRetrievalService service = new TermRetrievalService(MlTestFixtures.MODELS, () -> List.copyOf(published));

        assertThat(service.search(new MlDtos.TermSearchRequest("ETF가 뭔가요?", 5)).results())
                .extracting(MlDtos.TermHit::slug).containsExactly("etf");

        // 관리자가 새 용어를 게시한 상황. 모델에 없던 용어도 IDF 최댓값으로 바로 검색된다.
        published.add(term("new-term", "신규지표", "관리자가 새로 게시한 교육용 지표."));
        assertThat(service.search(new MlDtos.TermSearchRequest("신규지표", 5)).results()).isEmpty();
        service.onGlossaryChanged(new GlossaryContentChangedEvent("new-term", "PUBLISHED"));
        assertThat(service.search(new MlDtos.TermSearchRequest("신규지표", 5)).results())
                .extracting(MlDtos.TermHit::slug).first().isEqualTo("new-term");
    }

    @Test
    void 공개된_용어가_없으면_결과도_없다() {
        TermRetrievalService service = new TermRetrievalService(MlTestFixtures.MODELS, List::of);
        assertThat(service.search(new MlDtos.TermSearchRequest("공매도가 뭔가요?", null)).results()).isEmpty();
    }

    private static GlossaryTerm term(String slug, String name, String definition) {
        GlossaryTerm term = new GlossaryTerm(slug);
        term.setTerm(name);
        term.setEnglish(name);
        term.setDefinition(definition);
        term.setExplanation(definition);
        term.setCategoryId("market");
        return term;
    }
}
