package com.finmate.glossary;

import java.util.List;

/** Public DTOs keep admin-only draft status and internal rows out of the user API. */
public final class GlossaryDtos {
    public static final String EDUCATION_NOTICE =
            "교육용 용어 설명입니다. 특정 상품의 매매를 권유하지 않으며, 제도·세율·한도는 출처의 최신 자료를 확인하세요.";

    private GlossaryDtos() { }

    public record Category(String id, int order, String name, String description, long termCount) { }
    public record CategoriesResponse(String contentVersion, List<Category> categories) { }

    public record TermSummary(String slug, String term, String english, String categoryId,
                              String level, String definition) { }
    public record TermsResponse(String contentVersion, int total, List<TermSummary> terms) { }

    public record TermDetail(String slug, String term, String fullName, String english, List<String> aliases,
                             String categoryId, String level, String definition, String explanation,
                             String formula, String example, String caution, List<String> related,
                             String conceptSlug, String sourceName, String sourceUrl,
                             String contentVersion, String notice) { }

    public record QuizChoice(String id, String text) { }
    public record QuizQuestion(String id, String prompt, List<QuizChoice> choices) { }
    public record QuizResponse(String contentVersion, List<QuizQuestion> questions, String notice) { }
    public record QuizAnswer(String questionId, String choiceId) { }
    public record QuizGradeRequest(String contentVersion, List<QuizAnswer> answers) { }
    public record QuizGradeItem(String questionId, String termSlug, String prompt, String selectedChoiceId,
                                String selectedChoiceText, String correctChoiceId, boolean correct,
                                String explanation) { }
    public record QuizGradeResponse(int total, int answered, int correctCount, int scorePercent,
                                    List<QuizGradeItem> items, String contentVersion, String notice) { }

    public record BookmarksResponse(List<TermSummary> terms) { }
}
