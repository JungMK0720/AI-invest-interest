package com.finmate.glossary;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "glossary_bookmarks")
public class GlossaryBookmark {
    @EmbeddedId
    private GlossaryBookmarkId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GlossaryBookmark() { }

    public GlossaryBookmark(String userId, String termSlug) {
        this.id = new GlossaryBookmarkId(userId, termSlug);
        this.createdAt = Instant.now();
    }

    public GlossaryBookmarkId getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}
