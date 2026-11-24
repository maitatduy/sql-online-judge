package com.sqljudge.ai.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.ai.service.AssistantReply;
import com.sqljudge.ai.service.SolutionGuard;
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
class AiChatApiIntegrationTest extends ContainerSupport {

    private static final String FULL_QUERY =
            "Đây:\n```sql\nSELECT department_id, MAX(salary)\nFROM employees\nGROUP BY 1;\n```";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ScriptedAssistantModel model;

    private UUID userId;
    private String token;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ServiceStubs::url);
        registry.add("sqljudge.contest-service-url", ServiceStubs::url);
    }

    @BeforeEach
    void signIn() {
        ServiceStubs.reset();
        model.reset();
        userId = UUID.randomUUID();
        token = TestJwts.token(userId, "LEARNER");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM ai_messages");
        jdbcTemplate.update("DELETE FROM ai_conversations");
        redisTemplate.delete(redisTemplate.keys("sqljudge:ai:quota:*"));
    }

    @Test
    @DisplayName(
            "Hỏi trong hội thoại gắn bài: gửi đề, schema, lịch sử cho mô hình và lưu câu trả lời")
    void send_storesExchangeAndSendsProblemContext() {
        String conversationId = start(ServiceStubs.PROBLEM_ID);
        ask(conversationId, "Bài này dùng gì?", null);

        ResponseEntity<String> response =
                ask(conversationId, "Code mình sai ở đâu?", "SELECT * FROM employees");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext body = JsonPath.parse(response.getBody());
        assertThat(body.read("$.answer.content", String.class)).isEqualTo("Gợi ý: dùng GROUP BY.");
        assertThat(body.read("$.question.content", String.class))
                .contains("Code hiện tại của tôi:")
                .contains("SELECT * FROM employees");
        assertThat(body.read("$.quota.used", Integer.class)).isEqualTo(2);
        assertThat(model.lastSystemPrompt())
                .contains("Lương cao nhất")
                .contains("employees(salary INTEGER)")
                .contains("CHƯA giải được");
        assertThat(model.lastTurns()).hasSize(3);
        DocumentContext detail =
                JsonPath.parse(
                        send(HttpMethod.GET, "/api/ai/conversations/" + conversationId, null)
                                .getBody());
        assertThat(detail.read("$.messages[*].role", List.class))
                .containsExactly("USER", "ASSISTANT", "USER", "ASSISTANT");
        assertThat(detail.read("$.conversation.problemSlug", String.class))
                .isEqualTo("highest-salary");
    }

    @Test
    @DisplayName(
            "Chưa AC mà mô hình đưa câu truy vấn hoàn chỉnh thì thay bằng lời nhắc và đánh dấu bị"
                    + " chặn")
    void send_unsolvedFullQuery_isBlocked() {
        String conversationId = start(ServiceStubs.PROBLEM_ID);
        model.willReply(AssistantReply.answered(FULL_QUERY));

        DocumentContext body =
                JsonPath.parse(ask(conversationId, "Cho mình đáp án", null).getBody());

        assertThat(body.read("$.answer.content", String.class))
                .isEqualTo(SolutionGuard.BLOCKED_REPLY);
        assertThat(body.read("$.answer.isBlocked", Boolean.class)).isTrue();

        ServiceStubs.solved(userId);
        DocumentContext solved =
                JsonPath.parse(ask(conversationId, "Giờ thì sao?", null).getBody());
        assertThat(solved.read("$.answer.content", String.class)).isEqualTo(FULL_QUERY);
        assertThat(model.lastSystemPrompt()).contains("ĐÃ giải được");
    }

    @Test
    @DisplayName("Hết lượt hỏi trong ngày thì trả 429 kèm Retry-After, không gọi mô hình")
    void send_overDailyLimit_isRejected() {
        String conversationId = start(null);
        for (int asked = 0; asked < 3; asked++) {
            assertThat(ask(conversationId, "Câu " + asked, null).getStatusCode())
                    .isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<String> response = ask(conversationId, "Câu thứ tư", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("AI_QUOTA_EXCEEDED");
        assertThat(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
        DocumentContext quota =
                JsonPath.parse(send(HttpMethod.GET, "/api/ai/quota", null).getBody());
        assertThat(quota.read("$.used", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("Đang thi contest tính rating thì AI bị khoá phía server")
    void send_duringRatedContest_isForbidden() {
        String conversationId = start(null);
        ServiceStubs.inRatedContest(userId);

        ResponseEntity<String> response = ask(conversationId, "Gợi ý bài A", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(JsonPath.parse(response.getBody()).read("$.code", String.class))
                .isEqualTo("AI_DISABLED_DURING_CONTEST");
        assertThat(send(HttpMethod.POST, "/api/ai/conversations", "{}").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName(
            "Không xem hay hỏi tiếp được hội thoại của người khác; khách bị 401; câu hỏi trống 400")
    void conversations_areOwnedAndValidated() {
        String conversationId = start(null);
        String stranger = TestJwts.token(UUID.randomUUID(), "LEARNER");

        assertThat(
                        exchange(
                                        HttpMethod.GET,
                                        "/api/ai/conversations/" + conversationId,
                                        null,
                                        stranger)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange(HttpMethod.GET, "/api/ai/quota", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ask(conversationId, " ", null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        send(
                                        HttpMethod.POST,
                                        "/api/ai/conversations",
                                        "{\"problemId\": \"" + UUID.randomUUID() + "\"}")
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String start(UUID problemId) {
        String body = problemId == null ? "{}" : "{\"problemId\": \"" + problemId + "\"}";
        ResponseEntity<String> created = send(HttpMethod.POST, "/api/ai/conversations", body);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return JsonPath.parse(created.getBody()).read("$.id", String.class);
    }

    private ResponseEntity<String> ask(String conversationId, String content, String code) {
        String body =
                code == null
                        ? "{\"content\": \"%s\"}".formatted(content)
                        : "{\"content\": \"%s\", \"code\": \"%s\"}".formatted(content, code);
        return send(HttpMethod.POST, "/api/ai/conversations/" + conversationId + "/messages", body);
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String body) {
        return exchange(method, path, body, token);
    }

    private ResponseEntity<String> exchange(
            HttpMethod method, String path, String body, String bearer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (bearer != null) {
            headers.setBearerAuth(bearer);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
