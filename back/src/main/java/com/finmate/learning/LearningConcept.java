package com.finmate.learning;

import com.finmate.glossary.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

/**
 * DB에 저장된 학습 개념. 시드는 없는 slug만 넣으므로 관리자가 게시한 수정본은 재시작 후에도 유지된다.
 * 확인 문제는 {@link LearningQuizItem}에 따로 두고, 게시할 때 개념과 한 단위로 함께 바뀐다.
 */
@Entity
@Table(name = "learning_concepts", indexes = {
        @Index(name = "idx_learning_concepts_parent", columnList = "parent_slug"),
        @Index(name = "idx_learning_concepts_status", columnList = "status")
})
public class LearningConcept {

    @Id
    @Column(length = 120)
    private String slug;

    @Column(name = "parent_slug", length = 120)
    private String parentSlug;

    @Column(nullable = false)
    private int level;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 300)
    private String summary;

    @Column(name = "one_liner", nullable = false, length = 500)
    private String oneLiner;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> explanation = List.of();

    @Convert(converter = StringListConverter.class)
    @Column(name = "key_points", nullable = false, columnDefinition = "text")
    private List<String> keyPoints = List.of();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> checklist = List.of();

    @Column(columnDefinition = "text")
    private String misconception;

    @Column(name = "diagram_key", nullable = false, length = 40)
    private String diagramKey;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 600)
    private String sourceUrl;

    @Column(name = "simulation_id", length = 80)
    private String simulationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearningConceptStatus status = LearningConceptStatus.DRAFT;

    @Column(name = "content_version", nullable = false, length = 80)
    private String contentVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LearningConcept() { }

    public LearningConcept(String slug) { this.slug = slug; }

    @PrePersist
    @PreUpdate
    void stamp() { updatedAt = Instant.now(); }

    /** 화면·검증용 노드로 옮긴다. 확인 문제는 호출하는 쪽이 같은 개념의 문항을 정렬해 넘긴다. */
    public LearningContent.Node toNode(List<LearningContent.QuizItem> quiz) {
        return new LearningContent.Node(slug, parentSlug, level, sortOrder, title, summary, oneLiner, getExplanation(),
                getKeyPoints(), getChecklist(), misconception, diagramKey, sourceName, sourceUrl, simulationId,
                List.copyOf(quiz));
    }

    /** 노드의 본문을 옮겨 담는다. slug·상태·버전은 바꾸지 않는다. */
    public void apply(LearningContent.Node node) {
        this.parentSlug = node.parent();
        this.level = node.level();
        this.sortOrder = node.order();
        this.title = node.title();
        this.summary = node.summary();
        this.oneLiner = node.oneLiner();
        this.explanation = List.copyOf(node.explanation());
        this.keyPoints = List.copyOf(node.keyPoints());
        this.checklist = List.copyOf(node.checklist());
        this.misconception = node.misconception();
        this.diagramKey = node.diagramKey();
        this.sourceName = node.sourceName();
        this.sourceUrl = node.sourceUrl();
        this.simulationId = node.simulationId();
    }

    public String getSlug() { return slug; }
    public String getParentSlug() { return parentSlug; }
    public int getLevel() { return level; }
    public int getSortOrder() { return sortOrder; }
    public String getTitle() { return title; }
    public List<String> getExplanation() { return explanation == null ? List.of() : List.copyOf(explanation); }
    public List<String> getKeyPoints() { return keyPoints == null ? List.of() : List.copyOf(keyPoints); }
    public List<String> getChecklist() { return checklist == null ? List.of() : List.copyOf(checklist); }
    public LearningConceptStatus getStatus() { return status; }
    public String getContentVersion() { return contentVersion; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(LearningConceptStatus status) { this.status = status; }
    public void setContentVersion(String contentVersion) { this.contentVersion = contentVersion; }
}
