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

@RestController
@RequestMapping("/admin/glossary/terms")
public class AdminGlossaryController {

    private final AdminGlossaryService service;

    public AdminGlossaryController(AdminGlossaryService service) {
        this.service = service;
    }

    @GetMapping
    public AdminGlossaryDtos.Terms list(@CurrentUserId String userId) {
        return service.list(userId);
    }

    @GetMapping("/{slug}")
    public AdminGlossaryDtos.Term get(@CurrentUserId String userId, @PathVariable String slug) {
        return service.get(userId, slug);
    }

    @PutMapping("/{slug}")
    public AdminGlossaryDtos.Term edit(@CurrentUserId String userId, @PathVariable String slug,
                                       @Valid @RequestBody AdminGlossaryDtos.EditRequest request) {
        return service.edit(userId, slug, request);
    }

    @PostMapping("/{slug}/publish")
    public AdminGlossaryDtos.Term publish(@CurrentUserId String userId, @PathVariable String slug) {
        return service.publish(userId, slug);
    }

    @DeleteMapping("/{slug}/draft")
    public ResponseEntity<Void> discard(@CurrentUserId String userId, @PathVariable String slug) {
        service.discard(userId, slug);
        return ResponseEntity.noContent().build();
    }
}
