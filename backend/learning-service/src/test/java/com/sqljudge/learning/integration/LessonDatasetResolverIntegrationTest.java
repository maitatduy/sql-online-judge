package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.AppException;
import com.sqljudge.learning.service.LessonDataset;
import com.sqljudge.learning.service.LessonDatasetResolver;
import java.util.UUID;
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

    private static final UUID VERSION_ID = UUID.fromString("0192f3f5-0000-7000-8000-000000000002");

    @Autowired private LessonDatasetResolver resolver;

    @DynamicPropertySource
    static void problemServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("sqljudge.problem-service-url", ProblemServiceStub::url);
    }

    @Test
    @DisplayName("Bộ dữ liệu công khai của bản đã chốt được nhận, kèm bản và checksum")
    void resolve_publicFrozenVariant_returnsSnapshotReference() {
        UUID variantId = UUID.randomUUID();
        ProblemServiceStub.variant(variantId, VERSION_ID, true, true);

        LessonDataset dataset = resolver.resolve(variantId);

        assertThat(dataset)
                .isEqualTo(
                        new LessonDataset(
                                variantId, VERSION_ID, ProblemServiceStub.checksumOf(VERSION_ID)));
        assertThat(ProblemServiceStub.receivedToken()).isEqualTo("test-internal-token");
    }

    @Test
    @DisplayName("Bộ dữ liệu ẩn bị từ chối, không bao giờ gắn vào bài giảng")
    void resolve_hiddenVariant_isRejected() {
        UUID variantId = UUID.randomUUID();
        ProblemServiceStub.variant(variantId, VERSION_ID, false, true);

        assertRejected(
                variantId, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DATASET_VARIANT_NOT_PUBLIC);
    }

    @Test
    @DisplayName("Bộ dữ liệu của bản chưa chốt bị từ chối vì dữ liệu còn có thể đổi")
    void resolve_draftVersion_isRejected() {
        UUID variantId = UUID.randomUUID();
        ProblemServiceStub.variant(variantId, VERSION_ID, true, false);

        assertRejected(variantId, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DATASET_NOT_FROZEN);
    }

    @Test
    @DisplayName("problem-service trả 404 thì báo không tìm thấy bộ dữ liệu")
    void resolve_unknownVariant_isRejected() {
        assertRejected(
                UUID.randomUUID(),
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCode.DATASET_VARIANT_NOT_FOUND);
    }

    @Test
    @DisplayName("problem-service lỗi thì trả 503 thay vì gắn dữ liệu chưa kiểm tra")
    void resolve_problemServiceDown_isUnavailable() {
        UUID variantId = UUID.randomUUID();
        ProblemServiceStub.respond(variantId, 500, "{}");

        assertRejected(
                variantId, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.PROBLEM_SERVICE_UNAVAILABLE);
    }

    private void assertRejected(UUID variantId, HttpStatus status, ErrorCode code) {
        assertThatThrownBy(() -> resolver.resolve(variantId))
                .isInstanceOfSatisfying(
                        AppException.class,
                        error -> {
                            assertThat(error.getStatus()).isEqualTo(status);
                            assertThat(error.getCode()).isEqualTo(code);
                        });
    }
}
