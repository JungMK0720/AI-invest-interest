package com.finmate.assessment;

import com.finmate.common.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping
    public ResponseEntity<AssessmentDtos.AssessmentResultResponse> submit(
            @CurrentUserId String userId,
            @Valid @RequestBody AssessmentDtos.SubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.submit(userId, request));
    }

    @GetMapping("/availability")
    public AssessmentDtos.AvailabilityResponse getAvailability(@CurrentUserId String userId) {
        return assessmentService.findAvailability(userId);
    }

    @GetMapping
    public AssessmentDtos.HistoryResponse getHistory(@CurrentUserId String userId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page) {
        return assessmentService.findHistory(userId, page);
    }

    @GetMapping("/{assessmentId}")
    public AssessmentDtos.AssessmentResultResponse getResult(
            @CurrentUserId String userId,
            @PathVariable String assessmentId) {
        return assessmentService.findResult(userId, assessmentId);
    }

    @GetMapping("/{assessmentId}/recommendations")
    public List<AssessmentDtos.CategoryRecommendationResponse> getRecommendations(
            @CurrentUserId String userId,
            @PathVariable String assessmentId) {
        return assessmentService.findRecommendations(userId, assessmentId);
    }
}
