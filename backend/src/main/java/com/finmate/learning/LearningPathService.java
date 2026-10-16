package com.finmate.learning;

import com.finmate.analysis.InvestorProfileType;
import com.finmate.assessment.Assessment;
import com.finmate.assessment.AssessmentRepository;
import com.finmate.assessment.AssessmentStatus;
import com.finmate.assessment.DecisionSnapshot;
import com.finmate.assessment.InvestmentProfile;
import com.finmate.assessment.InvestmentProfileRepository;
import com.finmate.assessment.SnapshotCodec;
import com.finmate.glossary.GlossaryService;
import com.finmate.glossary.GlossaryTerm;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 진단 결과·자유 서술 신호·오답 기록을 합쳐 "다음에 볼 것"을 순서대로 만든다.
 *
 * <p>복습 시점이 된 오답은 따로 모으고, 학습 개념·용어는 (1) 서술과 설문의 차이 → (2) 사용자가 직접 쓴 관심사 태그
 * → (3) 나머지 진단 확인 플래그 → (4) 현재 성향에서 먼저 볼 개념 순으로 고른다. 확인 문제를 모두 맞힌 개념은 뺀다.
 * 규칙은 결정적이어서 같은 기록이면 같은 경로가 나오고, 모든 단계에 어떤 신호에서 나왔는지 사유를 붙인다.
 */
@Service
public class LearningPathService {

    private static final int MAX_CONCEPTS = 6;
    private static final int MAX_TERMS = 6;
    /** 한 신호가 목록을 독차지하지 않도록 신호마다 개념·용어를 2개까지만 가져온다. */
    private static final int PER_SIGNAL = 2;

    private final AssessmentRepository assessments;
    private final InvestmentProfileRepository profiles;
    private final SnapshotCodec snapshots;
    private final QuizReviewStateRepository reviews;
    private final QuizAttemptRecordRepository attempts;
    private final LearningCatalog concepts;
    private final LearningSignalCatalog signals;
    private final GlossaryService glossary;

    public LearningPathService(AssessmentRepository assessments, InvestmentProfileRepository profiles,
                               SnapshotCodec snapshots,
                               QuizReviewStateRepository reviews, QuizAttemptRecordRepository attempts,
                               LearningCatalog concepts, LearningSignalCatalog signals, GlossaryService glossary) {
        this.assessments = assessments;
        this.profiles = profiles;
        this.snapshots = snapshots;
        this.reviews = reviews;
        this.attempts = attempts;
        this.concepts = concepts;
        this.signals = signals;
        this.glossary = glossary;
    }

    @Transactional(readOnly = true)
    public LearningPathDtos.Response build(String userId) {
        Instant now = Instant.now();
        List<Signal> ordered = new ArrayList<>();
        LearningPathDtos.Basis basis = null;

        Assessment latest = assessments.findByUserIdAndStatusOrderByCreatedAtDescIdDesc(
                userId, AssessmentStatus.COMPLETED, PageRequest.of(0, 1)).stream().findFirst().orElse(null);
        if (latest != null) {
            DecisionSnapshot snapshot = snapshots.decode(latest.getDecisionSnapshot());
            List<String> flags = snapshot == null ? List.of()
                    : snapshot.reviewFlags().stream().map(DecisionSnapshot.ReviewFlag::code).toList();
            DecisionSnapshot.NarrativeSignal narrative = snapshot == null ? null : snapshot.narrative();
            List<DecisionSnapshot.NarrativeTag> tags = narrative == null ? List.of() : narrative.tags().stream()
                    .sorted(Comparator.comparingDouble(DecisionSnapshot.NarrativeTag::score).reversed()).toList();
            InvestorProfileType profile = profiles.findByAssessmentId(latest.getId())
                    .map(InvestmentProfile::getProfileType).orElse(null);

            if (flags.contains("NARRATIVE_MISMATCH")) {
                signals.reviewFlag("NARRATIVE_MISMATCH").ifPresent(path ->
                        ordered.add(new Signal("NARRATIVE_MISMATCH", path, path.message())));
            }
            tags.forEach(tag -> signals.tag(tag.code()).ifPresent(path -> ordered.add(new Signal(tag.code(), path, path.message()))));
            flags.stream().filter(code -> !"NARRATIVE_MISMATCH".equals(code))
                    .forEach(code -> signals.reviewFlag(code).ifPresent(path -> ordered.add(new Signal(code, path, path.message()))));
            if (profile != null) {
                signals.profile(profile.name()).ifPresent(path -> ordered.add(new Signal("PROFILE", path,
                        "현재 진단 결과(" + profile.label() + ")에서 먼저 이해하면 좋은 개념입니다.")));
            }
            basis = new LearningPathDtos.Basis(latest.getId(), profile == null ? null : profile.name(),
                    profile == null ? null : profile.label(), latest.getCompletedAt(), flags,
                    tags.stream().map(tag -> new LearningPathDtos.NarrativeTag(tag.code(), tag.label())).toList(),
                    narrative != null && narrative.mismatch());
        } else {
            signals.tag("BEGINNER").ifPresent(path -> ordered.add(new Signal("STARTER", path,
                    "아직 진단 기록이 없어 처음 배우는 사람에게 필요한 공통 개념부터 안내합니다.")));
        }

        Set<String> mastered = masteredConcepts(userId);
        return new LearningPathDtos.Response(basis, dueReviews(userId, now), conceptSteps(ordered, mastered),
                termSteps(ordered), signals.version(), now, LearningPathDtos.NOTICE);
    }

