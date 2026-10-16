package com.finmate.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AdminGlossaryDtos {

    private AdminGlossaryDtos() { }

    /** PUT은 용어 전체를 교체하는 초안 입력이다. 공개 원본은 publish 전까지 유지된다. */
    public record EditRequest(
            @NotBlank @Size(max = 40) String categoryId,
            @NotBlank @Size(max = 160) String term,
            @Size(max = 240) String fullName,
            @NotBlank @Size(max = 240) String english,
            @NotNull List<String> aliases,
            @NotBlank @Size(max = 20) String level,
            @NotBlank @Size(max = 500) String definition,
            @NotBlank @Size(max = 4000) String explanation,
            @Size(max = 1000) String formula,
            @Size(max = 2000) String example,
            @Size(max = 2000) String caution,
            @NotNull List<String> related,
            @NotBlank @Size(max = 120) String conceptSlug,
            @NotBlank @Size(max = 200) String sourceName,
            @NotBlank @Size(max = 600) String sourceUrl) { }

    /** 새 용어. 비공개(DRAFT)로 만들어지고 publish를 거쳐야 공개 API와 검색에 나타난다. */
    public record CreateRequest(
            @NotBlank @Size(max = 120) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*",
                    message = "slug는 영문 소문자·숫자와 하이픈만 사용할 수 있습니다.") String slug,
            @NotNull @Valid EditRequest content) { }

    public record Term(String slug, String categoryId, String term, String fullName, String english,
                       List<String> aliases, String level, String definition, String explanation,
                       String formula, String example, String caution, List<String> related,
                       String conceptSlug, String sourceName, String sourceUrl, String status,
                       String contentVersion, Instant updatedAt, boolean hasDraft) { }

    public record Terms(List<Term> items) { }

    public record Revision(String id, String action, String contentVersion, String editorEmail, Instant createdAt) { }

    public record Revisions(String slug, List<Revision> items) { }

    /** 운영 현황. 개인 정보 없이 개수와 버전만 보여 준다. */
    public record Overview(long users, long admins, long completedAssessments, long quizAttempts,
                           long publishedTerms, long hiddenTerms, long pendingDrafts,
                           String glossaryContentVersion, String learningContentVersion,
                           String styleModelVersion, String termModelVersion, Instant generatedAt) { }
}
