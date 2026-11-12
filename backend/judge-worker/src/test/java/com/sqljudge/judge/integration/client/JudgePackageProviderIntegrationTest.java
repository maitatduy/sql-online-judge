package com.sqljudge.judge.integration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.judge.client.JudgePackageProvider;
import com.sqljudge.judge.client.JudgePackageUnavailableException;
import com.sqljudge.judge.compare.Cell.TextCell;
import com.sqljudge.judge.guard.StatementType;
import com.sqljudge.judge.job.JudgePackage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class JudgePackageProviderIntegrationTest {

    private static final UUID REVISION_ID = UUID.randomUUID();
    private static final UUID MISSING_REVISION_ID = UUID.randomUUID();
    private static final UUID BROKEN_REVISION_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID SAMPLE_VARIANT = UUID.randomUUID();
    private static final UUID HIDDEN_VARIANT = UUID.randomUUID();
    private static final String PACKAGE_JSON =
            """
            {"revisionId": "%s", "datasetVersionId": "%s", "datasetChecksum": "abc123",
             "requiresOrder": true, "compareColumnNames": false, "floatTolerance": 0.01,
             "timeLimitMs": 3000, "allowedStatements": ["SELECT", "WITH"],
             "testcases": [
               {"testcaseId": "%s", "index": 1, "variantId": "%s", "isPublic": true,
                "expected": {"columns": ["name"],
                             "rows": [[{"kind": "TEXT", "value": "Kỹ thuật"}]]}},
               {"testcaseId": "%s", "index": 2, "variantId": "%s", "isPublic": false,
                "expected": {"columns": ["name"],
                             "rows": [[{"kind": "TEXT", "value": "Kinh doanh"}]]}}]}
            """;
    private static final HttpServer PROBLEM_SERVICE = startProblemService();
    private static final AtomicInteger PACKAGE_REQUESTS = new AtomicInteger();
    private static volatile String lastToken;

    @Autowired private JudgePackageProvider judgePackageProvider;

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

    @Test
    @DisplayName("Tải gói chấm qua API nội bộ kèm token, đọc đúng cài đặt, testcase và lưu cache")
    void packageOf_frozenRevision_mapsAndCachesThePackage() {
        JudgePackage judgePackage = judgePackageProvider.packageOf(REVISION_ID);
        judgePackageProvider.packageOf(REVISION_ID);

        assertThat(lastToken).isEqualTo("test-internal-token");
        assertThat(PACKAGE_REQUESTS.get()).isEqualTo(1);
        assertThat(judgePackage.datasetVersionId()).isEqualTo(VERSION_ID);
        assertThat(judgePackage.datasetChecksum()).isEqualTo("abc123");
        assertThat(judgePackage.compareOptions().ordered()).isTrue();
        assertThat(judgePackage.compareOptions().compareColumnNames()).isFalse();
        assertThat(judgePackage.compareOptions().tolerance())
                .isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(judgePackage.timeLimitMs()).isEqualTo(3000);
        assertThat(judgePackage.allowedStatements())
                .containsExactlyInAnyOrder(StatementType.SELECT, StatementType.WITH);
        assertThat(judgePackage.testcases())
                .extracting(JudgePackage.JudgeCase::index, JudgePackage.JudgeCase::isPublic)
                .containsExactly(tuple(1, true), tuple(2, false));
        assertThat(judgePackage.testcases().get(0).variantId()).isEqualTo(SAMPLE_VARIANT);
        assertThat(judgePackage.testcases().get(0).expected().rows().get(0).cells())
                .containsExactly(new TextCell("Kỹ thuật"));
        assertThat(judgePackage.testcases().get(1).expected().rows().get(0).cells())
                .containsExactly(new TextCell("Kinh doanh"));
    }

    @Test
    @DisplayName("Bản không có hoặc problem-service lỗi thì báo không tải được gói chấm")
    void packageOf_missingOrServerError_isUnavailable() {
        assertThatThrownBy(() -> judgePackageProvider.packageOf(MISSING_REVISION_ID))
                .isInstanceOf(JudgePackageUnavailableException.class);
        assertThatThrownBy(() -> judgePackageProvider.packageOf(BROKEN_REVISION_ID))
                .isInstanceOf(JudgePackageUnavailableException.class);
    }

    private static HttpServer startProblemService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    "/internal/problem-revisions/", JudgePackageProviderIntegrationTest::respond);
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        lastToken = exchange.getRequestHeaders().getFirst("X-Internal-Token");
        String path = exchange.getRequestURI().getPath();
        if (path.contains(BROKEN_REVISION_ID.toString())) {
            write(exchange, 500, "{}");
            return;
        }
        if (!path.contains(REVISION_ID.toString())) {
            write(exchange, 404, "{\"code\":\"PROBLEM_REVISION_NOT_FOUND\"}");
            return;
        }
        PACKAGE_REQUESTS.incrementAndGet();
        write(
                exchange,
                200,
                PACKAGE_JSON.formatted(
                        REVISION_ID,
                        VERSION_ID,
                        UUID.randomUUID(),
                        SAMPLE_VARIANT,
                        UUID.randomUUID(),
                        HIDDEN_VARIANT));
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
