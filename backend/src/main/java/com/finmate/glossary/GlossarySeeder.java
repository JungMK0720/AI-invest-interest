package com.finmate.glossary;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 스키마가 준비된 뒤 빠진 시드 분류·용어만 넣는다. 관리자가 게시한 수정본은 덮어쓰지 않는다.
 *
 * <p>웹 서버는 이 러너보다 먼저 요청을 받을 수 있으므로, 적재가 끝나면 이벤트를 보내
 * 그사이 비어 있는 상태로 만들어진 검색 인덱스를 버리게 한다.
 */
@Component
public class GlossarySeeder implements ApplicationRunner {
    private final GlossarySeedData source;
    private final GlossaryCategoryRepository categories;
    private final GlossaryTermRepository terms;
    private final TransactionTemplate transactions;
    private final ApplicationEventPublisher events;

    public GlossarySeeder(GlossarySeedData source, GlossaryCategoryRepository categories,
                          GlossaryTermRepository terms, TransactionTemplate transactions,
                          ApplicationEventPublisher events) {
        this.source = source;
        this.categories = categories;
        this.terms = terms;
        this.transactions = transactions;
        this.events = events;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactions.executeWithoutResult(ignored -> {
            List<GlossaryCategory> missingCategories = new ArrayList<>();
            for (GlossaryCategory category : source.categories()) {
                if (!categories.existsById(category.getId())) missingCategories.add(category);
            }
            categories.saveAll(missingCategories);

            List<GlossaryTerm> missingTerms = new ArrayList<>();
            for (GlossaryTerm term : source.terms()) {
                if (!terms.existsById(term.getSlug())) missingTerms.add(term);
            }
            terms.saveAll(missingTerms);
        });
        events.publishEvent(new GlossaryContentChangedEvent(null, "SEEDED"));
    }
}
