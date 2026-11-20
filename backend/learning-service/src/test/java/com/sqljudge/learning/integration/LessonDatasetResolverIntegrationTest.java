package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.learning.service.LessonDataset;
import com.sqljudge.learning.service.LessonDatasetResolver;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class LessonDatasetResolverIntegrationTest extends ContainerSupport {

    private static final UUID VARIANT_ID = UUID.fromString("0192f3f5-0000-7000-8000-000000000001");
    private static final UUID VERSION_ID = UUID.fromString("0192f3f5-0000-7000-8000-000000000002");
    private static final String CHECKSUM = "a".repeat(64);
    private static final HttpServer PROBLEM_SERVICE = startProblemService();
    private static final AtomicReference<StubResponse> NEXT_RESPONSE = new AtomicReference<>();
    private static final AtomicReference<String> RECEIVED_TOKEN = new AtomicReference<>();

    @Autowired private LessonDatasetResolver resolver;

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
    void resetStub() {
        RECEIVED_TOKEN.set(null);
    }

    @Test
    @DisplayName("Bộ dữ liệu công khai của bản đã chốt được nhận, kèm bản và checksum")
    void resolve_publicFrozenVariant_returnsSnapshotReference() {
        NEXT_RESPONSE.set(StubResponse.ok(variantJson(true, true)));

        LessonDataset dataset = resolver.resolve(VARIANT_ID);

        assertThat(dataset).isEqualTo(new LessonDataset(VARIANT_ID, VERSION_ID, CHECKSUM));
        assertThat(RECEIVED_TOKEN.get()).isEqualTo("test-internal-token");
    }

    @Test
    @DisplayName("Bộ dữ liệu ẩn bị từ chối, không bao giờ gắn vào bài giảng")
    void resolve_hiddenVariant_isRejected() {
        NEXT_RESPONSE.set(StubResponse.ok(variantJson(false, true)));

        assertRejected(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DATASET_VARIANT_NOT_PUBLIC);
    }

    @Test
    @DisplayName("Bộ dữ liệu của bản chưa chốt bị từ chối vì dữ liệu còn có thể đổi")
    void resolve_draftVersion_isRejected() {
        NEXT_RESPONSE.set(StubResponse.ok(variantJson(true, false)));

        assertRejected(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DATASET_NOT_FROZEN);
    }

    @Test
    @DisplayName("problem-service trả 404 thì báo không tìm thấy bộ dữ liệu")
    void resolve_unknownVariant_isRejected() {
        NEXT_RESPONSE.set(new StubResponse(404, "{\"code\":\"DATASET_VARIANT_NOT_FOUND\"}"));

        assertRejected(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DATASET_VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("problem-service lỗi thì trả 503 thay vì gắn dữ liệu chưa kiểm tra")
    void resolve_problemServiceDown_isUnavailable() {
        NEXT_RESPONSE.set(new StubResponse(500, "{}"));

        assertRejected(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.PROBLEM_SERVICE_UNAVAILABLE);
    }

    private void assertRejected(HttpStatus status, ErrorCode code) {
        assertThatThrownBy(() -> resolver.resolve(VARIANT_ID))
                .isInstanceOfSatisfying(
                        AppException.class,
                        error -> {
                            assertThat(error.getStatus()).isEqualTo(status);
                            assertThat(error.getCode()).isEqualTo(code);
                        });
    }

    private static String variantJson(boolean isPublic, boolean isFrozen) {
        return """
        {"variantId": "%s", "versionId": "%s", "datasetName": "Công ty",
         "variantName": "Mẫu", "isPublic": %s, "isFrozen": %s, "checksum": %s}
        """
                .formatted(
                        VARIANT_ID,
                        VERSION_ID,
                        isPublic,
                        isFrozen,
                        isFrozen ? "\"" + CHECKSUM + "\"" : "null");
    }

    private static HttpServer startProblemService() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    "/internal/dataset-variants/",
                    exchange -> {
                        RECEIVED_TOKEN.set(
                                exchange.getRequestHeaders().getFirst("X-Internal-Token"));
                        StubResponse response = NEXT_RESPONSE.get();
                        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "application/json");
                        exchange.sendResponseHeaders(response.status(), body.length);
                        exchange.getResponseBody().write(body);
                        exchange.close();
                    });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private record StubResponse(int status, String body) {

        static StubResponse ok(String body) {
            return new StubResponse(200, body);
        }
    }
}
