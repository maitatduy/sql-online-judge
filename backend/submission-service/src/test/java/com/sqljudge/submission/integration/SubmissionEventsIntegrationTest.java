package com.sqljudge.submission.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sqljudge.submission.entity.JudgeResult;
import com.sqljudge.submission.entity.Verdict;
import com.sqljudge.submission.service.JudgeResultService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class SubmissionEventsIntegrationTest extends ContainerSupport {

    private static final String PROBLEM_ID = "0192f3d0-0000-7000-8000-000000000007";
    private static final String END_OF_STREAM = "<end>";
    private static final long TIMEOUT_SECONDS = 20;

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private JudgeResultService judgeResultService;

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
        jdbcTemplate.update("DELETE FROM submissions");
    }

    @Test
    @DisplayName("Luồng sự kiện gửi trạng thái hiện tại, tiến độ, kết quả rồi tự đóng")
    void events_streamProgressAndVerdictThenClose() throws Exception {
        UUID id = storeSubmission(TestJwts.LEARNER_ID, "PENDING");
        BlockingQueue<String> events = open(id, TestJwts.learnerToken());

        assertThat(nextEvent(events).read("$.status", String.class)).isEqualTo("PENDING");

        judgeResultService.recordProgress(id, 1, 1);
        DocumentContext progress = nextEvent(events);
        assertThat(progress.read("$.status", String.class)).isEqualTo("JUDGING");
        assertThat(progress.read("$.judgedTestcases", Integer.class)).isEqualTo(1);

        judgeResultService.complete(
                id, new JudgeResult(1, Verdict.AC, 9, null, null, null, null, 2));
        DocumentContext completed = nextEvent(events);
        assertThat(completed.read("$.status", String.class)).isEqualTo("COMPLETED");
        assertThat(completed.read("$.verdict", String.class)).isEqualTo("AC");
        assertThat(events.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(END_OF_STREAM);
    }

    @Test
    @DisplayName("Bài nộp đã chấm xong thì gửi kết quả một lần rồi đóng ngay")
    void events_completedSubmission_sendsOnceAndCloses() throws Exception {
        UUID id = storeSubmission(TestJwts.LEARNER_ID, "COMPLETED");
        BlockingQueue<String> events = open(id, TestJwts.learnerToken());

        assertThat(nextEvent(events).read("$.status", String.class)).isEqualTo("COMPLETED");
        assertThat(events.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(END_OF_STREAM);
    }

    @Test
    @DisplayName("Bài nộp của người khác trả 404, chưa đăng nhập trả 401")
    void events_foreignOrAnonymous_isRejected() throws Exception {
        UUID foreign = storeSubmission(UUID.randomUUID(), "PENDING");

        assertThat(status(foreign, TestJwts.learnerToken())).isEqualTo(404);
        assertThat(status(foreign, null)).isEqualTo(401);
    }

    private UUID storeSubmission(UUID userId, String status) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO submissions (id, user_id, problem_id, problem_revision_id, dialect,"
                        + " code, status, judged_testcases, total_testcases, attempts, version,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), UUID_TO_BIN(UUID()), 'POSTGRESQL', 'SELECT 1', ?,"
                        + " 0, 2, 1, 0, NOW(6), NOW(6))",
                id.toString(),
                userId.toString(),
                PROBLEM_ID,
                status);
        return id;
    }

    private BlockingQueue<String> open(UUID id, String bearer) {
        BlockingQueue<String> events = new LinkedBlockingQueue<>();
        httpClient
                .sendAsync(request(id, bearer), HttpResponse.BodyHandlers.ofLines())
                .thenAccept(
                        response -> {
                            try (Stream<String> lines = response.body()) {
                                lines.filter(line -> line.startsWith("data:"))
                                        .map(line -> line.substring("data:".length()))
                                        .forEach(events::add);
                            }
                            events.add(END_OF_STREAM);
                        });
        return events;
    }

    private int status(UUID id, String bearer) throws IOException, InterruptedException {
        return httpClient
                .send(request(id, bearer), HttpResponse.BodyHandlers.discarding())
                .statusCode();
    }

    private HttpRequest request(UUID id, String bearer) {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder(
                                URI.create(
                                        "http://localhost:"
                                                + port
                                                + "/api/submissions/"
                                                + id
                                                + "/events"))
                        .header("Accept", "text/event-stream");
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return builder.GET().build();
    }

    private static DocumentContext nextEvent(BlockingQueue<String> events)
            throws InterruptedException {
        String data = events.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(data).isNotNull().isNotEqualTo(END_OF_STREAM);
        return JsonPath.parse(data);
    }
}
