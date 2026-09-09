package com.finmate.learning;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/learning")
public class LearningController {
    private final LearningCatalog catalog;
    public LearningController(LearningCatalog catalog) { this.catalog = catalog; }
    @GetMapping("/concepts") public List<LearningCatalog.Concept> list() { return catalog.list(); }
    @GetMapping("/concepts/{slug}") public LearningCatalog.Concept get(@PathVariable String slug) {
        try { return catalog.find(slug); }
        catch (IllegalArgumentException e) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage()); }
    }
}
