package com.finmate.learning;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 스키마가 준비된 뒤 빠진 시드 개념만 넣는다. 관리자가 게시한 수정본은 덮어쓰지 않는다.
 *
 * <p>개념이 이미 있으면 그 확인 문제도 건드리지 않는다. 관리자가 지운 문항이 재시작마다 되살아나면 안 되기 때문이다.
 * 넣은 뒤 공개 트리 전체를 다시 검증해, 새 시드 개념이 비공개 처리된 상위 개념을 가리키는 식의 어긋남이 있으면
 * 기동을 멈춘다. 웹 서버는 이 러너보다 먼저 요청을 받을 수 있으므로, 끝나면 이벤트로 카탈로그 캐시를 버리게 한다.
 */
@Component
public class LearningSeeder implements ApplicationRunner {

    private final LearningSeedData source;
    private final LearningConceptRepository concepts;
    private final LearningQuizItemRepository quizItems;
    private final LearningContentStore store;
    private final TransactionTemplate transactions;
    private final ApplicationEventPublisher events;

    public LearningSeeder(LearningSeedData source, LearningConceptRepository concepts,
                          LearningQuizItemRepository quizItems, LearningContentStore store,
                          TransactionTemplate transactions, ApplicationEventPublisher events) {
        this.source = source;
        this.concepts = concepts;
        this.quizItems = quizItems;
        this.store = store;
        this.transactions = transactions;
        this.events = events;
    }

    @Override
    public void run(ApplicationArguments args) {
        transactions.executeWithoutResult(ignored -> {
            List<LearningConcept> missingConcepts = new ArrayList<>();
            List<LearningQuizItem> missingItems = new ArrayList<>();
            for (LearningContent.Node node : source.tree().nodes()) {
                if (concepts.existsById(node.slug())) continue;
                LearningConcept concept = new LearningConcept(node.slug());
                concept.apply(node);
                concept.setStatus(LearningConceptStatus.PUBLISHED);
                concept.setContentVersion(source.version());
                missingConcepts.add(concept);
                for (int i = 0; i < node.quiz().size(); i++) {
                    LearningContent.QuizItem item = node.quiz().get(i);
                    if (quizItems.existsById(item.id())) {
                        throw new IllegalStateException("시드 확인 문제 id가 다른 개념에서 이미 쓰이고 있습니다: " + item.id());
                    }
                    LearningQuizItem row = new LearningQuizItem(item.id(), node.slug());
                    row.apply(i, item);
                    missingItems.add(row);
                }
            }
            concepts.saveAll(missingConcepts);
            quizItems.saveAll(missingItems);
            concepts.flush();
            store.loadPublished();
        });
        events.publishEvent(new LearningContentChangedEvent(null, "SEEDED"));
    }
}
