package com.finmate.learning;

import com.finmate.common.CurrentUserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class QuizProgressController {

    private final QuizProgressService service;

    public QuizProgressController(QuizProgressService service) {
        this.service = service;
    }

    @GetMapping("/quiz-attempts")
    public QuizProgressDtos.AttemptHistory history(@CurrentUserId String userId,
                                                   @RequestParam(defaultValue = "0") int page) {
        return service.history(userId, page);
    }

    @GetMapping("/quiz-attempts/{attemptId}")
    public QuizProgressDtos.AttemptDetail detail(@CurrentUserId String userId, @PathVariable String attemptId) {
        return service.detail(userId, attemptId);
    }

    @GetMapping("/wrong-answers")
    public QuizProgressDtos.WrongAnswerList wrongAnswers(@CurrentUserId String userId) {
        return service.wrongAnswers(userId);
    }

    @GetMapping("/review-schedule")
    public QuizProgressDtos.ReviewSchedule reviewSchedule(@CurrentUserId String userId) {
        return service.reviewSchedule(userId);
    }
}
