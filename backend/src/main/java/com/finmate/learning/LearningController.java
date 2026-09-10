package com.finmate.learning;

import com.finmate.common.ApiException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 학습·확인 문제·모의투자 API.
 *
 * <p>로그인 없이도 접근할 수 있다. docs/09의 "재진단 제한과 무관하게 학습은 열려 있어야 한다"는
 * 정책을 API 수준에서도 지킨다. 진도 저장은 사용자 식별이 필요하므로 다음 단계로 미뤄 둔다.
 */
@RestController
@RequestMapping("/learning")
public class LearningController {

    private final LearningCatalog catalog;
    private final LearningService service;
    private final QuizGrader grader;
    private final SimulationCatalog simulations;
    private final SimulationEngine engine;
    private final LearningAiService learningAiService;

    public LearningController(LearningCatalog catalog, LearningService service, QuizGrader grader,
                              SimulationCatalog simulations, SimulationEngine engine,
                              LearningAiService learningAiService) {
        this.catalog = catalog;
        this.service = service;
        this.grader = grader;
        this.simulations = simulations;
        this.engine = engine;
        this.learningAiService = learningAiService;
    }

    /** 좌측 개념 트리 전체. 한 번 받아 두고 클라이언트에서 펼치고 접는다. */
    @GetMapping("/tree")
    public LearningDtos.TreeResponse tree() {
        return service.tree();
    }

    /** v1 호환 평면 목록. 기존 클라이언트가 계속 동작하도록 유지한다. */
    @GetMapping("/concepts")
    public List<LearningCatalog.Concept> concepts() {
        return catalog.list();
    }

    @GetMapping("/concepts/{slug}")
    public LearningDtos.ConceptDetail concept(@PathVariable String slug) {
        return service.detail(slug);
    }

    /** 승인된 개념 콘텐츠를 근거로 AI가 질문에 답한다. 진단·추천·계산은 이 API가 담당하지 않는다. */
    @PostMapping("/concepts/{slug}/explain")
    public LearningAiDtos.ExplainResponse explain(@PathVariable String slug,
                                                   @RequestBody(required = false) LearningAiDtos.ExplainRequest request) {
        return learningAiService.explain(slug, request);
    }

    @GetMapping("/concepts/{slug}/quiz")
    public LearningDtos.QuizResponse quiz(@PathVariable String slug) {
        service.require(slug);
        return grader.questions(slug);
    }

    /** 채점은 서버에서만 한다. 요청 본문이 비어 있어도 전 문항 오답으로 채점해 해설을 돌려준다. */
    @PostMapping("/concepts/{slug}/quiz/grade")
    public LearningDtos.QuizGradeResponse grade(@PathVariable String slug,
                                                @RequestBody(required = false) LearningDtos.QuizGradeRequest request) {
        service.require(slug);
        return grader.grade(slug, request);
    }

    /** 운영 제출 계약: 콘텐츠 버전과 choice ID를 검증하고, 문항별 의미를 남긴다. */
    @PostMapping("/concepts/{slug}/quiz/attempts")
    public LearningDtos.QuizAttemptResponse attempt(@PathVariable String slug,
                                                     @RequestBody(required = false) LearningDtos.QuizAttemptRequest request) {
        service.require(slug);
        return grader.attempt(slug, request);
    }

    @GetMapping("/concepts/{slug}/simulations")
    public List<SimulationDtos.ScenarioSpec> conceptSimulations(@PathVariable String slug) {
        return service.scenariosFor(slug);
    }

    @GetMapping("/simulations")
    public SimulationDtos.ScenarioListResponse simulations() {
        return new SimulationDtos.ScenarioListResponse(simulations.list(), SimulationDtos.SIMULATION_NOTICE);
    }

    @GetMapping("/simulations/{scenarioId}")
    public SimulationDtos.ScenarioSpec simulation(@PathVariable String scenarioId) {
        return requireScenario(scenarioId);
    }

    /**
     * 모의투자 실행. 입력값은 명세 범위로 보정하며 잘못된 값 때문에 400을 내지 않는다.
     * seed를 바꾸면 같은 가정에서의 다른 시장 경로를 볼 수 있다.
     */
    @PostMapping("/simulations/{scenarioId}/run")
    public SimulationDtos.RunResponse run(@PathVariable String scenarioId,
                                          @RequestBody(required = false) SimulationDtos.RunRequest request) {
        requireScenario(scenarioId);
        return engine.run(scenarioId, request);
    }

    private SimulationDtos.ScenarioSpec requireScenario(String scenarioId) {
        if (!simulations.contains(scenarioId)) {
            throw ApiException.notFound("모의투자 시나리오를 찾을 수 없습니다: " + scenarioId);
        }
        return simulations.find(scenarioId);
    }
}