    /** 가장 최근 시도에서 확인 문제를 모두 맞힌 개념. 같은 개념을 다시 추천하지 않는다. */
    private Set<String> masteredConcepts(String userId) {
        Map<String, Integer> latestScore = new LinkedHashMap<>();
        attempts.findByUserIdOrderByAttemptedAtDesc(userId, PageRequest.of(0, 200))
                .forEach(attempt -> latestScore.putIfAbsent(attempt.getSlug(), attempt.getScorePercent()));
        return latestScore.entrySet().stream().filter(entry -> entry.getValue() == 100)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    private List<LearningPathDtos.Step> dueReviews(String userId, Instant now) {
        // 개념 slug와 용어 slug가 같은 경우(예: etf)가 있어 종류까지 키에 넣는다.
        Map<String, List<QuizReviewState>> bySlug = new LinkedHashMap<>();
        for (QuizReviewState state : reviews.findByUserIdOrderByDueAtAsc(userId)) {
            if (!state.getDueAt().isAfter(now)) {
                String kind = state.getQuestionId().startsWith("glossary:") ? "TERM" : "CONCEPT";
                bySlug.computeIfAbsent(kind + ":" + state.getSlug(), key -> new ArrayList<>()).add(state);
            }
        }
        List<LearningPathDtos.Step> steps = new ArrayList<>();
        bySlug.forEach((key, states) -> {
            String slug = states.getFirst().getSlug();
            long incorrect = states.stream().filter(QuizReviewState::isIncorrect).count();
            String target = states.getFirst().getQuestionId().startsWith("glossary:") ? "TERM" : "CONCEPT";
            String reason = incorrect > 0
                    ? "오답 " + incorrect + "개를 다시 확인할 때가 되었습니다."
                    : "맞힌 문항도 간격을 두고 다시 풀면 오래 기억합니다.";
            steps.add(new LearningPathDtos.Step("REVIEW", target, slug, states.getFirst().getTitle(), "DUE_REVIEW", reason));
        });
        return List.copyOf(steps);
    }

    private List<LearningPathDtos.Step> conceptSteps(List<Signal> ordered, Set<String> mastered) {
        List<LearningPathDtos.Step> steps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Signal signal : ordered) {
            int taken = 0;
            for (String slug : signal.path().concepts()) {
                if (steps.size() >= MAX_CONCEPTS) return List.copyOf(steps);
                if (taken >= PER_SIGNAL) break;
                if (mastered.contains(slug) || !concepts.contains(slug) || !seen.add(slug)) continue;
                steps.add(new LearningPathDtos.Step("CONCEPT", "CONCEPT", slug, concepts.node(slug).title(),
                        signal.code(), signal.reason()));
                taken++;
            }
        }
        return List.copyOf(steps);
    }

    private List<LearningPathDtos.Step> termSteps(List<Signal> ordered) {
        Map<String, GlossaryTerm> published = glossary.listPublished().stream()
                .collect(Collectors.toMap(GlossaryTerm::getSlug, Function.identity()));
        List<LearningPathDtos.Step> steps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Signal signal : ordered) {
            int taken = 0;
            for (String slug : signal.path().terms()) {
                if (steps.size() >= MAX_TERMS) return List.copyOf(steps);
                if (taken >= PER_SIGNAL) break;
                GlossaryTerm term = published.get(slug);
                if (term == null || !seen.add(slug)) continue;
                steps.add(new LearningPathDtos.Step("TERM", "TERM", slug, term.getTerm(), signal.code(), term.getDefinition()));
                taken++;
            }
        }
        return List.copyOf(steps);
    }

    private record Signal(String code, LearningSignalCatalog.Path path, String reason) { }
}
