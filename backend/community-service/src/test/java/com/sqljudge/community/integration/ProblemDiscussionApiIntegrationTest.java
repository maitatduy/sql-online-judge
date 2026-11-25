package com.sqljudge.community.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
@Import(TestJwts.DecoderConfig.class)
class ProblemDiscussionApiIntegrationTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.randomUUID();
    private static final String AUTHOR = TestJwts.token(AUTHOR_ID, "LEARNER");
    private static final String DISCUSSIONS = "/api/community/problem-discussions";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.auth-service-url", AuthServiceStub::url);
        registry.add("sqljudge.contest-service-url", ContestServiceStub::url);
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @BeforeEach
    void stubs() {
        AuthServiceStub.reset();
        ContestServiceStub.reset();
        ProblemServiceStub.reset();
        AuthServiceStub.user(AUTHOR_ID, "trang.hoang");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM comments");
    }

    @Test
    @DisplayName(
            "Khách xem bình luận mới ở các bài kèm tên bài và tác giả; bỏ bình luận khoá trong"
                    + " contest và bài đã xoá")
    void latest_listsVisibleProblemComments() {
        UUID problemId = UUID.randomUUID();
        UUID lockedProblem = UUID.randomUUID();
        UUID removedProblem = UUID.randomUUID();
        ContestServiceStub.inUnfinishedContest(lockedProblem);
        ProblemServiceStub.missing(removedProblem);
        comment(problemId, "Testcase 6 có **tên trùng**, nhớ GROUP BY theo id.");
        comment(lockedProblem, "Gợi ý trong giờ thi");
        comment(removedProblem, "Bài đã gỡ");
        comment(problemId, "Cảm ơn, mình sửa được rồi");

        ResponseEntity<String> response = restTemplate.getForEntity(DISCUSSIONS, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext items = JsonPath.parse(response.getBody());
        assertThat(items.read("$[*].excerpt", List.class))
                .containsExactly(
                        "Cảm ơn, mình sửa được rồi",
                        "Testcase 6 có tên trùng, nhớ GROUP BY theo id.");
        assertThat(items.read("$[0].problemSlug", String.class)).isEqualTo("second-highest-salary");
        assertThat(items.read("$[0].problemTitle", String.class)).isEqualTo("Lương cao thứ hai");
        assertThat(items.read("$[0].authorUsername", String.class)).isEqualTo("trang.hoang");
        assertThat(items.read("$[0].isReply", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("Số lượng ngoài khoảng 1 đến 20 trả 400")
    void latest_validatesSize() {
        assertThat(
                        restTemplate
                                .getForEntity(DISCUSSIONS + "?size=21", String.class)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void comment(UUID problemId, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(AUTHOR);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/community/comments",
                        HttpMethod.POST,
                        new HttpEntity<>(
                                "{\"problemId\": \""
                                        + problemId
                                        + "\", \"body\": \""
                                        + body
                                        + "\"}",
                                headers),
                        String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
