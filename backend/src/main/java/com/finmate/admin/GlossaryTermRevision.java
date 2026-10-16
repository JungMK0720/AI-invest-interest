package com.finmate.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 용어의 공개 상태가 바뀐 순간의 불변 기록. 누가, 언제, 어떤 내용을 공개하거나 내렸는지 감사할 수 있게 한다.
 * 기록은 추가만 하고 수정·삭제하지 않는다.
 */
@Entity
@Table(name = "glossary_term_revisions",
        indexes = @Index(name = "idx_glossary_revision_slug_time", columnList = "slug,created_at"))
public class GlossaryTermRevision {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 120)
    private String slug;

    /** CREATED, PUBLISHED, UNPUBLISHED 중 하나. */
    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "content_version", nullable = false, length = 80)
    private String contentVersion;

    @Column(name = "editor_user_id", nullable = false)
    private String editorUserId;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GlossaryTermRevision() { }

    public GlossaryTermRevision(String id, String slug, String action, String contentVersion,
                                String editorUserId, String snapshotJson, Instant createdAt) {
        this.id = id;
        this.slug = slug;
        this.action = action;
        this.contentVersion = contentVersion;
        this.editorUserId = editorUserId;
        this.snapshotJson = snapshotJson;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getSlug() { return slug; }
    public String getAction() { return action; }
    public String getContentVersion() { return contentVersion; }
    public String getEditorUserId() { return editorUserId; }
    public String getSnapshotJson() { return snapshotJson; }
    public Instant getCreatedAt() { return createdAt; }
}
