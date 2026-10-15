package com.finmate.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
            @NotBlank String definition,
            @NotBlank String explanation,
            String formula,
            String example,
            String caution,
            @NotNull List<String> related,
            @NotBlank @Size(max = 120) String conceptSlug,
            @NotBlank @Size(max = 200) String sourceName,
            @NotBlank @Size(max = 600) String sourceUrl) { }

    public record Term(String slug, String categoryId, String term, String fullName, String english,
                       List<String> aliases, String level, String definition, String explanation,
                       String formula, String example, String caution, List<String> related,
                       String conceptSlug, String sourceName, String sourceUrl, String status,
                       String contentVersion, Instant updatedAt, boolean hasDraft) { }

    public record Terms(List<Term> items) { }
}
