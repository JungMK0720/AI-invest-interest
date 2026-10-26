package com.finmate.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.auth.AuthTokenStore;
import com.finmate.learning.LearningDtos;
import com.finmate.learning.LearningSeeder;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 개념·확인 문제 초안 → 게시 → 학습 화면 즉시 반영 → 옛 버전 제출 409 → 공개 내리기 → 감사 기록까지
 * 운영 흐름 전체를 확인한다. 같은 DB를 다른 테스트와 함께 쓰므로 바꾼 개념은 끝나면 원래 내용으로 되돌린다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminLearningLifecycleIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired AuthTokenStore tokens;
    @Autowired LearningSeeder seeder;
    @Autowired LearningConceptDraftRepository drafts;

    @Test
    void 해설을_초안으로_고치고_게시하면_재시작_없이_학습_화면에_반영되고_옛_버전_제출은_409다() throws Exception {
        String admin = adminToken();
        String slug = "volatility";
        AdminLearningDtos.EditRequest original = content(admin, slug);
        String rationale = "운영자가 고친 해설 " + UUID.randomUUID();
        try {
            String before = quizVersion(slug);
            JsonNode saved = body(mvc.perform(put("/admin/learning/concepts/" + slug)
                            .header("Authorization", bearer(admin)).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(withFirstRationale(original, rationale))))
                    .andExpect(status().isOk()).andReturn());
            assertThat(saved.get("hasDraft").asBoolean()).isTrue();
            assertThat(saved.at("/content/quiz/0/options/0/rationale").asText()).isEqualTo(rationale);

            // 게시 전에는 학습 화면이 공개본 그대로다.
            assertThat(quizVersion(slug)).isEqualTo(before);
            assertThat(firstRationale(slug, before)).isEqualTo(original.quiz().get(0).options().get(0).rationale());

            mvc.perform(post("/admin/learning/concepts/" + slug + "/publish").header("Authorization", bearer(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"))
                    .andExpect(jsonPath("$.hasDraft").value(false));

            String after = quizVersion(slug);
            assertThat(after).isNotEqualTo(before);
            mvc.perform(get("/learning/tree")).andExpect(jsonPath("$.contentVersion").value(after));
            mvc.perform(post("/learning/concepts/" + slug + "/quiz/attempts").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(attempt(before, original))))
                    .andExpect(status().isConflict());
            assertThat(firstRationale(slug, after)).isEqualTo(rationale);

            mvc.perform(post("/admin/learning/concepts/" + slug + "/publish").header("Authorization", bearer(admin)))
                    .andExpect(status().isConflict());

            // 재시작에 해당하는 시드 재적재도 관리자 수정본을 덮어쓰지 않는다.
            seeder.run(null);
            assertThat(firstRationale(slug, quizVersion(slug))).isEqualTo(rationale);

            List<String> actions = actions(admin, slug);
            assertThat(actions).first().isEqualTo("PUBLISHED");
        } finally {
            restore(admin, slug, original);
        }
        assertThat(firstRationale(slug, quizVersion(slug))).isEqualTo(original.quiz().get(0).options().get(0).rationale());
    }

    @Test
    void 정답_수_해설_출처_레벨_문항_id_규칙을_어긴_초안은_거절한다() throws Exception {
        String admin = adminToken();
        String slug = "volatility";
        AdminLearningDtos.EditRequest original = content(admin, slug);

        expectBadRequest(admin, slug, editQuiz(original, item -> new AdminLearningDtos.QuizItemInput(item.id(),
                item.prompt(), item.difficulty(), item.options().stream()
                .map(option -> new AdminLearningDtos.OptionInput(option.label(), true, option.rationale())).toList(),
                item.takeaway())));
        expectBadRequest(admin, slug, withFirstRationale(original, " "));
        expectBadRequest(admin, slug, editQuiz(original, item -> new AdminLearningDtos.QuizItemInput(item.id(),
                item.prompt(), item.difficulty(), item.options().subList(0, 2), item.takeaway())));
        expectBadRequest(admin, slug, editQuiz(original, item -> new AdminLearningDtos.QuizItemInput("risk-return-q1",
                item.prompt(), item.difficulty(), item.options(), item.takeaway())));
        expectBadRequest(admin, slug, copy(original, original.parent(), original.level(), ""));
        expectBadRequest(admin, slug, copy(original, original.parent(), original.level(), "ftp://example.test/a"));
        expectBadRequest(admin, slug, copy(original, original.parent(), 2, original.sourceUrl()));
        expectBadRequest(admin, slug, copy(original, "no-such-concept", original.level(), original.sourceUrl()));
        expectBadRequest(admin, slug, copy(original, null, original.level(), original.sourceUrl()));

        mvc.perform(get("/admin/learning/concepts/" + slug).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasDraft").value(false));
        // 공개 중인 하위 개념이 있으면 상위 개념의 공개를 내릴 수 없다.
        mvc.perform(post("/admin/learning/concepts/foundations/unpublish").header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
        mvc.perform(get("/admin/learning/concepts/no-such-concept").header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());

        String userToken = tokens.issue(saveUser(false).getId()).value();
        mvc.perform(get("/admin/learning/concepts").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/learning/concepts/" + slug + "/publish").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/learning/concepts")).andExpect(status().isUnauthorized());
    }

    @Test
    void 공개를_내리면_학습_트리와_용어_링크에서_빠지고_다시_게시하면_돌아온다() throws Exception {
        String admin = adminToken();
        String slug = "duration";
        try {
            mvc.perform(post("/admin/learning/concepts/" + slug + "/unpublish").header("Authorization", bearer(admin)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
            mvc.perform(get("/learning/concepts/" + slug)).andExpect(status().isNotFound());
            assertThat(treeSlugs()).doesNotContain(slug);
            mvc.perform(get("/glossary/terms/duration")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.conceptSlug").doesNotExist());
            JsonNode overview = body(mvc.perform(get("/admin/overview").header("Authorization", bearer(admin)))
                    .andExpect(status().isOk()).andReturn());
            assertThat(overview.get("hiddenConcepts").asLong()).isGreaterThanOrEqualTo(1);
            mvc.perform(post("/admin/learning/concepts/" + slug + "/unpublish").header("Authorization", bearer(admin)))
                    .andExpect(status().isConflict());
        } finally {
            mvc.perform(post("/admin/learning/concepts/" + slug + "/publish").header("Authorization", bearer(admin)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        }
        mvc.perform(get("/learning/concepts/" + slug)).andExpect(status().isOk());
        assertThat(treeSlugs()).contains(slug);
        mvc.perform(get("/glossary/terms/duration")).andExpect(jsonPath("$.conceptSlug").value(slug));
        assertThat(actions(admin, slug)).startsWith("PUBLISHED", "UNPUBLISHED");

        JsonNode list = body(mvc.perform(get("/admin/learning/concepts").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn());
        List<String> order = new ArrayList<>();
        list.get("items").forEach(item -> order.add(item.get("slug").asText()));
        assertThat(order).hasSizeGreaterThanOrEqualTo(59);
        assertThat(order.indexOf("instruments")).isLessThan(order.indexOf("bonds"));
        assertThat(order.indexOf("bonds")).isLessThan(order.indexOf("duration"));
    }

    @Test
    void 그사이_공개본이_바뀐_초안은_게시하지_않고_버릴_수_있다() throws Exception {
        String admin = adminToken();
        String slug = "drawdown";
        AdminLearningDtos.EditRequest original = content(admin, slug);
        drafts.save(new LearningConceptDraft(slug, "stale-version", "nobody", mapper.writeValueAsString(original),
                Instant.now()));

        mvc.perform(post("/admin/learning/concepts/" + slug + "/publish").header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/admin/learning/concepts/" + slug + "/draft").header("Authorization", bearer(admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/admin/learning/concepts/" + slug).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasDraft").value(false));
    }

    private void restore(String admin, String slug, AdminLearningDtos.EditRequest original) throws Exception {
        mvc.perform(delete("/admin/learning/concepts/" + slug + "/draft").header("Authorization", bearer(admin)));
        mvc.perform(put("/admin/learning/concepts/" + slug).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(original)))
                .andExpect(status().isOk());
        mvc.perform(post("/admin/learning/concepts/" + slug + "/publish").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    private void expectBadRequest(String admin, String slug, AdminLearningDtos.EditRequest request) throws Exception {
        mvc.perform(put("/admin/learning/concepts/" + slug).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private AdminLearningDtos.EditRequest content(String admin, String slug) throws Exception {
        JsonNode concept = body(mvc.perform(get("/admin/learning/concepts/" + slug).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn());
        return mapper.treeToValue(concept.get("content"), AdminLearningDtos.EditRequest.class);
    }

    private String quizVersion(String slug) throws Exception {
        return body(mvc.perform(get("/learning/concepts/" + slug + "/quiz")).andExpect(status().isOk()).andReturn())
                .get("contentVersion").asText();
    }

    /** 첫 문항에 첫 선택지를 골라 제출하고, 그 선택지의 해설을 돌려받는다. */
    private String firstRationale(String slug, String version) throws Exception {
        JsonNode quiz = body(mvc.perform(get("/learning/concepts/" + slug + "/quiz")).andReturn());
        String questionId = quiz.at("/questions/0/id").asText();
        LearningDtos.QuizAttemptRequest request = new LearningDtos.QuizAttemptRequest(version,
                List.of(new LearningDtos.QuizAttemptAnswer(questionId, questionId + "-choice-0")));
        JsonNode result = body(mvc.perform(post("/learning/concepts/" + slug + "/quiz/attempts")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn());
        return result.at("/items/0/choices/0/rationale").asText();
    }

    private static LearningDtos.QuizAttemptRequest attempt(String version, AdminLearningDtos.EditRequest content) {
        String questionId = content.quiz().get(0).id();
        return new LearningDtos.QuizAttemptRequest(version,
                List.of(new LearningDtos.QuizAttemptAnswer(questionId, questionId + "-choice-0")));
    }

    private List<String> treeSlugs() throws Exception {
        List<String> slugs = new ArrayList<>();
        collect(body(mvc.perform(get("/learning/tree")).andExpect(status().isOk()).andReturn()).get("roots"), slugs);
        return slugs;
    }

    private static void collect(JsonNode nodes, List<String> into) {
        nodes.forEach(node -> {
            into.add(node.get("slug").asText());
            collect(node.get("children"), into);
        });
    }

    private List<String> actions(String admin, String slug) throws Exception {
        JsonNode history = body(mvc.perform(get("/admin/learning/concepts/" + slug + "/revisions")
                .header("Authorization", bearer(admin))).andExpect(status().isOk()).andReturn());
        List<String> actions = new ArrayList<>();
        history.get("items").forEach(item -> actions.add(item.get("action").asText()));
        return actions;
    }

    private static AdminLearningDtos.EditRequest withFirstRationale(AdminLearningDtos.EditRequest content, String rationale) {
        return editQuiz(content, item -> {
            List<AdminLearningDtos.OptionInput> options = new ArrayList<>(item.options());
            AdminLearningDtos.OptionInput first = options.get(0);
            options.set(0, new AdminLearningDtos.OptionInput(first.label(), first.correct(), rationale));
            return new AdminLearningDtos.QuizItemInput(item.id(), item.prompt(), item.difficulty(), options, item.takeaway());
        });
    }

    /** 첫 문항만 바꾼다. */
    private static AdminLearningDtos.EditRequest editQuiz(AdminLearningDtos.EditRequest content,
                                                          UnaryOperator<AdminLearningDtos.QuizItemInput> change) {
        List<AdminLearningDtos.QuizItemInput> quiz = new ArrayList<>(content.quiz());
        quiz.set(0, change.apply(quiz.get(0)));
        return new AdminLearningDtos.EditRequest(content.parent(), content.level(), content.order(), content.title(),
                content.summary(), content.oneLiner(), content.explanation(), content.keyPoints(), content.checklist(),
                content.misconception(), content.diagramKey(), content.sourceName(), content.sourceUrl(),
                content.simulationId(), quiz);
    }

    private static AdminLearningDtos.EditRequest copy(AdminLearningDtos.EditRequest content, String parent, int level,
                                                      String sourceUrl) {
        return new AdminLearningDtos.EditRequest(parent, level, content.order(), content.title(), content.summary(),
                content.oneLiner(), content.explanation(), content.keyPoints(), content.checklist(),
                content.misconception(), content.diagramKey(), content.sourceName(), sourceUrl,
                content.simulationId(), content.quiz());
    }

    private String adminToken() {
        return tokens.issue(saveUser(true).getId()).value();
    }

    private User saveUser(boolean admin) {
        User user = new User(UUID.randomUUID().toString(), "ops-" + UUID.randomUUID() + "@example.test",
                "not-used", admin ? "운영자" : "사용자", Instant.now());
        if (admin) user.promoteToAdmin();
        return users.save(user);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
