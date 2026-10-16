package com.finmate.learning;

import com.finmate.common.CurrentUserId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class LearningPathController {

    private final LearningPathService service;

    public LearningPathController(LearningPathService service) {
        this.service = service;
    }

    /** 최근 진단·자유 서술 신호·오답 기록으로 만든 다음 학습 순서. 본인 기록만 사용한다. */
    @GetMapping("/learning-path")
    public LearningPathDtos.Response learningPath(@CurrentUserId String userId) {
        return service.build(userId);
    }
}
