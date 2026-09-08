package com.finmate.product;

import com.finmate.analysis.RiskGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_categories")
public class ProductCategory {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    /** 금융투자상품 위험등급 1~6. 1등급이 가장 위험이 크다. */
    @Column(name = "risk_grade", nullable = false)
    private int riskGrade;

    @Column(nullable = false)
    private String description;

    protected ProductCategory() {
    }

    public ProductCategory(String id, String name, int riskGrade, String description) {
        this.id = id;
        this.name = name;
        this.riskGrade = riskGrade;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRiskGrade() {
        return riskGrade;
    }

    public RiskGrade riskGrade() {
        return RiskGrade.of(riskGrade);
    }

    public String getDescription() {
        return description;
    }
}
