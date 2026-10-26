package com.finmate.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finmate.auth.AuthDtos;
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

/** 자유 서술 신호가 진단 등급을 바꾸지 않으면서 근거·확인 플래그·학습 경로로 이어지는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class NarrativeLearningPathIntegrationTest {

    private static final String AGGRESSIVE_NARRATIVE =
            "반토막이 나도 상관없으니 레버리지로 몇 배 수익을 노리고 싶어요. 신용 대출까지 써서 크게 베팅할 거예요";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AssessmentRepository assessments;

    @Test
    void 서술이_설문과_정반대면_등급은_그대로_두고_확인_플래그와_학습_경로만_바뀐다() throws Exception {
        String token = loginNewUser();

        JsonNode before = body(mvc.perform(get("/me/learning-path").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn());
        assertThat(before.get("basis").isNull()).isTrue();
        assertThat(before.get("concepts").get(0).get("reasonCode").asText()).isEqualTo("STARTER");

        JsonNode result = body(mvc.perform(post("/assessments").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AssessmentDtos.SubmitRequest(lowestAnswers(), AGGRESSIVE_NARRATIVE))))
                .andExpect(status().isCreated()).andReturn());

        System.out.println("DEBUG_BODY " + result);
        System.out.println("DEBUG_SNAPSHOT " + assessments.findById(result.get("id").asText()).orElseThrow().getDecisionSnapshot());
        System.out.println("DEBUG_GET " + body(mvc.perform(get("/assessments/" + result.get("id").asText()).header("Authorization", bearer(token))).andReturn()));
        // 등급은 설문 규칙이 정한다. 서술이 공격적이어도 안정형 그대로다.
        assertThat(result.at("/profile/profileType").asText()).isEqualTo("STABLE");
        JsonNode narrative = result.at("/evidence/narrative");
        assertThat(narrative.get("status").asText()).isEqualTo("ESTIMATED");
        assertThat(narrative.get("profileType").asText()).isEqualTo("AGGRESSIVE");
        assertThat(narrative.get("mismatch").asBoolean()).isTrue();
        assertThat(codes(narrative.get("tags"))).contains("LEVERAGE_INTEREST");
        assertThat(codes(result.at("/evidence/reviewFlags"))).contains("NARRATIVE_MISMATCH");

        // 원문은 어디에도 저장하지 않는다.
        Assessment saved = assessments.findById(result.get("id").asText()).orElseThrow();
        assertThat(saved.getDecisionSnapshot()).doesNotContain("반토막").doesNotContain("베팅");

        JsonNode path = body(mvc.perform(get("/me/learning-path").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn());
        assertThat(path.at("/basis/profileType").asText()).isEqualTo("STABLE");
        assertThat(path.at("/basis/narrativeMismatch").asBoolean()).isTrue();
        assertThat(path.at("/concepts/0/reasonCode").asText()).isEqualTo("NARRATIVE_MISMATCH");
        assertThat(slugs(path.get("concepts"))).contains("risk-capacity", "leverage");
        assertThat(path.get("terms").size()).isPositive();
    }

    @Test
    void 서술을_비우면_서술_신호_없이_기존과_같이_동작하고_너무_긴_서술은_거절한다() throws Exception {
        String token = loginNewUser();
        mvc.perform(post("/assessments").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AssessmentDtos.SubmitRequest(lowestAnswers(), "가".repeat(1001)))))
                .andExpect(status().isBadRequest());

        JsonNode result = body(mvc.perform(post("/assessments").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AssessmentDtos.SubmitRequest(lowestAnswers()))))
                .andExpect(status().isCreated()).andReturn());
        assertThat(result.at("/evidence/narrative").isNull()).isTrue();
        assertThat(codes(result.at("/evidence/reviewFlags"))).doesNotContain("NARRATIVE_MISMATCH");
    }

    private static List<AssessmentDtos.AnswerItem> lowestAnswers() {
        List<AssessmentDtos.AnswerItem> answers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            answers.add(new AssessmentDtos.AnswerItem("Q" + i, "Q" + i + "C1"));
        }
        return answers;
    }

    private String loginNewUser() throws Exception {
        String email = "path-" + UUID.randomUUID() + "@example.test";
        mvc.perform(post("/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AuthDtos.SignupRequest(email, "test-password-123", "학습자"))))
                .andExpect(status().isCreated());
        return body(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AuthDtos.LoginRequest(email, "test-password-123"))))
                .andExpect(status().isOk()).andReturn()).get("accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static List<String> codes(JsonNode array) {
        List<String> codes = new ArrayList<>();
        array.forEach(item -> codes.add(item.get("code").asText()));
        return codes;
    }

    private static List<String> slugs(JsonNode array) {
        List<String> slugs = new ArrayList<>();
        array.forEach(item -> slugs.add(item.get("slug").asText()));
        return slugs;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
