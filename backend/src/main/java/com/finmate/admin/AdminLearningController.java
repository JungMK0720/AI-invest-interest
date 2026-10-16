package com.finmate.admin;

import com.finmate.common.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 전용 학습 개념·확인 문제 운영 API. 모든 메서드가 서비스에서 ADMIN 역할을 확인한다. */
@RestController
@RequestMapping("/admin/learning/concepts")
public class AdminLearningController {

    private final AdminLearningService service;

    public AdminLearningController(AdminLearningService service) {
        this.service = service;
    }

    @GetMapping
    public AdminLearningDtos.Concepts list(@CurrentUserId String userId) {
        return service.list(userId);
    }

    @GetMapping("/{slug}")
    public AdminLearningDtos.Concept get(@CurrentUserId String userId, @PathVariable String slug) {
        return service.get(userId, slug);
    }

    @PutMapping("/{slug}")
    public AdminLearningDtos.Concept edit(@CurrentUserId String userId, @PathVariable String slug,
                                          @Valid @RequestBody AdminLearningDtos.EditRequest request) {
        return service.edit(userId, slug, request);
    }

    @PostMapping("/{slug}/publish")
    public AdminLearningDtos.Concept publish(@CurrentUserId String userId, @PathVariable String slug) {
        return service.publish(userId, slug);
    }

    @PostMapping("/{slug}/unpublish")
    public AdminLearningDtos.Concept unpublish(@CurrentUserId String userId, @PathVariable String slug) {
        return service.unpublish(userId, slug);
    }

    @DeleteMapping("/{slug}/draft")
    public ResponseEntity<Void> discard(@CurrentUserId String userId, @PathVariable String slug) {
        service.discard(userId, slug);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{slug}/revisions")
    public AdminLearningDtos.Revisions revisions(@CurrentUserId String userId, @PathVariable String slug) {
        return service.revisions(userId, slug);
    }
}
