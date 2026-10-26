package com.finmate.glossary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "glossary_categories")
public class GlossaryCategory {
    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    protected GlossaryCategory() { }

    public GlossaryCategory(String id, int sortOrder, String name, String description) {
        this.id = id;
        this.sortOrder = sortOrder;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public int getSortOrder() { return sortOrder; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
