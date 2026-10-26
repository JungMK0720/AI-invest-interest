package com.finmate.glossary;

import com.finmate.auth.AuthTokenStore;
import com.finmate.common.CurrentUserId;
import com.finmate.learning.GlossaryQuizProgressService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/glossary")
public class GlossaryController {
    private final GlossaryService glossary;
    private final GlossaryQuizService quiz;
    private final AuthTokenStore tokens;
    private final GlossaryQuizProgressService progress;

    public GlossaryController(GlossaryService glossary, GlossaryQuizService quiz,
                              AuthTokenStore tokens, GlossaryQuizProgressService progress) {
        this.glossary = glossary;
        this.quiz = quiz;
        this.tokens = tokens;
        this.progress = progress;
    }

    @GetMapping("/categories")
    public GlossaryDtos.CategoriesResponse categories() { return glossary.categories(); }

    @GetMapping("/terms")
    public GlossaryDtos.TermsResponse terms(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String level) {
        return glossary.terms(q, category, level);
    }

    @GetMapping("/terms/{slug}")
    public GlossaryDtos.TermDetail term(@PathVariable String slug) { return glossary.detail(slug); }

    @GetMapping("/quiz")
    public GlossaryDtos.QuizResponse quiz(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) Integer count,
                                           @RequestParam(required = false) Long seed) {
        return quiz.questions(category, count, seed);
    }

    @PostMapping("/quiz/grade")
    public GlossaryDtos.QuizGradeResponse grade(@RequestBody(required = false) GlossaryDtos.QuizGradeRequest request,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        var userId = tokens.resolveOptionalBearerUserId(authorization);
        GlossaryDtos.QuizGradeResponse result = quiz.grade(request);
        userId.ifPresent(id -> progress.record(id, request, result));
        return result;
    }

    @GetMapping("/bookmarks")
    public GlossaryDtos.BookmarksResponse bookmarks(@CurrentUserId String userId) {
        return glossary.bookmarks(userId);
    }

    @PutMapping("/bookmarks/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addBookmark(@CurrentUserId String userId, @PathVariable String slug) {
        glossary.addBookmark(userId, slug);
    }

    @DeleteMapping("/bookmarks/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBookmark(@CurrentUserId String userId, @PathVariable String slug) {
        glossary.removeBookmark(userId, slug);
    }
}
