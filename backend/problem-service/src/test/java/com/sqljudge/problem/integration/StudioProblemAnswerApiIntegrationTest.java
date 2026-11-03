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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioProblemAnswerApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private JdbcTemplate jdbcTemplate;

    private StudioFixtures studio;
    private String problemId;
    private String datasetId;
    private String sampleVariantId;
    private String hiddenVariantId;

    @BeforeEach
    void createProblemWithDataset() {
        studio = new StudioFixtures(restTemplate, SETTER);
        problemId = studio.createProblem("Phòng ban");
        datasetId = studio.createReadyDataset("Công ty");
        hiddenVariantId = studio.addVariant(datasetId, "Ẩn", false);
        sampleVariantId = studio.variantIdsOf(datasetId).get(0);
        studio.chooseDataset(problemId, datasetId);
    }

    @AfterEach
    void deleteAll() {
        jdbcTemplate.update("UPDATE problems SET draft_revision_id = NULL");
        jdbcTemplate.update("DELETE FROM problems");
        jdbcTemplate.update("DELETE FROM datasets");
    }

    @Test
    @DisplayName("Bài mới chưa có đáp án: danh sách trống và cài đặt so khớp mặc định")
    void getAnswer_newProblem_isEmptyWithDefaults() {
        DocumentContext answer =
                JsonPath.parse(studio.send(HttpMethod.GET, answerPath(), null).getBody());

        assertThat(answer.read("$.solutions", List.class)).isEmpty();
        assertThat(answer.read("$.testcases", List.class)).isEmpty();
        assertThat(answer.read("$.requiresOrder", Boolean.class)).isFalse();
        assertThat(answer.read("$.compareColumnNames", Boolean.class)).isFalse();
        assertThat(answer.read("$.floatTolerance", Double.class)).isEqualTo(1e-6);
    }

    @Test
    @DisplayName("Lưu đáp án theo dialect, testcase theo thứ tự đã chọn và cài đặt so khớp")
    void saveAnswer_validRequest_isStoredAndReturned() {
        ResponseEntity<String> saved =
                studio.send(
                        HttpMethod.PUT,
                        answerPath(),
                        answer(
                                "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT name FROM"
                                        + " departments\"},{\"dialect\":\"SQLITE\",\"sql\":\"SELECT"
                                        + " name FROM departments;\"}",
                                hiddenVariantId,
                                sampleVariantId));

        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext answer =
                JsonPath.parse(studio.send(HttpMethod.GET, answerPath(), null).getBody());
        assertThat(answer.read("$.solutions[*].dialect", List.class))
                .containsExactly("POSTGRESQL", "SQLITE");
        assertThat(answer.read("$.solutions[1].sql", String.class))
                .isEqualTo("SELECT name FROM departments;");
        assertThat(answer.read("$.testcases[*].variantId", List.class))
                .containsExactly(hiddenVariantId, sampleVariantId);
        assertThat(answer.read("$.testcases[0].variantName", String.class)).isEqualTo("Ẩn");
        assertThat(answer.read("$.testcases[0].isPublic", Boolean.class)).isFalse();
        assertThat(answer.read("$.testcases[1].isPublic", Boolean.class)).isTrue();
        assertThat(answer.read("$.requiresOrder", Boolean.class)).isTrue();
        assertThat(answer.read("$.floatTolerance", Double.class)).isEqualTo(0.01);
    }

    @Test
    @DisplayName("Lưu lại đáp án cùng dialect thì cập nhật, bỏ dialect thì xoá đáp án đó")
    void saveAnswer_again_updatesInPlace() {
        studio.send(
                HttpMethod.PUT,
                answerPath(),
                answer(
                        "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"},"
                                + "{\"dialect\":\"MYSQL\",\"sql\":\"SELECT 1\"}",
                        sampleVariantId));

        ResponseEntity<String> resaved =
                studio.send(
                        HttpMethod.PUT,
                        answerPath(),
                        answer(
                                "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 2\"}",
                                sampleVariantId));

        assertThat(resaved.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext answer = JsonPath.parse(resaved.getBody());
        assertThat(answer.read("$.solutions[*].dialect", List.class)).containsExactly("POSTGRESQL");
        assertThat(answer.read("$.solutions[0].sql", String.class)).isEqualTo("SELECT 2");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM reference_solutions", Long.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Thiếu đáp án PostgreSQL, trùng dialect hay bộ dữ liệu lạ đều báo lỗi đúng trường")
    void saveAnswer_invalidAnswer_reportsField() {
        assertFieldError(
                answer("{\"dialect\":\"MYSQL\",\"sql\":\"SELECT 1\"}", sampleVariantId),
                "solutions",
                "Cần có đáp án PostgreSQL");
        assertFieldError(
                answer(
                        "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"},"
                                + "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 2\"}",
                        sampleVariantId),
                "solutions",
                "Mỗi dialect chỉ có một đáp án");
        assertFieldError(
                answer(
                        "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"}",
                        sampleVariantId,
                        sampleVariantId),
                "testcaseVariantIds",
                "Mỗi bộ dữ liệu chỉ chọn một lần");
        assertFieldError(
                answer(
                        "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"}",
                        UUID.randomUUID().toString()),
                "testcaseVariantIds",
                "Bộ dữ liệu không thuộc database của bài");
        assertFieldError(
                answer("{\"dialect\":\"POSTGRESQL\",\"sql\":\" \"}", sampleVariantId),
                "solutions[0].sql",
                "Không được để trống");
    }

    @Test
    @DisplayName("Bài chưa chọn database hoặc đã khoá thì không lưu đáp án được: 422")
    void saveAnswer_missingDatasetOrLocked_isRejected() {
        String bare = studio.createProblem("Chưa có database");
        ResponseEntity<String> missing =
                studio.send(
                        HttpMethod.PUT,
                        "/api/studio/problems/" + bare + "/answer",
                        answer(
                                "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"}",
                                sampleVariantId));
        jdbcTemplate.update(
                "UPDATE problems SET status = 'PENDING_REVIEW' WHERE id = UUID_TO_BIN(?)",
                problemId);
        ResponseEntity<String> locked =
                studio.send(
                        HttpMethod.PUT,
                        answerPath(),
                        answer(
                                "{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"}",
                                sampleVariantId));

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(missing.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_DATASET_MISSING");
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(JsonPath.parse(locked.getBody()).read("$.code", String.class))
                .isEqualTo("PROBLEM_LOCKED");
    }

    @Test
    @DisplayName("Học viên nhận 403, người ra đề khác nhận 404")
    void answer_wrongUser_isRejected() {
        String body = answer("{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"}", sampleVariantId);

        assertThat(
                        studio.send(HttpMethod.PUT, answerPath(), body, TestJwts.learnerToken())
                                .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(studio.send(HttpMethod.GET, answerPath(), null, OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Đổi sang database khác thì danh sách testcase cũ bị bỏ")
    void chooseOtherDataset_clearsTestcases() {
        studio.send(
                HttpMethod.PUT,
                answerPath(),
                answer("{\"dialect\":\"POSTGRESQL\",\"sql\":\"SELECT 1\"}", sampleVariantId));

        studio.chooseDataset(problemId, studio.createReadyDataset("Trường học"));

        DocumentContext answer =
                JsonPath.parse(studio.send(HttpMethod.GET, answerPath(), null).getBody());
        assertThat(answer.read("$.testcases", List.class)).isEmpty();
        assertThat(answer.read("$.solutions", List.class)).hasSize(1);
    }

    private void assertFieldError(String body, String field, String message) {
        ResponseEntity<String> response = studio.send(HttpMethod.PUT, answerPath(), body);
        assertThat(response.getStatusCode()).as(body).isEqualTo(HttpStatus.BAD_REQUEST);
        DocumentContext error = JsonPath.parse(response.getBody());
        assertThat(error.read("$.errors[0].field", String.class)).isEqualTo(field);
        assertThat(error.read("$.errors[0].message", String.class)).isEqualTo(message);
    }

    private static String answer(String solutions, String... variantIds) {
        String ids = String.join("\",\"", variantIds);
        return "{\"solutions\":["
                + solutions
                + "],\"testcaseVariantIds\":[\""
                + ids
                + "\"],\"requiresOrder\":true,\"compareColumnNames\":false,\"floatTolerance\":0.01}";
    }

    private String answerPath() {
        return "/api/studio/problems/" + problemId + "/answer";
    }
}
