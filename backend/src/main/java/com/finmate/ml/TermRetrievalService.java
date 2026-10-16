package com.finmate.ml;

import com.finmate.common.ApiException;
import com.finmate.glossary.GlossaryContentChangedEvent;
import com.finmate.glossary.GlossaryService;
import com.finmate.glossary.GlossaryTerm;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 학습된 IDF와 필드 가중치로 질문에 가까운 공개 용어를 찾는다(ml/finmate_ml/retrieval.py와 같은 계산).
 *
 * <p>문서 벡터는 모델 파일이 아니라 DB의 공개 용어로 만든다. 관리자가 용어를 게시·비공개 처리하면
 * {@link GlossaryContentChangedEvent}를 받아 인덱스를 버리고, 다음 검색에서 새로 만든다.
 * 그래서 수정본이 재학습·재배포 없이 바로 검색에 반영된다.
 */
@Service
public class TermRetrievalService {

    private static final List<String> FIELDS = List.of("name", "definition", "body");

    private final MlModelCatalog.TermModel model;
    private final Supplier<List<GlossaryTerm>> publishedTerms;
    private volatile Index index;

    @Autowired
    public TermRetrievalService(MlModelCatalog catalog, GlossaryService glossary) {
        this(catalog, glossary::listPublished);
    }

    TermRetrievalService(MlModelCatalog catalog, Supplier<List<GlossaryTerm>> publishedTerms) {
        this.model = catalog.terms();
        this.publishedTerms = publishedTerms;
    }

    @EventListener
    public void onGlossaryChanged(GlossaryContentChangedEvent event) {
        index = null;
    }

    public MlDtos.TermSearchResponse search(MlDtos.TermSearchRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()
                || request.question().length() > 500
                || (request.limit() != null && (request.limit() < 1 || request.limit() > 10))) {
            throw ApiException.badRequest("question(1~500자)과 limit(1~10)을 입력해 주세요.");
        }
        Index current = index();
        int limit = request.limit() == null ? 5 : request.limit();
        List<MlDtos.TermHit> hits = rank(current, request.question(), limit).stream()
                .map(item -> new MlDtos.TermHit(item.term().getSlug(), item.term().getTerm(),
                        item.term().getCategoryId(), item.term().getDefinition(),
                        Math.round(item.score() * 10000) / 10000.0, item.nameMatched()))
                .toList();
        return new MlDtos.TermSearchResponse(hits, model.version(), current.entries().size());
    }

    /** 반올림 전 점수. Python 정답 파일과 일치를 검사할 때 쓴다. */
    List<Ranked> rank(String question, int limit) {
        return rank(index(), question, limit);
    }

    public Map<String, Object> runtime() {
        Index current = index;
        Map<String, Object> info = new HashMap<>();
        info.put("termModelVersion", model.version());
        info.put("indexedTerms", current == null ? null : current.entries().size());
        info.put("indexBuiltAt", current == null ? null : current.builtAt());
        return info;
    }

    private Index index() {
        Index current = index;
        if (current == null) {
            synchronized (this) {
                current = index;
                if (current == null) {
                    current = build(publishedTerms.get());
                    index = current;
                }
            }
        }
        return current;
    }

    private Index build(List<GlossaryTerm> terms) {
        List<Entry> entries = new ArrayList<>();
        for (GlossaryTerm term : terms) {
            Map<String, Vector> fields = new HashMap<>();
            fields.put("name", vector(join(Stream.concat(
                    Stream.of(term.getTerm(), term.getFullName(), term.getEnglish()), term.getAliases().stream()))));
            fields.put("definition", vector(term.getDefinition()));
            fields.put("body", vector(join(Stream.of(term.getExplanation(), term.getCaution()))));
            List<String> otherNames = Stream.concat(Stream.of(term.getFullName(), term.getEnglish()),
                    term.getAliases().stream()).filter(name -> name != null && !name.isEmpty()).toList();
            entries.add(new Entry(term, fields, otherNames));
        }
        return new Index(List.copyOf(entries), Instant.now());
    }

    private List<Ranked> rank(Index current, String question, int limit) {
        Vector query = vector(question);
        List<Ranked> ranked = new ArrayList<>();
        for (Entry entry : current.entries()) {
            double score = 0;
            for (String field : FIELDS) {
                score += model.fieldWeights().getOrDefault(field, 0.0) * cosine(query, entry.fields().get(field));
            }
            boolean termMatch = TextFeatures.nameMatches(entry.term().getTerm(), question);
            boolean aliasMatch = !termMatch && entry.otherNames().stream()
                    .anyMatch(name -> TextFeatures.nameMatches(name, question));
            if (termMatch) {
                score += model.termBoost();
            } else if (aliasMatch) {
                score += model.termBoost() * model.aliasFactor();
            }
            if (score > 0) {
                ranked.add(new Ranked(entry.term(), score, termMatch || aliasMatch));
            }
        }
        ranked.sort(Comparator.comparingDouble(Ranked::score).reversed()
                .thenComparing(item -> item.term().getSlug()));
        return List.copyOf(ranked.subList(0, Math.min(limit, ranked.size())));
    }

    private static String join(Stream<String> parts) {
        return String.join(" ", parts.filter(Objects::nonNull).filter(part -> !part.isEmpty()).toList());
    }

    private Vector vector(String text) {
        Map<String, Double> weights = new HashMap<>();
        double sumSquares = 0;
        for (Map.Entry<String, Integer> entry : TextFeatures.grams(text).entrySet()) {
            double weight = (1 + Math.log(entry.getValue())) * model.idf().getOrDefault(entry.getKey(), model.idfMax());
            weights.put(entry.getKey(), weight);
            sumSquares += weight * weight;
        }
        return new Vector(weights, Math.sqrt(sumSquares));
    }

    private static double cosine(Vector query, Vector document) {
        if (query.norm() == 0 || document.norm() == 0) return 0;
        Map<String, Double> small = query.weights().size() <= document.weights().size() ? query.weights() : document.weights();
        Map<String, Double> large = small == query.weights() ? document.weights() : query.weights();
        double dot = 0;
        for (Map.Entry<String, Double> entry : small.entrySet()) {
            dot += entry.getValue() * large.getOrDefault(entry.getKey(), 0.0);
        }
        return dot / (query.norm() * document.norm());
    }

    record Ranked(GlossaryTerm term, double score, boolean nameMatched) {}

    private record Vector(Map<String, Double> weights, double norm) {}

    private record Entry(GlossaryTerm term, Map<String, Vector> fields, List<String> otherNames) {}

    private record Index(List<Entry> entries, Instant builtAt) {}
}
