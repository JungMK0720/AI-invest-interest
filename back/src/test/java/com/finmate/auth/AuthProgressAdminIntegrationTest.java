package com.finmate.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.admin.AdminGlossaryDtos;
import com.finmate.glossary.GlossaryTerm;
import com.finmate.glossary.GlossaryTermRepository;
import com.finmate.learning.LearningCatalog;
import com.finmate.learning.LearningContent;
import com.finmate.learning.LearningDtos;
import com.finmate.user.User;
import com.finmate.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
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
class AuthProgressAdminIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired AuthTokenStore tokens;
    @Autowired LearningCatalog catalog;
    @Autowired GlossaryTermRepository terms;

    @Test
    void sessionsPersistHashedTokensExpireAndLogoutRevokesThem() throws Exception {
        JsonNode account = signup();
        String token = login(account.get("email").asText());
        assertThat(sessions.findById(token)).isEmpty();
        mvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"));

        mvc.perform(post("/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());

        String expiredToken = "expired-" + UUID.randomUUID();
        sessions.save(new AuthSession(hash(expiredToken), account.get("id").asText(),
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(3600)));
        mvc.perform(get("/auth/me").header("Authorization", bearer(expiredToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void quizAttemptsStayPrivateAndCorrectRetryClearsWrongAnswer() throws Exception {
        JsonNode firstUser = signup();
        JsonNode secondUser = signup();
        String firstToken = login(firstUser.get("email").asText());
        String secondToken = login(secondUser.get("email").asText());
        LearningContent.Node node = catalog.node("risk-return");
        LearningContent.QuizItem firstQuestion = node.quiz().get(0);
        int correctIndex = correctIndex(firstQuestion);
        int wrongIndex = (correctIndex + 1) % firstQuestion.options().size();
        LearningDtos.QuizAttemptRequest wrong = new LearningDtos.QuizAttemptRequest(catalog.version(),
                List.of(new LearningDtos.QuizAttemptAnswer(firstQuestion.id(),
                        firstQuestion.id() + "-choice-" + wrongIndex)));

        // 비로그인 시도는 채점되지만 사용자 기록을 만들지 않는다.
        mvc.perform(post("/learning/concepts/risk-return/quiz/attempts")
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(wrong)))
                .andExpect(status().isOk());
        mvc.perform(get("/me/quiz-attempts").header("Authorization", bearer(firstToken)))
                .andExpect(jsonPath("$.items.length()").value(0));

        mvc.perform(post("/learning/concepts/risk-return/quiz/attempts")
                .header("Authorization", bearer(firstToken))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(wrong)))
                .andExpect(status().isOk());
        JsonNode history = body(mvc.perform(get("/me/quiz-attempts")
                .header("Authorization", bearer(firstToken))).andExpect(status().isOk()).andReturn());
        assertThat(history.path("items").size()).isEqualTo(1);
        String attemptId = history.path("items").get(0).path("id").asText();
        mvc.perform(get("/me/quiz-attempts/{id}", attemptId).header("Authorization", bearer(secondToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/me/wrong-answers").header("Authorization", bearer(firstToken)))
                .andExpect(jsonPath("$.items[0].dueAt").exists());
        mvc.perform(get("/me/quiz-attempts").header("Authorization", bearer(secondToken)))
                .andExpect(jsonPath("$.items.length()").value(0));

        List<LearningDtos.QuizAttemptAnswer> allCorrect = node.quiz().stream()
                .map(question -> new LearningDtos.QuizAttemptAnswer(question.id(),
                        question.id() + "-choice-" + correctIndex(question))).toList();
        mvc.perform(post("/learning/concepts/risk-return/quiz/attempts")
                .header("Authorization", bearer(firstToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new LearningDtos.QuizAttemptRequest(catalog.version(), allCorrect))))
                .andExpect(status().isOk());
        mvc.perform(get("/me/wrong-answers").header("Authorization", bearer(firstToken)))
                .andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/me/review-schedule").header("Authorization", bearer(firstToken)))
                .andExpect(jsonPath("$.items[0].slug").value("risk-return"))
                .andExpect(jsonPath("$.items[0].incorrectCount").value(0))
                .andExpect(jsonPath("$.items[0].questionCount").value(node.quiz().size()));
    }

    @Test
    void glossaryQuizWrongAnswerIsSavedAndClearedByCorrectRetry() throws Exception {
        JsonNode account = signup();
        String token = login(account.get("email").asText());
        JsonNode quiz = body(mvc.perform(get("/glossary/quiz").param("count", "1").param("seed", "42"))
                .andExpect(status().isOk()).andReturn());
        JsonNode question = quiz.path("questions").get(0);
        String questionId = question.path("id").asText();
        String firstChoiceId = question.path("choices").get(0).path("id").asText();
        String secondChoiceId = question.path("choices").get(1).path("id").asText();
        JsonNode firstGrade = gradeGlossary(token, quiz.path("contentVersion").asText(), questionId, firstChoiceId);
        String correctChoiceId = firstGrade.path("items").get(0).path("correctChoiceId").asText();
        String wrongChoiceId = firstChoiceId.equals(correctChoiceId) ? secondChoiceId : firstChoiceId;
        if (!wrongChoiceId.equals(firstChoiceId)) {
            gradeGlossary(token, quiz.path("contentVersion").asText(), questionId, wrongChoiceId);
        }
        mvc.perform(get("/me/wrong-answers").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].questionId").exists());
        mvc.perform(get("/me/review-schedule").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items[0].incorrectCount").value(1));

        gradeGlossary(token, quiz.path("contentVersion").asText(), questionId, correctChoiceId);
        mvc.perform(get("/me/wrong-answers").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.items.length()").value(0));
        JsonNode history = body(mvc.perform(get("/me/quiz-attempts")
                .header("Authorization", bearer(token))).andExpect(status().isOk()).andReturn());
        assertThat(history.path("items").size()).isGreaterThanOrEqualTo(2);
        assertThat(history.path("items").get(0).path("slug").asText()).isEqualTo("glossary");
    }

    @Test
    @Transactional
    void onlyAdminCanDraftAndPublishWithoutChangingPublicTermBeforePublication() throws Exception {
        User ordinary = new User(UUID.randomUUID().toString(), "user-" + UUID.randomUUID() + "@example.test",
                "unused", "사용자", Instant.now());
        User admin = new User(UUID.randomUUID().toString(), "admin-" + UUID.randomUUID() + "@example.test",
                "unused", "운영자", Instant.now());
        admin.promoteToAdmin();
        users.save(ordinary);
        users.save(admin);
        String userToken = tokens.issue(ordinary.getId()).value();
        String adminToken = tokens.issue(admin.getId()).value();
        mvc.perform(get("/admin/glossary/terms").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());

        GlossaryTerm original = terms.findById("krx").orElseThrow();
        String revisedDefinition = original.getDefinition() + " 편집 테스트.";
        AdminGlossaryDtos.EditRequest edit = new AdminGlossaryDtos.EditRequest(
                original.getCategoryId(), original.getTerm(), original.getFullName(), original.getEnglish(),
                original.getAliases(), original.getLevel(), revisedDefinition, original.getExplanation(),
                original.getFormula(), original.getExample(), original.getCaution(), original.getRelated(),
                original.getConceptSlug(), original.getSourceName(), original.getSourceUrl());
        mvc.perform(put("/admin/glossary/terms/krx").header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(edit)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasDraft").value(true));
        JsonNode before = body(mvc.perform(get("/glossary/terms/krx"))
                .andExpect(status().isOk()).andReturn());
        assertThat(before.path("definition").asText()).isEqualTo(original.getDefinition());

        mvc.perform(post("/admin/glossary/terms/krx/publish").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasDraft").value(false));
        mvc.perform(get("/glossary/terms/krx"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.definition").value(revisedDefinition));
    }

    private JsonNode signup() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.test";
        return body(mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AuthDtos.SignupRequest(email, "test-password-123", "사용자"))))
                .andExpect(status().isCreated()).andReturn());
    }

    private String login(String email) throws Exception {
        JsonNode response = body(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AuthDtos.LoginRequest(email, "test-password-123"))))
                .andExpect(status().isOk()).andReturn());
        assertThat(Instant.parse(response.path("expiresAt").asText())).isAfter(Instant.now());
        return response.path("accessToken").asText();
    }

    private JsonNode gradeGlossary(String token, String version, String questionId, String choiceId) throws Exception {
        return body(mvc.perform(post("/glossary/quiz/grade").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new com.finmate.glossary.GlossaryDtos.QuizGradeRequest(
                        version, List.of(new com.finmate.glossary.GlossaryDtos.QuizAnswer(questionId, choiceId))))))
                .andExpect(status().isOk()).andReturn());
    }

    private JsonNode body(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private static int correctIndex(LearningContent.QuizItem question) {
        for (int i = 0; i < question.options().size(); i++) {
            if (question.options().get(i).correct()) return i;
        }
        throw new IllegalStateException(question.id());
    }

    private static String bearer(String token) { return "Bearer " + token; }

    private static String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}
