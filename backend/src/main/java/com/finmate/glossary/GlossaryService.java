package com.finmate.glossary;

import com.finmate.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GlossaryService {
    private final GlossaryTermRepository terms;
    private final GlossaryCategoryRepository categories;
    private final GlossaryBookmarkRepository bookmarks;
    private final GlossarySeedData source;

    public GlossaryService(GlossaryTermRepository terms, GlossaryCategoryRepository categories,
                           GlossaryBookmarkRepository bookmarks, GlossarySeedData source) {
        this.terms = terms;
        this.categories = categories;
        this.bookmarks = bookmarks;
        this.source = source;
    }

    /** 시드 버전 + 공개 용어 집합의 해시. 관리자가 게시하거나 공개를 내리면 바뀌어 퀴즈 버전 충돌을 잡는다. */
    @Transactional(readOnly = true)
    public String contentVersion() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(source.version().getBytes(StandardCharsets.UTF_8));
            terms.findByStatus(GlossaryStatus.PUBLISHED).stream()
                    .sorted(Comparator.comparing(GlossaryTerm::getSlug))
                    .forEach(term -> {
                        digest.update(term.getSlug().getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                        digest.update(term.getContentVersion().getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                    });
            return source.version() + "-" + HexFormat.of().formatHex(digest.digest(), 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<GlossaryTerm> listPublished() {
        Map<String, Integer> order = categoryOrder();
        return terms.findByStatus(GlossaryStatus.PUBLISHED).stream()
                .sorted(Comparator.comparingInt((GlossaryTerm term) -> order.getOrDefault(term.getCategoryId(), 999))
                        .thenComparing(GlossaryTerm::getTerm)
                        .thenComparing(GlossaryTerm::getSlug))
                .toList();
    }

    @Transactional(readOnly = true)
    public GlossaryTerm findPublished(String slug) {
        return terms.findById(slug)
                .filter(term -> term.getStatus() == GlossaryStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("용어를 찾을 수 없습니다: " + slug));
    }

    @Transactional(readOnly = true)
    public GlossaryDtos.CategoriesResponse categories() {
        Map<String, Long> counts = listPublished().stream()
                .collect(Collectors.groupingBy(GlossaryTerm::getCategoryId, Collectors.counting()));
        List<GlossaryDtos.Category> items = categories.findAllByOrderBySortOrderAsc().stream()
                .map(category -> new GlossaryDtos.Category(category.getId(), category.getSortOrder(),
                        category.getName(), category.getDescription(), counts.getOrDefault(category.getId(), 0L)))
                .toList();
        return new GlossaryDtos.CategoriesResponse(contentVersion(), items);
    }

    @Transactional(readOnly = true)
    public GlossaryDtos.TermsResponse terms(String query, String category, String level) {
        if (query != null && query.length() > 100) throw ApiException.badRequest("검색어는 100자 이하여야 합니다.");
        if (category != null && !category.isBlank() && !categories.existsById(category)) {
            throw ApiException.badRequest("존재하지 않는 용어 분류입니다: " + category);
        }
        String levelFilter = level == null || level.isBlank() ? null : level.toUpperCase(Locale.ROOT);
        if (levelFilter != null && !List.of("BASIC", "INTERMEDIATE", "ADVANCED").contains(levelFilter)) {
            throw ApiException.badRequest("학습 수준은 BASIC, INTERMEDIATE, ADVANCED 중 하나여야 합니다.");
        }
        String needle = normalize(query);
        List<GlossaryDtos.TermSummary> items = listPublished().stream()
                .filter(term -> category == null || category.isBlank() || category.equals(term.getCategoryId()))
                .filter(term -> levelFilter == null || levelFilter.equals(term.getLevel()))
                .filter(term -> needle.isEmpty() || searchable(term, needle))
                .map(GlossaryService::summary)
                .toList();
        return new GlossaryDtos.TermsResponse(contentVersion(), items.size(), items);
    }

    @Transactional(readOnly = true)
    public GlossaryDtos.TermDetail detail(String slug) {
        GlossaryTerm term = findPublished(slug);
        List<String> publishedSlugs = listPublished().stream().map(GlossaryTerm::getSlug).toList();
        return new GlossaryDtos.TermDetail(term.getSlug(), term.getTerm(), term.getFullName(), term.getEnglish(),
                term.getAliases(), term.getCategoryId(), term.getLevel(), term.getDefinition(),
                term.getExplanation(), term.getFormula(), term.getExample(), term.getCaution(),
                term.getRelated().stream().filter(publishedSlugs::contains).toList(),
                term.getConceptSlug(), term.getSourceName(), term.getSourceUrl(), term.getContentVersion(),
                GlossaryDtos.EDUCATION_NOTICE);
    }

    @Transactional(readOnly = true)
    public GlossaryDtos.BookmarksResponse bookmarks(String userId) {
        Map<String, GlossaryTerm> published = listPublished().stream()
                .collect(Collectors.toMap(GlossaryTerm::getSlug, Function.identity()));
        List<GlossaryDtos.TermSummary> items = bookmarks.findByIdUserIdOrderByCreatedAtDesc(userId).stream()
                .map(bookmark -> published.get(bookmark.getId().getTermSlug()))
                .filter(term -> term != null)
                .map(GlossaryService::summary)
                .toList();
        return new GlossaryDtos.BookmarksResponse(items);
    }

    @Transactional
    public void addBookmark(String userId, String slug) {
        findPublished(slug);
        GlossaryBookmarkId id = new GlossaryBookmarkId(userId, slug);
        if (!bookmarks.existsById(id)) bookmarks.save(new GlossaryBookmark(userId, slug));
    }

    @Transactional
    public void removeBookmark(String userId, String slug) {
        bookmarks.deleteById(new GlossaryBookmarkId(userId, slug));
    }

    private Map<String, Integer> categoryOrder() {
        return categories.findAllByOrderBySortOrderAsc().stream()
                .collect(Collectors.toMap(GlossaryCategory::getId, GlossaryCategory::getSortOrder));
    }

    private static GlossaryDtos.TermSummary summary(GlossaryTerm term) {
        return new GlossaryDtos.TermSummary(term.getSlug(), term.getTerm(), term.getEnglish(),
                term.getCategoryId(), term.getLevel(), term.getDefinition());
    }

    private static boolean searchable(GlossaryTerm term, String needle) {
        if (normalize(term.getTerm()).contains(needle) || normalize(term.getEnglish()).contains(needle)
                || normalize(term.getFullName()).contains(needle) || normalize(term.getDefinition()).contains(needle)) {
            return true;
        }
        return term.getAliases().stream().anyMatch(alias -> normalize(alias).contains(needle));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }
}
