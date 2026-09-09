package com.finmate.learning;

import java.util.List;

/**
 * 학습 화면 응답 DTO.
 *
 * <p>확인 문제 응답에는 정답을 담지 않는다. 채점은 {@link QuizGrader}가 서버에서 수행하고,
 * 해설은 채점 응답에서만 내려간다. 화면 소스에서 정답이 보이면 확인 문제가 학습 도구로 동작하지 않는다.
 */
public final class LearningDtos {

    /** 모의투자·확인 문제 화면에 항상 노출하는 고지. 상품 권유가 아님을 반복해서 밝힌다. */
    public static final String EDUCATION_NOTICE =
            "교육용 학습 콘텐츠입니다. 특정 상품의 가입·매매를 권유하지 않으며, 실제 투자 판단은 "
                    + "상품설명서와 공식 위험 정보를 확인한 뒤 투자자 본인이 합니다.";

    private LearningDtos() {
    }

    /** 좌측 트리 한 노드. children을 그대로 품어 한 번의 호출로 전체 트리를 그린다. */
    public record TreeNode(
            String slug,
            String title,
            String summary,
            int level,
            boolean hasSimulation,
            int quizCount,
            int descendantCount,
            List<TreeNode> children) {
    }

    public record TreeResponse(String contentVersion, int conceptCount, List<TreeNode> roots, String notice) {
    }

    /** breadcrumb 한 칸. */
    public record Crumb(String slug, String title, int level) {
    }

    /** 좌측에서 개념을 눌렀을 때 우측에 펼쳐지는 본문. */
    public record ConceptDetail(
            String slug,
            String title,
            String summary,
            String oneLiner,
            int level,
            List<String> explanation,
            List<String> keyPoints,
            List<String> checklist,
            String misconception,
            String diagramKey,
            String sourceName,
            String sourceUrl,
            String simulationId,
            int quizCount,
            List<Crumb> breadcrumb,
            List<TreeNode> children,
            List<String> relatedSlugs,
            String contentVersion,
            String notice) {
    }

    /** 정답이 제거된 확인 문제. options는 표시 순서 그대로의 라벨만 담는다. */
    public record QuizQuestion(String id, String prompt, String difficulty, List<String> options) {
    }

    public record QuizResponse(String slug, String title, String contentVersion, List<QuizQuestion> questions, String notice) {
    }

    /** 채점 요청. selectedIndex는 QuizQuestion.options의 인덱스이며 -1은 미응답이다. */
    public record QuizAnswer(String questionId, int selectedIndex) {
    }

    public record QuizGradeRequest(List<QuizAnswer> answers) {
    }

    /** 문항별 채점 결과. 모든 선택지의 해설을 함께 돌려줘 오답 이유까지 학습하게 한다. */
    public record QuizGradedItem(
            String questionId,
            String prompt,
            int selectedIndex,
            int correctIndex,
            boolean correct,
            List<OptionFeedback> options,
            String takeaway) {
    }

    public record OptionFeedback(int index, String label, boolean correct, String rationale) {
    }

    public record QuizGradeResponse(
            String slug,
            int total,
            int correctCount,
            int scorePercent,
            String contentVersion,
            List<QuizGradedItem> items,
            String summary) {
    }
}
