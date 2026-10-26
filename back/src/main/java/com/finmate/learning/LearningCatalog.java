package com.finmate.learning;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 학습 카탈로그. DB의 공개 개념·확인 문제로 만든 {@link LearningTree}를 캐시해 두고 화면과 채점에 내준다.
 *
 * <p>관리자가 개념을 게시하거나 공개를 내리면 {@link LearningContentChangedEvent}를 커밋 뒤에 받아 캐시를 버리고,
 * 다음 요청에서 다시 만든다. 그래서 수정본이 재시작 없이 바로 학습 화면에 반영된다. 커밋 전에 버리면 다른 요청이
 * 옛 내용으로 캐시를 다시 채울 수 있어 커밋 뒤에만 버린다.
 *
 * <p>한 요청 안에서 여러 번 조회할 때는 {@link #current()}로 트리 한 벌을 받아 쓴다. 조회 사이에 게시가 끼어도
 * 같은 요청은 같은 버전을 보게 된다.
 */
@Service
public class LearningCatalog {

    private final Supplier<LearningTree> loader;
    private final AtomicLong generation = new AtomicLong();
    private volatile LearningTree current;

    @Autowired
    public LearningCatalog(LearningContentStore store) {
        this(store::loadPublished);
    }

    /** 스프링 없이 고정된 트리로 쓸 때(단위 테스트·시드 검증) 사용한다. */
    public LearningCatalog(Supplier<LearningTree> loader) {
        this.loader = loader;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onContentChanged(LearningContentChangedEvent event) {
        generation.incrementAndGet();
        current = null;
    }

    /** 지금 공개 중인 트리. 다시 만드는 사이에 무효화가 오면 결과를 캐시하지 않아 옛 내용이 남지 않는다. */
    public LearningTree current() {
        LearningTree tree = current;
        if (tree != null) {
            return tree;
        }
        synchronized (this) {
            tree = current;
            if (tree != null) {
                return tree;
            }
            long seen = generation.get();
            tree = loader.get();
            if (generation.get() == seen) {
                current = tree;
            }
            return tree;
        }
    }

    public String version() {
        return current().version();
    }

    public List<LearningContent.Node> nodes() {
        return current().nodes();
    }

    public List<String> rootSlugs() {
        return current().rootSlugs();
    }

    public List<String> childSlugs(String slug) {
        return current().childSlugs(slug);
    }

    public List<LearningContent.Node> descendants(String slug) {
        return current().descendants(slug);
    }

    public List<LearningContent.Node> path(String slug) {
        return current().path(slug);
    }

    public LearningContent.Node node(String slug) {
        return current().node(slug);
    }

    public Optional<LearningContent.Node> lookup(String slug) {
        return current().lookup(slug);
    }

    public boolean contains(String slug) {
        return current().contains(slug);
    }

    /** 기존 /learning/concepts 응답 형태를 유지하기 위한 평면 목록 (하위 호환). */
    public List<Concept> list() {
        return current().nodes().stream().map(LearningCatalog::concept).toList();
    }

    public Concept find(String slug) {
        return concept(node(slug));
    }

    private static Concept concept(LearningContent.Node node) {
        return new Concept(node.slug(), node.title(), node.summary(), String.join(" ", node.explanation()),
                node.diagramKey(), node.sourceUrl());
    }

    /** v1 응답 호환용 축약 표현. 신규 화면은 LearningDtos.ConceptDetail을 사용한다. */
    public record Concept(String slug, String title, String summary, String explanation, String diagramKey, String sourceUrl) {
    }
}
