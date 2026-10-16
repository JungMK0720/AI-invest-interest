package com.finmate.learning;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB의 개념·확인 문제를 화면용 트리로 옮긴다.
 *
 * <p>콘텐츠 버전은 시드 버전 + 공개 개념 (slug, 개념 버전) 집합의 해시다. 관리자가 개념을 게시하거나 공개를
 * 내리면 값이 바뀌므로, 그 전에 불러 둔 확인 문제를 제출하면 {@link QuizGrader}가 409로 다시 불러오게 한다.
 */
@Component
public class LearningContentStore {

    private final LearningSeedData seed;
    private final LearningConceptRepository concepts;
    private final LearningQuizItemRepository quizItems;

    public LearningContentStore(LearningSeedData seed, LearningConceptRepository concepts,
                                LearningQuizItemRepository quizItems) {
        this.seed = seed;
        this.concepts = concepts;
        this.quizItems = quizItems;
    }

    /** 공개 개념으로 만든 트리. 규칙을 어긴 상태라면 예외를 던진다(게시 단계에서 미리 막는다). */
    @Transactional(readOnly = true)
    public LearningTree loadPublished() {
        List<LearningConcept> published = concepts.findByStatus(LearningConceptStatus.PUBLISHED);
        return LearningTree.of(version(published), nodes(published));
    }

    /** 개념들을 확인 문제와 함께 노드로 옮긴다. 문항은 한 번에 읽어 개념별로 나눈다. */
    @Transactional(readOnly = true)
    public List<LearningContent.Node> nodes(Collection<LearningConcept> source) {
        Map<String, List<LearningContent.QuizItem>> quiz = quizItems.findAll().stream()
                .sorted(Comparator.comparing(LearningQuizItem::getConceptSlug)
                        .thenComparingInt(LearningQuizItem::getSortOrder))
                .collect(Collectors.groupingBy(LearningQuizItem::getConceptSlug,
                        Collectors.mapping(LearningQuizItem::toItem, Collectors.toList())));
        return source.stream().map(concept -> concept.toNode(quiz.getOrDefault(concept.getSlug(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public LearningContent.Node node(LearningConcept concept) {
        return concept.toNode(quizItems.findByConceptSlugOrderBySortOrderAsc(concept.getSlug()).stream()
                .map(LearningQuizItem::toItem).toList());
    }

    public String version(Collection<LearningConcept> published) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(seed.version().getBytes(StandardCharsets.UTF_8));
            published.stream()
                    .sorted(Comparator.comparing(LearningConcept::getSlug))
                    .forEach(concept -> {
                        digest.update(concept.getSlug().getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                        digest.update(concept.getContentVersion().getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                    });
            return seed.version() + "-" + HexFormat.of().formatHex(digest.digest(), 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
