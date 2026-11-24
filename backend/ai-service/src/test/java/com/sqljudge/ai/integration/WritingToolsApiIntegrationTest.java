package com.sqljudge.ai.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.ai.service.AssistantReply;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestJwts.DecoderConfig.class, ScriptedAssistantModel.Config.class})
class WritingToolsApiIntegrationTest extends ContainerSupport {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ScriptedAssistantModel model;

    private UUID userId;
    private String token;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ServiceStubs::url);
        registry.add("sqljudge.contest-service-url", ServiceStubs::url);
        registry.add("sqljudge.auth-service-url", ServiceStubs::url);
        registry.add("sqljudge.community-service-url", ServiceStubs::url);
    }

    @BeforeEach
    void signIn() {
        ServiceStubs.reset();
        model.reset();
        userId = UUID.randomUUID();
        token = TestJwts.token(userId, "LEARNER");
    }

    @AfterEach
    void clearQuota() {
        redisTemplate.delete(redisTemplate.keys("sqljudge:ai:quota:*"));
    }

    @Test
    @DisplayName("Tóm tắt đọc bài và mọi bình luận kể cả trả lời, tính một lượt hỏi")
    void summarize_readsPostAndAllComments() {
        model.willReply(AssistantReply.answered("- COUNT(cột) bỏ qua NULL."));

        ResponseEntity<String> response =
                post(
                        "/api/ai/discussion-summaries",
                        "{\"postId\": \"" + ServiceStubs.POST_ID + "\"}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext body = JsonPath.parse(response.getBody());
        assertThat(body.read("$.text", String.class)).isEqualTo("- COUNT(cột) bỏ qua NULL.");
        assertThat(body.read("$.quota.used", Integer.class)).isEqualTo(1);
        assertThat(model.lastTurns().get(0).content())
                .contains("COUNT(*) hay COUNT(cột)?")
                .contains("- an.bui: COUNT(cột) bỏ qua NULL.")
                .contains("- minh.anh: Cảm ơn!");
        assertThat(model.lastSystemPrompt()).contains("tóm tắt thảo luận");
    }

    @Test
    @DisplayName("Sửa chính tả trả lại văn bản đã sửa; bài không tồn tại 404; đang thi thì 403")
    void proofreadAndGuards() {
        model.willReply(AssistantReply.answered("Câu truy vấn đúng."));

        DocumentContext fixed =
                JsonPath.parse(
                        post("/api/ai/proofreads", "{\"text\": \"cau truy van dung.\"}").getBody());
        assertThat(fixed.read("$.text", String.class)).isEqualTo("Câu truy vấn đúng.");
        assertThat(model.lastSystemPrompt()).contains("sửa chính tả");

        assertThat(
                        post(
                                        "/api/ai/discussion-summaries",
                                        "{\"postId\": \"" + UUID.randomUUID() + "\"}")
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(post("/api/ai/proofreads", "{\"text\": \" \"}").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        ServiceStubs.inRatedContest(userId);
        assertThat(post("/api/ai/proofreads", "{\"text\": \"abc\"}").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<String> post(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return restTemplate.exchange(
                path, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}
