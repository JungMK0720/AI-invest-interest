package com.finmate.ml;

import com.finmate.common.ApiException;
import com.finmate.glossary.GlossaryService;
import com.finmate.glossary.GlossaryTerm;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 학습된 TF-IDF 지수로 질문에 가까운 공개 용어를 찾는다. */
@Service
public class TermRetrievalService {

    private final MlModelCatalog.TermModel model;
    private final Supplier<List<GlossaryTerm>> publishedTerms;
    private final List<IndexedDocument> index;

    @Autowired
    public TermRetrievalService(MlModelCatalog catalog, GlossaryService glossary) {
        this(catalog, glossary::listPublished);
    }

    TermRetrievalService(MlModelCatalog catalog, Supplier<List<GlossaryTerm>> publishedTerms) {
        this.model = catalog.terms();
        this.publishedTerms = publishedTerms;
        this.index = model.documents().stream().map(doc -> {
            Vector vector = vector(TextFeatures.grams(doc.text()), model.idf());
            return new IndexedDocument(doc, vector);
        }).toList();
    }

    public MlDtos.TermSearchResponse search(MlDtos.TermSearchRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()
                || request.question().length() > 500
                || (request.limit() != null && (request.limit() < 1 || request.limit() > 10))) {
            throw ApiException.badRequest("question(1~500자)과 limit(1~10)을 입력해 주세요.");
        }
        int limit = request.limit() == null ? 5 : request.limit();
        Vector query = vector(TextFeatures.grams(request.question()), model.idf());
        if (query.norm() == 0) {
            return new MlDtos.TermSearchResponse(List.of(), model.version());
        }
        Map<String, GlossaryTerm> published = new HashMap<>();
        for (GlossaryTerm term : publishedTerms.get()) {
            published.put(term.getSlug(), term);
        }
        String compactQuestion = TextFeatures.normalize(request.question());
        List<MlDtos.TermHit> hits = new ArrayList<>();
        for (IndexedDocument item : index) {
            GlossaryTerm current = published.get(item.document().slug());
            if (current == null || item.vector().norm() == 0) {
                continue;
            }
            double dot = 0;
            for (Map.Entry<String, Double> entry : query.weights().entrySet()) {
                dot += entry.getValue() * item.vector().weights().getOrDefault(entry.getKey(), 0.0);
            }
            double score = dot / (query.norm() * item.vector().norm());
            List<String> names = new ArrayList<>();
            names.add(current.getTerm());
            if (current.getAliases() != null) names.addAll(current.getAliases());
            boolean exactName = names.stream().filter(Objects::nonNull)
                    .map(TextFeatures::normalize)
                    .anyMatch(name -> name.length() >= 2 && compactQuestion.contains(name));
            if (exactName) score += 0.35;
            if (score > 0) {
                hits.add(new MlDtos.TermHit(item.document().slug(), current.getTerm(),
                        Math.round(Math.min(score, 1.0) * 10000) / 10000.0));
            }
        }
        hits.sort(Comparator.comparingDouble(MlDtos.TermHit::score).reversed()
                .thenComparing(MlDtos.TermHit::slug));
        return new MlDtos.TermSearchResponse(List.copyOf(hits.subList(0, Math.min(limit, hits.size()))), model.version());
    }

    private static Vector vector(Map<String, Integer> counts, Map<String, Double> idf) {
        Map<String, Double> weights = new HashMap<>();
        double sumSquares = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Double importance = idf.get(entry.getKey());
            if (importance == null) continue;
            double weight = (1 + Math.log(entry.getValue())) * importance;
            weights.put(entry.getKey(), weight);
            sumSquares += weight * weight;
        }
        return new Vector(Map.copyOf(weights), Math.sqrt(sumSquares));
    }

    private record Vector(Map<String, Double> weights, double norm) {}

    private record IndexedDocument(MlModelCatalog.TermDocument document, Vector vector) {}
}
