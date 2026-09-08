package com.finmate.assessment;

import com.finmate.product.ProductCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 진단 결과에 따라 안내되는 상품 카테고리.
 * 과제용 내부 탐색 필터를 통과한 카테고리만 저장된다.
 */
@Entity
@Table(name = "category_recommendations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"assessment_id", "category_id"}),
                @UniqueConstraint(columnNames = {"assessment_id", "rank_no"})
        })
public class CategoryRecommendation {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    /** 노출 순위. rank 는 SQL 예약어이므로 컬럼명을 rank_no 로 둔다. */
    @Column(name = "rank_no", nullable = false)
    private int rankNo;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CategoryRecommendation() {
    }

    public CategoryRecommendation(String id, Assessment assessment, ProductCategory category,
                                  int rankNo, String reason, Instant createdAt) {
        this.id = id;
        this.assessment = assessment;
        this.category = category;
        this.rankNo = rankNo;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public int getRankNo() {
        return rankNo;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
