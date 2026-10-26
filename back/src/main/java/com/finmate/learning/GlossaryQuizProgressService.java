package com.finmate.learning;

import com.finmate.glossary.GlossaryDtos;
import com.finmate.glossary.GlossaryTerm;
import com.finmate.glossary.GlossaryTermRepository;
import java.util.Objects;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공개 용어 퀴즈의 채점 결과를 로그인 사용자에게만 저장한다. */
@Service
public class GlossaryQuizProgressService {

    private final QuizAttemptRecordRepository attempts;
    private final QuizAttemptAnswerRecordRepository answers;
    private final QuizReviewStateRepository reviews;
    private final GlossaryTermRepository terms;

    public GlossaryQuizProgressService(QuizAttemptRecordRepository attempts,
            QuizAttemptAnswerRecordRepository answers, QuizReviewStateRepository reviews,
            GlossaryTermRepository terms) {
        this.attempts = attempts;
        this.answers = answers;
        this.reviews = reviews;
        this.terms = terms;
    }

    @Transactional
    public void record(String userId, GlossaryDtos.QuizGradeRequest request,
                       GlossaryDtos.QuizGradeResponse result) {
        if (!Objects.equals(request.contentVersion(), result.contentVersion())) {
            throw new IllegalStateException("채점 결과의 콘텐츠 버전이 요청과 다릅니다.");
        }
        Instant now = Instant.now();
        String attemptId = UUID.randomUUID().toString();
        attempts.save(new QuizAttemptRecord(attemptId, userId, "glossary", result.contentVersion(),
                result.total(), result.answered(), result.correctCount(), result.scorePercent(), now));
        for (GlossaryDtos.QuizGradeItem item : result.items()) {
            GlossaryTerm term = terms.findById(item.termSlug())
                    .orElseThrow(() -> new IllegalStateException("채점된 용어를 찾지 못했습니다: " + item.termSlug()));
            QuizAttemptAnswerRecord answer = new QuizAttemptAnswerRecord(
                    UUID.randomUUID().toString(), attemptId, item.questionId(), item.prompt(),
                    item.selectedChoiceId(), item.correctChoiceId(), item.correct(),
                    item.selectedChoiceText() == null ? "응답하지 않았습니다." : item.selectedChoiceText(),
                    item.explanation(), "용어 정의를 다시 읽고 비슷한 용어와 차이를 설명해 보세요.");
            answers.save(answer);
            // 공개 questionId에는 카탈로그 버전이 들어간다. 복습 키는 용어 slug로 고정해
            // 콘텐츠 개정 뒤 재시도가 이전 오답을 해소할 수 있게 한다.
            String reviewQuestionId = "glossary:" + item.termSlug();
            QuizReviewState state = reviews.findByUserIdAndQuestionId(userId, reviewQuestionId)
                    .orElseGet(() -> new QuizReviewState(UUID.randomUUID().toString(), userId,
                            item.termSlug(), term.getTerm(), reviewQuestionId));
            state.apply(answer, result.contentVersion(), now);
            reviews.save(state);
        }
    }
}
