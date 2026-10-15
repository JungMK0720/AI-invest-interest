package com.finmate.glossary;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GlossaryBookmarkId implements Serializable {
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "term_slug", nullable = false, length = 120)
    private String termSlug;

    protected GlossaryBookmarkId() { }

    public GlossaryBookmarkId(String userId, String termSlug) {
        this.userId = userId;
        this.termSlug = termSlug;
    }

    public String getUserId() { return userId; }
    public String getTermSlug() { return termSlug; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GlossaryBookmarkId id)) return false;
        return Objects.equals(userId, id.userId) && Objects.equals(termSlug, id.termSlug);
    }

    @Override
    public int hashCode() { return Objects.hash(userId, termSlug); }
}
