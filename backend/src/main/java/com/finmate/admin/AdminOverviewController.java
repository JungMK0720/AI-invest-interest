package com.finmate.admin;

import com.finmate.assessment.AssessmentRepository;
import com.finmate.assessment.AssessmentStatus;
import com.finmate.common.CurrentUserId;
import com.finmate.glossary.GlossaryService;
import com.finmate.glossary.GlossaryStatus;
import com.finmate.glossary.GlossaryTermRepository;
import com.finmate.learning.LearningCatalog;
import com.finmate.learning.QuizAttemptRecordRepository;
import com.finmate.ml.MlModelCatalog;
import com.finmate.user.UserRepository;
import com.finmate.user.UserRole;
import java.time.Instant;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 운영 콘솔 첫 화면의 현황. 개인을 식별할 수 있는 정보는 내보내지 않는다. */
@RestController
public class AdminOverviewController {

    private final AdminAuthorizer authorizer;
    private final UserRepository users;
    private final AssessmentRepository assessments;
    private final QuizAttemptRecordRepository attempts;
    private final GlossaryTermRepository terms;
    private final GlossaryTermDraftRepository drafts;
    private final GlossaryService glossary;
    private final LearningCatalog concepts;
    private final MlModelCatalog models;

    public AdminOverviewController(AdminAuthorizer authorizer, UserRepository users, AssessmentRepository assessments,
                                   QuizAttemptRecordRepository attempts, GlossaryTermRepository terms,
                                   GlossaryTermDraftRepository drafts, GlossaryService glossary,
                                   LearningCatalog concepts, MlModelCatalog models) {
        this.authorizer = authorizer;
        this.users = users;
        this.assessments = assessments;
        this.attempts = attempts;
        this.terms = terms;
        this.drafts = drafts;
        this.glossary = glossary;
        this.concepts = concepts;
        this.models = models;
    }

    @GetMapping("/admin/overview")
    @Transactional(readOnly = true)
    public AdminGlossaryDtos.Overview overview(@CurrentUserId String userId) {
        authorizer.requireAdmin(userId);
        return new AdminGlossaryDtos.Overview(users.count(), users.countByRole(UserRole.ADMIN),
                assessments.countByStatus(AssessmentStatus.COMPLETED), attempts.count(),
                terms.countByStatus(GlossaryStatus.PUBLISHED), terms.countByStatus(GlossaryStatus.DRAFT),
                drafts.count(), glossary.contentVersion(), concepts.version(),
                models.style().version(), models.terms().version(), Instant.now());
    }
}
