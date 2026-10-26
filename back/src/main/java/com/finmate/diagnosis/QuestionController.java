package com.finmate.diagnosis;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/questions")
public class QuestionController {

    private final QuestionRepository questionRepository;

    public QuestionController(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<QuestionDtos.QuestionResponse> listActiveQuestions() {
        return questionRepository.findByActiveTrueOrderByOrderNoAsc().stream()
                .map(QuestionDtos::from)
                .toList();
    }
}
