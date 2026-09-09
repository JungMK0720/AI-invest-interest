package com.finmate.learning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 학습 콘텐츠의 원본 스키마. classpath의 learning/concepts.json을 그대로 바인딩한다.
 *
 * <p>운영 단계에서는 docs/10의 {@code concepts} / {@code content_revisions} 테이블이 이 구조를
 * 그대로 대체한다. 지금 JSON으로 두는 이유는 콘텐츠 개정 이력을 코드 변경과 분리하기 위해서다.
 * 개념 하나가 곧 하나의 개정 단위이므로 출처(sourceName/sourceUrl)를 필수로 유지한다.
 */
public final class LearningContent {

    private LearningContent() {
    }

    /** 카탈로그 전체. version은 콘텐츠 개정 버전이며 응답과 진도 저장의 기준이 된다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Catalog(String version, List<Node> concepts) {
    }

    /**
     * 개념 노드 하나. parent가 null이면 최상위 분류다.
     *
     * @param level        1=대분류, 2=상품군/핵심개념, 3=세부개념
     * @param oneLiner     한 문장 정의. docs/10의 5단계 학습 구조 1단계에 해당한다.
     * @param explanation  단락 배열. 화면에서 문단으로 렌더링한다.
     * @param keyPoints    "무엇을 확인해야 하는가"의 압축. 3~5개.
     * @param checklist    실제 상품을 볼 때 확인할 문서·항목.
     * @param misconception 자주 오해하는 지점. 확인 문제의 오답 근거와 연결된다.
     * @param diagramKey   구조 그림 종류. 프론트의 ConceptDiagram이 해석한다.
     * @param simulationId 이 개념을 직접 실험할 모의투자 시나리오 id. 없으면 null.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Node(
            String slug,
            String parent,
            int level,
            int order,
            String title,
            String summary,
            String oneLiner,
            List<String> explanation,
            List<String> keyPoints,
            List<String> checklist,
            String misconception,
            String diagramKey,
            String sourceName,
            String sourceUrl,
            String simulationId,
            List<QuizItem> quiz) {

        public List<String> explanation() {
            return explanation == null ? List.of() : explanation;
        }

        public List<String> keyPoints() {
            return keyPoints == null ? List.of() : keyPoints;
        }

        public List<String> checklist() {
            return checklist == null ? List.of() : checklist;
        }

        public List<QuizItem> quiz() {
            return quiz == null ? List.of() : quiz;
        }
    }

    /**
     * 확인 문제 한 문항.
     *
     * @param takeaway 정답 여부와 무관하게 남겨야 하는 한 줄 결론. 채점 후 항상 노출한다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuizItem(
            String id,
            String prompt,
            String difficulty,
            List<Option> options,
            String takeaway) {

        public List<Option> options() {
            return options == null ? List.of() : options;
        }
    }

    /**
     * 선택지 하나. rationale은 정답·오답 모두에 필수다.
     * 오답 선택지에도 "왜 틀렸는지"를 남겨야 확인 문제가 학습 도구가 된다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(String label, boolean correct, String rationale) {
    }
}
