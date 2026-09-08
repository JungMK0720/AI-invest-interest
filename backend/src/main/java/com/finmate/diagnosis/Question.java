package com.finmate.diagnosis;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * 투자자 성향 진단 문항. weight 는 표준투자권유준칙의 문항별 배점 비중이며 전체 합이 100이 되도록 관리한다.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    private String id;

    @Column(nullable = false)
    private String content;

    @Column(name = "order_no", nullable = false, unique = true)
    private int orderNo;

    @Column(nullable = false)
    private int weight;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("orderNo ASC")
    private List<QuestionChoice> choices = new ArrayList<>();

    protected Question() {
    }

    public String getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isActive() {
        return active;
    }

    public List<QuestionChoice> getChoices() {
        return choices;
    }
}
