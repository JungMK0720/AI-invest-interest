package com.finmate.learning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "quiz_attempt_answers",
        indexes = @Index(name = "idx_quiz_answer_attempt", columnList = "attempt_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"attempt_id", "question_id"}))
public class QuizAttemptAnswerRecord {

    @Id
    private String id;

    @Column(name = "attempt_id", nullable = false)
    private String attemptId;

    @Column(name = "question_id", nullable = false)
    private String questionId;

    @Column(name = "selected_choice_id")
    private String selectedChoiceId;

    @Column(name = "correct_choice_id", nullable = false)
    private String correctChoiceId;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(name = "selected_rationale", columnDefinition = "text")
    private String selectedRationale;

    @Column(name = "correct_rationale", nullable = false, columnDefinition = "text")
    private String correctRationale;

    @Column(nullable = false, columnDefinition = "text")
    private String takeaway;

    protected QuizAttemptAnswerRecord() { }

    public QuizAttemptAnswerRecord(String id, String attemptId, LearningDtos.QuizAttemptItem item) {
        this(id, attemptId, item.questionId(), item.prompt(), item.selectedChoiceId(), item.correctChoiceId(),
                item.correct(), item.choices().stream()
                        .filter(choice -> choice.id().equals(item.selectedChoiceId()))
                        .map(LearningDtos.QuizAttemptOption::rationale).findFirst().orElse(null),
                item.choices().stream()
                        .filter(choice -> choice.id().equals(item.correctChoiceId()))
                        .map(LearningDtos.QuizAttemptOption::rationale).findFirst().orElse(""),
                item.takeaway());
    }

    public QuizAttemptAnswerRecord(String id, String attemptId, String questionId, String prompt,
                                   String selectedChoiceId, String correctChoiceId, boolean correct,
                                   String selectedRationale, String correctRationale, String takeaway) {
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.selectedChoiceId = selectedChoiceId;
        this.correctChoiceId = correctChoiceId;
        this.correct = correct;
        this.prompt = prompt;
        this.selectedRationale = selectedRationale;
        this.correctRationale = correctRationale;
        this.takeaway = takeaway;
    }

    public String getQuestionId() { return questionId; }
    public String getSelectedChoiceId() { return selectedChoiceId; }
    public String getCorrectChoiceId() { return correctChoiceId; }
    public boolean isCorrect() { return correct; }
    public String getPrompt() { return prompt; }
    public String getSelectedRationale() { return selectedRationale; }
    public String getCorrectRationale() { return correctRationale; }
    public String getTakeaway() { return takeaway; }
}
