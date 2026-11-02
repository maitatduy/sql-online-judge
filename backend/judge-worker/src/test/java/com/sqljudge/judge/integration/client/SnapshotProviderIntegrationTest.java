package com.sqljudge.judge.integration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.judge.client.DatasetSnapshotResponse;
import com.sqljudge.judge.client.SnapshotProvider;
import com.sqljudge.judge.client.SnapshotUnavailableException;
import com.sqljudge.judge.sandbox.LogicalType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SnapshotProviderIntegrationTest {

    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID BROKEN_VERSION_ID = UUID.randomUUID();
    private static final String SNAPSHOT_JSON =
            """
            {"versionId": "%s", "checksum": "abc123",
             "tables": [{"name": "departments", "columns": [
               {"name": "id", "type": "INTEGER", "isPrimaryKey": true, "isNullable": false,
                "references": null},
               {"name": "parent_id", "type": "INTEGER", "isPrimaryKey": false,
                "isNullable": true, "references": {"table": "departments", "column": "id"}}]}],
             "variants": [{"id": "%s", "name": "Ẩn", "isPublic": false,
               "tables": [{"tableName": "departments", "rows": [["1", null]]}]}]}
            """;
    private static final HttpServer PROBLEM_SERVICE = startProblemService();
    private static final AtomicInteger SNAPSHOT_REQUESTS = new AtomicInteger();
    private static volatile String lastToken;

    @Autowired private SnapshotProvider snapshotProvider;

    @DynamicPropertySource
    static void problemServiceUrl(DynamicPropertyRegistry registry) {
        registry.add(
                "sqljudge.problem-service-url",
                () -> "http://localhost:" + PROBLEM_SERVICE.getAddress().getPort());
    }

    @AfterAll
    static void stopProblemService() {
        PROBLEM_SERVICE.stop(0);
    }

    @BeforeEach
    void resetRequests() {
        SNAPSHOT_REQUESTS.set(0);
    }

    @Test
    @DisplayName("Tải snapshot qua API nội bộ kèm token, đọc đúng schema và dữ liệu")
    void snapshotOf_matchingChecksum_returnsSnapshotAndCachesIt() {
        DatasetSnapshotResponse snapshot = snapshotProvider.snapshotOf(VERSION_ID, "abc123");
        snapshotProvider.snapshotOf(VERSION_ID, "abc123");

        assertThat(lastToken).isEqualTo("test-internal-token");
        assertThat(snapshot.tables().get(0).columns().get(0).type()).isEqualTo(LogicalType.INTEGER);
        assertThat(snapshot.tables().get(0).columns().get(0).isPrimaryKey()).isTrue();
        assertThat(snapshot.tables().get(0).columns().get(1).references().table())
                .isEqualTo("departments");
        assertThat(snapshot.variants().get(0).isPublic()).isFalse();
        assertThat(snapshot.variants().get(0).tables().get(0).rows().get(0))
                .containsExactly("1", null);
        assertThat(SNAPSHOT_REQUESTS.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Checksum không khớp hoặc problem-service lỗi thì báo không tải được snapshot")
    void snapshotOf_mismatchOrServerError_isUnavailable() {
        assertThatThrownBy(() -> snapshotProvider.snapshotOf(VERSION_ID, "other-checksum"))
                .isInstanceOf(SnapshotUnavailableException.class);
        assertThatThrownBy(() -> snapshotProvider.snapshotOf(BROKEN_VERSION_ID, "abc123"))
                .isInstanceOf(SnapshotUnavailableException.class);
    }

    private static HttpServer startProblemService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    "/internal/dataset-versions/", SnapshotProviderIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        lastToken = exchange.getRequestHeaders().getFirst("X-Internal-Token");
        String path = exchange.getRequestURI().getPath();
        if (path.contains(BROKEN_VERSION_ID.toString())) {
            write(exchange, 500, "{}");
            return;
        }
        SNAPSHOT_REQUESTS.incrementAndGet();
        write(exchange, 200, SNAPSHOT_JSON.formatted(VERSION_ID, UUID.randomUUID()));
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
