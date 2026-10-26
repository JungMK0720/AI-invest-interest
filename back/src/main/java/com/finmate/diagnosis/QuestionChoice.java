package com.finmate.diagnosis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * 진단 문항의 선택지.
 * score 는 0~100 원점수이며 문항 weight 와 가중 평균되어 투자자 성향 점수를 만든다.
 * analysisKey 는 화면 문구가 바뀌어도 AI 프롬프트 입력이 흔들리지 않도록 유지하는 의미 키다.
 */
@Entity
@Table(name = "question_choices",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "order_no"}))
public class QuestionChoice {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private String label;

    @Column(name = "analysis_key", nullable = false)
    private String analysisKey;

    @Column(nullable = false)
    private int score;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    protected QuestionChoice() {
    }

    public String getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public String getLabel() {
        return label;
    }

    public String getAnalysisKey() {
        return analysisKey;
    }

    public int getScore() {
        return score;
    }

    public int getOrderNo() {
        return orderNo;
    }
}
