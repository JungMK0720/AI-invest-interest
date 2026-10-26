package com.finmate.admin;

import com.finmate.learning.LearningContent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AdminLearningDtos {

    private AdminLearningDtos() { }

    /** 선택지 하나. 오답에도 "왜 틀렸는지"가 있어야 하므로 해설은 모든 선택지에 필수다. */
    public record OptionInput(
            @NotBlank @Size(max = 300) String label,
            boolean correct,
            @NotBlank @Size(max = 1000) String rationale) { }

    /** 확인 문제 한 문항. id는 복습 기록의 키이므로 전체 개념에서 겹치면 안 된다. */
    public record QuizItemInput(
            @NotBlank @Size(max = 120) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                    message = "문항 id는 영문 소문자·숫자와 하이픈만 사용할 수 있습니다.") String id,
            @NotBlank @Size(max = 500) String prompt,
            @NotBlank @Size(max = 20) String difficulty,
            @NotNull @Size(min = 3, max = 6) List<@NotNull @Valid OptionInput> options,
            @NotBlank @Size(max = 1000) String takeaway) { }

    /**
     * PUT은 개념 본문과 확인 문제 전체를 교체하는 초안 입력이다. 공개 원본은 publish 전까지 유지된다.
     * 개념 하나와 그 문항들이 하나의 개정 단위다.
     */
    public record EditRequest(
            @Size(max = 120) String parent,
            @Min(1) @Max(3) int level,
            @Min(0) @Max(999) int order,
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 300) String summary,
            @NotBlank @Size(max = 500) String oneLiner,
            @NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 2000) String> explanation,
            @NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 300) String> keyPoints,
            @NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 300) String> checklist,
            @NotBlank @Size(max = 1000) String misconception,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Z][A-Z_]*",
                    message = "그림 키는 영문 대문자와 밑줄만 사용할 수 있습니다.") String diagramKey,
            @NotBlank @Size(max = 200) String sourceName,
            @NotBlank @Size(max = 600) String sourceUrl,
            @Size(max = 80) String simulationId,
            @NotNull @Size(max = 10) List<@NotNull @Valid QuizItemInput> quiz) {

        public LearningContent.Node toNode(String slug) {
            return new LearningContent.Node(slug, blankToNull(parent), level, order, title, summary, oneLiner,
                    List.copyOf(explanation), List.copyOf(keyPoints), List.copyOf(checklist), misconception,
                    diagramKey, sourceName, sourceUrl, blankToNull(simulationId),
                    quiz.stream().map(item -> new LearningContent.QuizItem(item.id(), item.prompt(), item.difficulty(),
                            item.options().stream().map(option -> new LearningContent.Option(
                                    option.label(), option.correct(), option.rationale())).toList(),
                            item.takeaway())).toList());
        }

        public static EditRequest from(LearningContent.Node node) {
            return new EditRequest(node.parent(), node.level(), node.order(), node.title(), node.summary(),
                    node.oneLiner(), node.explanation(), node.keyPoints(), node.checklist(), node.misconception(),
                    node.diagramKey(), node.sourceName(), node.sourceUrl(), node.simulationId(),
                    node.quiz().stream().map(item -> new QuizItemInput(item.id(), item.prompt(),
                            item.difficulty() == null ? "BASIC" : item.difficulty(),
                            item.options().stream().map(option -> new OptionInput(
                                    option.label(), option.correct(), option.rationale())).toList(),
                            item.takeaway())).toList());
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }

    /** 편집용 개념. content는 초안이 있으면 초안, 없으면 저장된 내용이다. */
    public record Concept(String slug, EditRequest content, String status, String contentVersion,
                          Instant updatedAt, boolean hasDraft) { }

    /** 목록 한 줄. 트리 순서(상위 → 하위, 같은 층은 order 순)로 내려간다. */
    public record ConceptSummary(String slug, String parent, int level, String title, int quizCount, String status,
                                 String contentVersion, Instant updatedAt, boolean hasDraft) { }

    public record Concepts(String learningContentVersion, List<ConceptSummary> items) { }

    public record Revision(String id, String action, String contentVersion, String editorEmail, Instant createdAt) { }

    public record Revisions(String slug, List<Revision> items) { }
}
