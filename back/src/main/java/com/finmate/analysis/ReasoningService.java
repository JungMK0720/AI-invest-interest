package com.finmate.analysis;

import com.finmate.assessment.ReasoningSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 근거 문장 생성의 단일 진입점.
 *
 * AI 호출이 실패해도 진단 자체를 실패시키지 않는다. 성향 등급과 추천은 규칙 엔진이 이미 확정했으므로
 * 설명만 템플릿으로 낮춰서 응답한다 (ADR-008). 응답의 reasoningSource 로 어느 경로였는지 구분한다.
 */
@Service
public class ReasoningService {

    private static final Logger log = LoggerFactory.getLogger(ReasoningService.class);

    private final ClaudeReasoningGenerator claudeGenerator;
    private final TemplateReasoningGenerator templateGenerator;
    private final ReasoningValidator validator;

    public ReasoningService(ClaudeReasoningGenerator claudeGenerator,
                            TemplateReasoningGenerator templateGenerator, ReasoningValidator validator) {
        this.validator = validator;
        this.claudeGenerator = claudeGenerator;
        this.templateGenerator = templateGenerator;
    }

    public Reasoning generate(ReasoningRequest request) {
        if (claudeGenerator.isEnabled()) {
            try {
                String text = claudeGenerator.generate(request);
                if (validator.accepts(text, request)) return new Reasoning(text, ReasoningSource.LLM);
                log.warn("AI 설명 출력 검사 실패로 기본 설명을 사용합니다.");
            } catch (RuntimeException e) {
                log.warn("AI 근거 생성에 실패해 템플릿으로 대체합니다. 오류 유형: {}", e.getClass().getSimpleName());
            }
        }
        return new Reasoning(templateGenerator.generate(request), ReasoningSource.TEMPLATE);
    }

    public record Reasoning(String text, ReasoningSource source) {
    }
}
