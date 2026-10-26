package com.finmate.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** 공개 중인 개념·확인 문제를 유지하면서 편집할 수 있는 별도 초안. 개념 하나당 하나다. */
@Entity
@Table(name = "learning_concept_drafts")
public class LearningConceptDraft {

    @Id
    @Column(length = 120)
    private String slug;

    @Column(name = "base_version", nullable = false, length = 80)
    private String baseVersion;

    @Column(name = "editor_user_id", nullable = false)
    private String editorUserId;

    @Column(name = "draft_json", nullable = false, columnDefinition = "text")
    private String draftJson;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LearningConceptDraft() { }

    public LearningConceptDraft(String slug, String baseVersion, String editorUserId,
                                String draftJson, Instant updatedAt) {
        this.slug = slug;
        this.baseVersion = baseVersion;
        this.editorUserId = editorUserId;
        this.draftJson = draftJson;
        this.updatedAt = updatedAt;
    }

    public String getSlug() { return slug; }
    public String getBaseVersion() { return baseVersion; }
    public String getDraftJson() { return draftJson; }
    public Instant getUpdatedAt() { return updatedAt; }
}
