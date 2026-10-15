package com.finmate.glossary;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Reads and validates the authored JSON before any rows are imported. */
@Component
public class GlossarySeedData {
    private final String version;
    private final List<GlossaryCategory> categories;
    private final List<GlossaryTerm> terms;

    public GlossarySeedData(ObjectMapper mapper) {
        try {
            CategoryFile categoryFile;
            try (InputStream input = new ClassPathResource("glossary/categories.json").getInputStream()) {
                categoryFile = mapper.readValue(input, CategoryFile.class);
            }
            if (categoryFile.categories() == null || categoryFile.categories().isEmpty()) {
                throw new IllegalStateException("용어 분류가 비어 있습니다.");
            }
            this.version = required(categoryFile.version(), "contentVersion");
            Set<String> categoryIds = new HashSet<>();
            List<GlossaryCategory> loadedCategories = new ArrayList<>();
            for (CategorySource source : categoryFile.categories()) {
                String id = required(source.id(), "category id");
                if (!categoryIds.add(id)) throw new IllegalStateException("중복된 용어 분류: " + id);
                loadedCategories.add(new GlossaryCategory(id, source.order(),
                        required(source.name(), "category name"), required(source.description(), "category description")));
            }
            this.categories = List.copyOf(loadedCategories);

            Resource[] files = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:glossary/terms/*.json");
            if (files.length == 0) throw new IllegalStateException("용어 JSON 파일이 없습니다.");
            List<Resource> sortedFiles = new ArrayList<>(List.of(files));
            sortedFiles.sort(Comparator.comparing(Resource::getFilename));
            List<GlossaryTerm> loadedTerms = new ArrayList<>();
            Set<String> slugs = new HashSet<>();
            for (Resource file : sortedFiles) {
                TermFile termFile;
                try (InputStream input = file.getInputStream()) {
                    termFile = mapper.readValue(input, TermFile.class);
                }
                if (!categoryIds.contains(termFile.category())) {
                    throw new IllegalStateException("존재하지 않는 용어 분류: " + termFile.category());
                }
                if (termFile.terms() == null || termFile.terms().isEmpty()) {
                    throw new IllegalStateException("빈 용어 파일: " + file.getFilename());
                }
                for (TermSource source : termFile.terms()) {
                    String slug = required(source.slug(), "term slug");
                    if (!slugs.add(slug)) throw new IllegalStateException("중복된 용어 slug: " + slug);
                    GlossaryTerm term = new GlossaryTerm(slug);
                    term.setCategoryId(termFile.category());
                    term.setTerm(required(source.term(), slug + " term"));
                    term.setFullName(source.fullName());
                    term.setEnglish(required(source.english(), slug + " english"));
                    term.setAliases(source.aliases());
                    term.setLevel(required(source.level(), slug + " level"));
                    term.setDefinition(required(source.definition(), slug + " definition"));
                    term.setExplanation(required(source.explanation(), slug + " explanation"));
                    term.setFormula(source.formula());
                    term.setExample(source.example());
                    term.setCaution(source.caution());
                    term.setRelated(source.related());
                    term.setConceptSlug(required(source.conceptSlug(), slug + " conceptSlug"));
                    term.setSourceName(required(source.sourceName(), slug + " sourceName"));
                    term.setSourceUrl(required(source.sourceUrl(), slug + " sourceUrl"));
                    term.setStatus(GlossaryStatus.PUBLISHED);
                    term.setContentVersion(version);
                    loadedTerms.add(term);
                }
            }
            for (GlossaryTerm term : loadedTerms) {
                for (String related : term.getRelated()) {
                    if (!slugs.contains(related)) {
                        throw new IllegalStateException("존재하지 않는 관련 용어: " + term.getSlug() + " -> " + related);
                    }
                }
            }
            this.terms = List.copyOf(loadedTerms);
        } catch (IOException e) {
            throw new IllegalStateException("용어 JSON을 읽지 못했습니다.", e);
        }
    }

    public String version() { return version; }
    public List<GlossaryCategory> categories() { return categories; }
    public List<GlossaryTerm> terms() { return terms; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalStateException("필수 용어 데이터가 없습니다: " + field);
        return value;
    }

    private record CategoryFile(String version, List<CategorySource> categories) { }
    private record CategorySource(String id, int order, String name, String description) { }
    private record TermFile(String category, List<TermSource> terms) { }
    private record TermSource(String slug, String term, String fullName, String english, List<String> aliases,
                              String level, String definition, String explanation, String formula, String example,
                              String caution, List<String> related, String conceptSlug, String sourceName,
                              String sourceUrl) { }
}
