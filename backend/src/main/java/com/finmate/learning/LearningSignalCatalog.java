package com.finmate.learning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.glossary.GlossarySeedData;
import com.finmate.glossary.GlossaryTerm;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 신호(자유 서술 태그·진단 확인 플래그·성향)를 다음 학습 개념과 용어로 잇는 규칙.
 *
 * <p>규칙은 learning/signal-paths.json에 두고, 가리키는 개념·용어가 실제로 있는지 기동 시점에 검증한다.
 * 링크가 깨진 채 배포되면 추천 카드가 빈 화면으로 보이기 때문이다.
 */
@Component
public class LearningSignalCatalog {

    private static final String RESOURCE_PATH = "learning/signal-paths.json";

    private final String version;
    private final Map<String, Path> tags;
    private final Map<String, Path> reviewFlags;
    private final Map<String, Path> profiles;
    private final Map<String, String> conceptTitles;
    private final Map<String, String> termTitles;

    public LearningSignalCatalog(ObjectMapper mapper, LearningCatalog concepts, GlossarySeedData glossary) {
        Source source;
        try (InputStream input = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            source = mapper.readValue(input, Source.class);
        } catch (IOException e) {
            throw new IllegalStateException("학습 신호 규칙을 읽지 못했습니다: " + RESOURCE_PATH, e);
        }
        Set<String> termSlugs = glossary.terms().stream().map(GlossaryTerm::getSlug).collect(Collectors.toSet());
        for (Map<String, Path> group : List.of(source.tags(), source.reviewFlags(), source.profiles())) {
            group.forEach((code, path) -> {
                path.concepts().forEach(slug -> {
                    if (!concepts.contains(slug)) {
                        throw new IllegalStateException("학습 신호가 없는 개념을 가리킵니다: " + code + " -> " + slug);
                    }
                });
                path.terms().forEach(slug -> {
                    if (!termSlugs.contains(slug)) {
                        throw new IllegalStateException("학습 신호가 없는 용어를 가리킵니다: " + code + " -> " + slug);
                    }
                });
            });
        }
        this.version = source.version();
        this.tags = Map.copyOf(source.tags());
        this.reviewFlags = Map.copyOf(source.reviewFlags());
        this.profiles = Map.copyOf(source.profiles());
        this.conceptTitles = concepts.nodes().stream()
                .collect(Collectors.toUnmodifiableMap(LearningContent.Node::slug, LearningContent.Node::title));
        this.termTitles = glossary.terms().stream()
                .collect(Collectors.toUnmodifiableMap(GlossaryTerm::getSlug, GlossaryTerm::getTerm));
    }

    /** 화면 링크에 붙일 개념 제목. 모르면 slug를 그대로 돌려준다. */
    public String conceptTitle(String slug) { return conceptTitles.getOrDefault(slug, slug); }

    /** 시드 기준 용어 이름. 관리자가 이름을 바꿨다면 상세 화면에서 최신 이름이 보인다. */
    public String termTitle(String slug) { return termTitles.getOrDefault(slug, slug); }

    public String version() { return version; }

    public Optional<Path> tag(String code) { return Optional.ofNullable(tags.get(code)); }

    public Optional<Path> reviewFlag(String code) { return Optional.ofNullable(reviewFlags.get(code)); }

    public Optional<Path> profile(String profileType) { return Optional.ofNullable(profiles.get(profileType)); }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Source(String version, Map<String, Path> tags, Map<String, Path> reviewFlags,
                          Map<String, Path> profiles) {}

    /** 신호 하나가 안내하는 학습 경로. label·message는 성향 프로필 항목에서는 비어 있을 수 있다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Path(String label, String message, List<String> concepts, List<String> terms) {
        public Path {
            concepts = concepts == null ? List.of() : List.copyOf(concepts);
            terms = terms == null ? List.of() : List.copyOf(terms);
        }
    }
}
