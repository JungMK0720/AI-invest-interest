package com.finmate.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.List;

/**
 * 개념에 딸린 확인 문제 한 문항.
 *
 * <p>선택지는 순서가 곧 choice ID({@code {id}-choice-{index}})이므로 문항 행 하나에 JSON 배열로 함께 저장한다.
 * 선택지만 따로 고치는 경로를 두지 않아 순서와 정답 개수가 어긋난 상태가 저장될 틈이 없다.
 */
@Entity
@Table(name = "learning_quiz_items", indexes = {
        @Index(name = "idx_learning_quiz_items_concept", columnList = "concept_slug,sort_order")
})
public class LearningQuizItem {

    @Id
    @Column(length = 120)
    private String id;

    @Column(name = "concept_slug", nullable = false, length = 120)
    private String conceptSlug;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Convert(converter = QuizOptionListConverter.class)
    @Column(name = "options_json", nullable = false, columnDefinition = "text")
    private List<LearningContent.Option> options = List.of();

    @Column(nullable = false, columnDefinition = "text")
    private String takeaway;

    protected LearningQuizItem() { }

    public LearningQuizItem(String id, String conceptSlug) {
        this.id = id;
        this.conceptSlug = conceptSlug;
    }

    public LearningContent.QuizItem toItem() {
        return new LearningContent.QuizItem(id, prompt, difficulty, getOptions(), takeaway);
    }

    public void apply(int sortOrder, LearningContent.QuizItem item) {
        this.sortOrder = sortOrder;
        this.prompt = item.prompt();
        this.difficulty = item.difficulty() == null ? "BASIC" : item.difficulty();
        this.options = List.copyOf(item.options());
        this.takeaway = item.takeaway();
    }

    public String getId() { return id; }
    public String getConceptSlug() { return conceptSlug; }
    public int getSortOrder() { return sortOrder; }
    public List<LearningContent.Option> getOptions() { return options == null ? List.of() : List.copyOf(options); }
}
