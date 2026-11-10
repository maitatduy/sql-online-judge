package com.sqljudge.problem.integration;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class AdminProblemQueueApiIntegrationTest extends ContainerSupport {

    private static final UUID SETTER_ID = UUID.randomUUID();
    private static final String SETTER = TestJwts.token(SETTER_ID, "SETTER");
    private static final String ADMIN = TestJwts.token(UUID.randomUUID(), "ADMIN");
    private static final String QUEUE = "/api/admin/problems";

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String newerId;
    private String olderId;
    private String draftId;

    @BeforeEach
    void createProblems() {
        studio = new StudioFixtures(restTemplate, SETTER);
        newerId = studio.createProblem("Bài gửi sau");
        olderId = studio.createProblem("Bài gửi trước");
        draftId = studio.createProblem("Bài nháp");
        markPendingReview(newerId, "2026-11-10 09:00:00");
        markPendingReview(olderId, "2026-11-10 08:00:00");
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
    }

    @Test
    @DisplayName("Mặc định liệt kê bài chờ duyệt, bài gửi lâu nhất lên đầu, kèm người ra đề")
    void listQueue_defaultsToPendingReviewOldestFirst() {
        DocumentContext page =
                JsonPath.parse(studio.send(HttpMethod.GET, QUEUE, null, ADMIN).getBody());

        assertThat(page.read("$.items[*].id", List.class)).containsExactly(olderId, newerId);
        assertThat(page.read("$.items[0].title", String.class)).isEqualTo("Bài gửi trước");
        assertThat(page.read("$.items[0].status", String.class)).isEqualTo("PENDING_REVIEW");
        assertThat(page.read("$.items[0].authorId", String.class)).isEqualTo(SETTER_ID.toString());
        assertThat(page.read("$.items[0].updatedAt", String.class)).isNotBlank();
        assertThat(page.read("$.totalItems", Integer.class)).isEqualTo(2);
        assertThat(page.read("$.page", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Lọc theo trạng thái khác và phân trang")
    void listQueue_filtersByStatusAndPages() {
        DocumentContext drafts =
                JsonPath.parse(
                        studio.send(HttpMethod.GET, QUEUE + "?status=DRAFT", null, ADMIN)
                                .getBody());
        assertThat(drafts.read("$.items[*].id", List.class)).containsExactly(draftId);

        DocumentContext secondPage =
                JsonPath.parse(
                        studio.send(HttpMethod.GET, QUEUE + "?page=2&size=1", null, ADMIN)
                                .getBody());
        assertThat(secondPage.read("$.items[*].id", List.class)).containsExactly(newerId);
        assertThat(secondPage.read("$.totalPages", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("Tham số sai trả 400, người không phải ADMIN nhận 403")
    void listQueue_invalidParamsOrNonAdmin_isRejected() {
        assertThat(studio.send(HttpMethod.GET, QUEUE + "?size=101", null, ADMIN).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(
                        studio.send(HttpMethod.GET, QUEUE + "?status=UNKNOWN", null, ADMIN)
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(studio.send(HttpMethod.GET, QUEUE, null, SETTER).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(
                        studio.send(
                                        HttpMethod.GET,
                                        QUEUE,
                                        null,
                                        TestJwts.token(UUID.randomUUID(), "MODERATOR"))
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void markPendingReview(String problemId, String updatedAt) {
        jdbcTemplate.update(
                "UPDATE problems SET status = 'PENDING_REVIEW', updated_at = ?"
                        + " WHERE id = UUID_TO_BIN(?)",
                updatedAt,
                problemId);
    }
}
