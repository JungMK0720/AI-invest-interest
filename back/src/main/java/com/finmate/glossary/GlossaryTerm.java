package com.finmate.glossary;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

/** DB에 저장된 용어. 시드는 없는 slug만 넣으므로 관리자 수정본은 재시작 후에도 유지된다. */
@Entity
@Table(name = "glossary_terms", indexes = {
        @Index(name = "idx_glossary_terms_category_status", columnList = "category_id,status")
})
public class GlossaryTerm {
    @Id
    @Column(length = 120)
    private String slug;

    @Column(name = "category_id", nullable = false, length = 40)
    private String categoryId;

    @Column(nullable = false, length = 160)
    private String term;

    @Column(name = "full_name", length = 240)
    private String fullName;

    @Column(nullable = false, length = 240)
    private String english;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> aliases = List.of();

    @Column(nullable = false, length = 20)
    private String level;

    @Column(nullable = false, columnDefinition = "text")
    private String definition;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

    @Column(columnDefinition = "text")
    private String formula;

    @Column(columnDefinition = "text")
    private String example;

    @Column(columnDefinition = "text")
    private String caution;

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private List<String> related = List.of();

    @Column(name = "concept_slug", nullable = false, length = 120)
    private String conceptSlug;

    @Column(name = "source_name", nullable = false, length = 200)
    private String sourceName;

    @Column(name = "source_url", nullable = false, length = 600)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GlossaryStatus status = GlossaryStatus.DRAFT;

    @Column(name = "content_version", nullable = false, length = 80)
    private String contentVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GlossaryTerm() { }

    public GlossaryTerm(String slug) { this.slug = slug; }

    @PrePersist
    @PreUpdate
    void stamp() { updatedAt = Instant.now(); }

    public String getSlug() { return slug; }
    public String getCategoryId() { return categoryId; }
    public String getTerm() { return term; }
    public String getFullName() { return fullName; }
    public String getEnglish() { return english; }
    public List<String> getAliases() { return aliases == null ? List.of() : List.copyOf(aliases); }
    public String getLevel() { return level; }
    public String getDefinition() { return definition; }
    public String getExplanation() { return explanation; }
    public String getFormula() { return formula; }
    public String getExample() { return example; }
    public String getCaution() { return caution; }
    public List<String> getRelated() { return related == null ? List.of() : List.copyOf(related); }
    public String getConceptSlug() { return conceptSlug; }
    public String getSourceName() { return sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public GlossaryStatus getStatus() { return status; }
    public String getContentVersion() { return contentVersion; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public void setTerm(String term) { this.term = term; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEnglish(String english) { this.english = english; }
    public void setAliases(List<String> aliases) { this.aliases = aliases == null ? List.of() : List.copyOf(aliases); }
    public void setLevel(String level) { this.level = level; }
    public void setDefinition(String definition) { this.definition = definition; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setFormula(String formula) { this.formula = formula; }
    public void setExample(String example) { this.example = example; }
    public void setCaution(String caution) { this.caution = caution; }
    public void setRelated(List<String> related) { this.related = related == null ? List.of() : List.copyOf(related); }
    public void setConceptSlug(String conceptSlug) { this.conceptSlug = conceptSlug; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public void setStatus(GlossaryStatus status) { this.status = status; }
    public void setContentVersion(String contentVersion) { this.contentVersion = contentVersion; }
}
