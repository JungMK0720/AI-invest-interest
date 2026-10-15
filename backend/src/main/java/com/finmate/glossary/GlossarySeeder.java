package com.finmate.glossary;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Inserts missing seed content after the schema is ready without overwriting editor changes. */
@Component
public class GlossarySeeder implements ApplicationRunner {
    private final GlossarySeedData source;
    private final GlossaryCategoryRepository categories;
    private final GlossaryTermRepository terms;
    private final TransactionTemplate transactions;

    public GlossarySeeder(GlossarySeedData source, GlossaryCategoryRepository categories,
                          GlossaryTermRepository terms, TransactionTemplate transactions) {
        this.source = source;
        this.categories = categories;
        this.terms = terms;
        this.transactions = transactions;
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
    }
}
