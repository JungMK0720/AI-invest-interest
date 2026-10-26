package com.finmate.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.auth.AuthTokenStore;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** 새 용어 생성 → 게시 → 검색 반영 → 공개 내리기 → 감사 기록까지 운영 흐름 전체를 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class AdminGlossaryLifecycleIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired AuthTokenStore tokens;

    @Test
    void 새_용어는_비공개로_만들어지고_게시하면_목록과_모델_검색에_바로_나타난다() throws Exception {
        String admin = adminToken();
        String slug = "test-term-" + UUID.randomUUID().toString().substring(0, 8);
        AdminGlossaryDtos.CreateRequest create = new AdminGlossaryDtos.CreateRequest(slug, content("엑스지표", List.of("per")));

        JsonNode created = body(mvc.perform(post("/admin/glossary/terms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(create)))
                .andExpect(status().isCreated()).andReturn());
        assertThat(created.get("status").asText()).isEqualTo("DRAFT");
        mvc.perform(get("/glossary/terms/" + slug)).andExpect(status().isNotFound());
        mvc.perform(post("/admin/glossary/terms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(create)))
                .andExpect(status().isConflict());

        mvc.perform(post("/admin/glossary/terms/" + slug + "/publish").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/glossary/terms/" + slug)).andExpect(status().isOk())
                .andExpect(jsonPath("$.term").value("엑스지표"));
        assertThat(searchSlugs("엑스지표가 뭐예요?")).first().isEqualTo(slug);

        mvc.perform(post("/admin/glossary/terms/" + slug + "/unpublish").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
        mvc.perform(get("/glossary/terms/" + slug)).andExpect(status().isNotFound());
        assertThat(searchSlugs("엑스지표가 뭐예요?")).doesNotContain(slug);

        JsonNode history = body(mvc.perform(get("/admin/glossary/terms/" + slug + "/revisions")
                .header("Authorization", bearer(admin))).andExpect(status().isOk()).andReturn());
        List<String> actions = new ArrayList<>();
        history.get("items").forEach(item -> actions.add(item.get("action").asText()));
        assertThat(actions).containsExactly("UNPUBLISHED", "PUBLISHED", "CREATED");
    }

    @Test
    void 존재하지_않는_관련_용어나_학습_개념은_거절하고_일반_사용자는_접근할_수_없다() throws Exception {
        String admin = adminToken();
        mvc.perform(post("/admin/glossary/terms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                                new AdminGlossaryDtos.CreateRequest("bad-related", content("나쁜관련", List.of("no-such-term"))))))
                .andExpect(status().isBadRequest());
        AdminGlossaryDtos.EditRequest badConcept = new AdminGlossaryDtos.EditRequest("market", "나쁜개념", null, "Bad",
                List.of(), "BASIC", "정의", "설명", null, null, null, List.of(), "no-such-concept",
                "한국거래소", "https://www.krx.co.kr");
        mvc.perform(post("/admin/glossary/terms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                                new AdminGlossaryDtos.CreateRequest("bad-concept", badConcept))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/admin/glossary/terms").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(
                                new AdminGlossaryDtos.CreateRequest("Bad Slug!", content("슬러그", List.of())))))
                .andExpect(status().isBadRequest());

        JsonNode overview = body(mvc.perform(get("/admin/overview").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn());
        assertThat(overview.get("publishedTerms").asLong()).isGreaterThanOrEqualTo(247);
        assertThat(overview.get("admins").asLong()).isGreaterThanOrEqualTo(1);

        String userToken = tokens.issue(saveUser(false).getId()).value();
        mvc.perform(get("/admin/overview").header("Authorization", bearer(userToken))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/glossary/terms/krx/unpublish").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    private List<String> searchSlugs(String question) throws Exception {
        JsonNode result = body(mvc.perform(post("/ml/term-search").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new com.finmate.ml.MlDtos.TermSearchRequest(question, 5))))
                .andExpect(status().isOk()).andReturn());
        List<String> slugs = new ArrayList<>();
        result.get("results").forEach(hit -> slugs.add(hit.get("slug").asText()));
        return slugs;
    }

    private static AdminGlossaryDtos.EditRequest content(String name, List<String> related) {
        return new AdminGlossaryDtos.EditRequest("valuation", name, null, "X Indicator", List.of(), "BASIC",
                name + "는 테스트용으로 만든 교육 지표다.", name + "의 설명 문단.", null, null, null, related,
                "equity-valuation", "한국거래소", "https://www.krx.co.kr");
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
