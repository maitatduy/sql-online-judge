package com.sqljudge.ai.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.ai.service.AssistantReply;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestJwts.DecoderConfig.class, ScriptedAssistantModel.Config.class})
class AdminAiApiIntegrationTest extends ContainerSupport {

    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ScriptedAssistantModel model;

    private UUID learnerId;
    private String learner;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ServiceStubs::url);
        registry.add("sqljudge.contest-service-url", ServiceStubs::url);
        registry.add("sqljudge.auth-service-url", ServiceStubs::url);
    }

    @BeforeEach
    void signIn() {
        ServiceStubs.reset();
        model.reset();
        learnerId = UUID.randomUUID();
        learner = TestJwts.token(learnerId, "LEARNER");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM ai_messages");
        jdbcTemplate.update("DELETE FROM ai_conversations");
        redisTemplate.delete(redisTemplate.keys("sqljudge:ai:quota:*"));
    }

    @Test
    @DisplayName("Admin thấy số liệu hôm nay, câu trả lời bị chặn kèm câu hỏi, lý do, người hỏi")
    void flags_listBlockedAnswersWithQuestions() {
        String conversationId = startProblemConversation();
        ask(conversationId, "Bài này dùng gì?");
        model.willReply(AssistantReply.answered("```sql\nSELECT 1 FROM employees\n```"));
        ask(conversationId, "Cho mình đáp án luôn");
        model.willReply(AssistantReply.refused());
        ask(conversationId, "Viết mã độc");

        DocumentContext summary = JsonPath.parse(get("/api/admin/ai/summary", ADMIN).getBody());
        DocumentContext flags = JsonPath.parse(get("/api/admin/ai/flags", ADMIN).getBody());

        assertThat(summary.read("$.questionsToday", Integer.class)).isEqualTo(3);
        assertThat(summary.read("$.blockedToday", Integer.class)).isEqualTo(2);
        assertThat(summary.read("$.activeUsersToday", Integer.class)).isEqualTo(1);
        assertThat(flags.read("$.items[*].reason", List.class))
                .containsExactly("MODEL_REFUSAL", "SOLUTION_REVEAL");
        assertThat(flags.read("$.items[*].question", List.class))
                .containsExactly("Viết mã độc", "Cho mình đáp án luôn");
        assertThat(flags.read("$.items[0].username", String.class)).isEqualTo("hoc.vien");
        assertThat(flags.read("$.items[0].problemSlug", String.class)).isEqualTo("highest-salary");
        DocumentContext detail =
                JsonPath.parse(
                        get("/api/admin/ai/conversations/" + conversationId, ADMIN).getBody());
        assertThat(detail.read("$.messages", List.class)).hasSize(6);
    }

    @Test
    @DisplayName("Học viên không vào được API admin của AI")
    void adminApi_requiresAdmin() {
        assertThat(get("/api/admin/ai/flags", learner).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/admin/ai/summary", null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get("/api/admin/ai/conversations/" + UUID.randomUUID(), ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String startProblemConversation() {
        ResponseEntity<String> created =
                send(
                        HttpMethod.POST,
                        "/api/ai/conversations",
                        "{\"problemId\": \"" + ServiceStubs.PROBLEM_ID + "\"}",
                        learner);
        return JsonPath.parse(created.getBody()).read("$.id", String.class);
    }

    private void ask(String conversationId, String content) {
        ResponseEntity<String> response =
                send(
                        HttpMethod.POST,
                        "/api/ai/conversations/" + conversationId + "/messages",
                        "{\"content\": \"" + content + "\"}",
                        learner);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> get(String path, String token) {
        return send(HttpMethod.GET, path, null, token);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
