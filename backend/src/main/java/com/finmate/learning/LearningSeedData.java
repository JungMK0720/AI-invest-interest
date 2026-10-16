package com.finmate.learning;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * 작성된 학습 개념 JSON(learning/concepts.json)을 읽고 검증한다.
 *
 * <p>규칙을 어긴 콘텐츠가 배포되면 기동 자체가 실패한다. 화면에서 조용히 깨지는 것보다 배포 시점에
 * 드러나는 편이 안전하다. 여기서 읽은 트리는 DB 시드({@link LearningSeeder})와 신호 규칙의 링크 검증에만 쓰고,
 * 화면은 DB의 공개 개념({@link LearningCatalog})을 본다.
 */
@Component
public class LearningSeedData {

    private static final String RESOURCE_PATH = "learning/concepts.json";

    private final LearningTree tree;

    public LearningSeedData(ObjectMapper mapper) {
        LearningContent.Catalog catalog;
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            catalog = mapper.readValue(in, LearningContent.Catalog.class);
        } catch (IOException e) {
            throw new IllegalStateException("학습 콘텐츠를 읽지 못했습니다: " + RESOURCE_PATH, e);
        }
        if (catalog.concepts() == null || catalog.concepts().isEmpty()) {
            throw new IllegalStateException("학습 콘텐츠가 비어 있습니다.");
        }
        if (catalog.version() == null || catalog.version().isBlank()) {
            throw new IllegalStateException("학습 콘텐츠 버전이 없습니다.");
        }
        this.tree = LearningTree.of(catalog.version(), catalog.concepts());
    }

    /** 시드 파일의 콘텐츠 버전. DB 카탈로그 버전의 앞부분이 된다. */
    public String version() {
        return tree.version();
    }

    public LearningTree tree() {
        return tree;
    }
}
