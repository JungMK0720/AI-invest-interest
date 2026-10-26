package com.finmate.glossary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class GlossaryIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private GlossaryTermRepository terms;
    @Autowired private GlossaryCategoryRepository categories;
    @Autowired private GlossarySeeder seeder;
    @Autowired private GlossaryService glossary;
    @Autowired private GlossarySeedData source;

    /** 테스트 클래스끼리 DB를 공유하므로 전체 행 수 대신 시드 slug가 모두 들어왔는지 센다. */
    private long seededTermCount() {
        return terms.findAllById(source.terms().stream().map(GlossaryTerm::getSlug).toList()).size();
    }

    @Test
    void importsAllAuthoredTermsAndExposesSearchAndDetail() throws Exception {
        assertThat(categories.count()).isEqualTo(14);
        assertThat(seededTermCount()).isEqualTo(247);

        mvc.perform(get("/api/glossary/categories").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(14))
                .andExpect(jsonPath("$.categories[0].id").value("market"));

        mvc.perform(get("/api/glossary/terms").contextPath("/api")
                        .param("q", "케이알엑스").param("category", "market"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.terms[0].slug").value("krx"));

        mvc.perform(get("/api/glossary/terms/krx").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value("market"))
                .andExpect(jsonPath("$.aliases.length()").isNumber())
                .andExpect(jsonPath("$.sourceUrl").value("https://www.krx.co.kr"));

        mvc.perform(get("/api/glossary/terms").contextPath("/api").param("level", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/glossary/terms/does-not-exist").contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void startupImportDoesNotOverwriteEditedOrUnpublishedTerms() throws Exception {
        String oldVersion = glossary.contentVersion();
        GlossaryTerm term = terms.findById("krx").orElseThrow();
        term.setTerm("관리자가 수정한 이름");
        term.setStatus(GlossaryStatus.DRAFT);
        terms.saveAndFlush(term);

        long before = terms.count();
        seeder.run(null);

        assertThat(terms.count()).isEqualTo(before);
        assertThat(seededTermCount()).isEqualTo(247);
        assertThat(terms.findById("krx").orElseThrow().getTerm()).isEqualTo("관리자가 수정한 이름");
        assertThat(glossary.contentVersion()).isNotEqualTo(oldVersion);
        mvc.perform(get("/api/glossary/terms/krx").contextPath("/api"))
                .andExpect(status().isNotFound());
    }

    @Test
    void quizHidesAnswersAndGradesOnlyItsOwnChoices() throws Exception {
        String response = mvc.perform(get("/api/glossary/quiz").contextPath("/api")
                        .param("category", "market").param("count", "1").param("seed", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(1))
                .andExpect(jsonPath("$.questions[0].choices.length()").value(4))
                .andExpect(jsonPath("$.questions[0].correctChoiceId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode question = mapper.readTree(response).path("questions").get(0);
        String version = mapper.readTree(response).path("contentVersion").asText();
        String questionId = question.path("id").asText();
        String firstChoice = question.path("choices").get(0).path("id").asText();

        String firstResult = mvc.perform(post("/api/glossary/quiz/grade").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeRequest(version, questionId, firstChoice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.answered").value(1))
                .andReturn().getResponse().getContentAsString();
        String correctChoice = mapper.readTree(firstResult).path("items").get(0)
                .path("correctChoiceId").asText();
        assertThat(question.path("choices").findValuesAsText("id")).contains(correctChoice);

        mvc.perform(post("/api/glossary/quiz/grade").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeRequest(version, questionId, correctChoice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correctCount").value(1))
                .andExpect(jsonPath("$.scorePercent").value(100));

        mvc.perform(post("/api/glossary/quiz/grade").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeRequest(version, questionId, "foreign-choice")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/glossary/quiz/grade").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradeRequest("stale-version", questionId, correctChoice)))
                .andExpect(status().isConflict());
    }

    @Test
    void bookmarksRequireLoginAndStayPrivateToTheirOwner() throws Exception {
        mvc.perform(get("/api/glossary/bookmarks").contextPath("/api"))
                .andExpect(status().isUnauthorized());
        String firstToken = loginNewUser();
        String secondToken = loginNewUser();

        mvc.perform(put("/api/glossary/bookmarks/krx").contextPath("/api")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/glossary/bookmarks/krx").contextPath("/api")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/glossary/bookmarks").contextPath("/api")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms.length()").value(1))
                .andExpect(jsonPath("$.terms[0].slug").value("krx"));
        mvc.perform(get("/api/glossary/bookmarks").contextPath("/api")
                        .header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms.length()").value(0));
        mvc.perform(delete("/api/glossary/bookmarks/krx").contextPath("/api")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isNoContent());
    }

    private String loginNewUser() throws Exception {
        String email = "glossary-" + UUID.randomUUID() + "@example.test";
        String signup = mapper.writeValueAsString(Map.of("email", email, "password", "TestPass123!", "name", "Glossary Test"));
        mvc.perform(post("/api/auth/signup").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content(signup))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/auth/login").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", "TestPass123!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(login).path("accessToken").asText();
    }

    private String gradeRequest(String version, String questionId, String choiceId) throws Exception {
        return mapper.writeValueAsString(Map.of("contentVersion", version,
                "answers", List.of(Map.of("questionId", questionId, "choiceId", choiceId))));
    }
}
