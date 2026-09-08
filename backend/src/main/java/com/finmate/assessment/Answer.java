package com.finmate.assessment;

import com.finmate.diagnosis.Question;
import com.finmate.diagnosis.QuestionChoice;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "answers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessment_id", "question_id"}))
public class Answer {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "choice_id", nullable = false)
    private QuestionChoice choice;

    protected Answer() {
    }

    public Answer(String id, Assessment assessment, Question question, QuestionChoice choice) {
        this.id = id;
        this.assessment = assessment;
        this.question = question;
        this.choice = choice;
    }

    public String getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public Question getQuestion() {
        return question;
    }

    public QuestionChoice getChoice() {
        return choice;
    }
}
