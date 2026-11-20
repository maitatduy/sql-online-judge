package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwts.DecoderConfig.class)
class StudioQuizApiIntegrationTest extends ContainerSupport {

    private static final String SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");
    private static final String OTHER_SETTER = TestJwts.token(UUID.randomUUID(), "SETTER");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private String questionsPath;

    @BeforeEach
    void createLesson() throws Exception {
        new CourseFixtures(jdbcTemplate).clear();
        String courseId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/courses",
                                "{\"slug\": \"sql-moi\", \"title\": \"SQL mới\","
                                        + " \"summary\": \"Tóm tắt.\", \"difficulty\": \"EASY\"}",
                                SETTER));
        String chapterId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/courses/" + courseId + "/chapters",
                                "{\"title\": \"Làm quen\"}",
                                SETTER));
        String lessonId =
                idOf(
                        send(
                                HttpMethod.POST,
                                "/api/studio/chapters/" + chapterId + "/lessons",
                                "{\"slug\": \"bai-1\", \"title\": \"Bài 1\", \"body\": \"\","
                                        + " \"estimatedMinutes\": 10}",
                                SETTER));
        questionsPath = "/api/studio/lessons/" + lessonId + "/quiz-questions";
    }

    @Test
    @DisplayName("Thêm câu hỏi kèm lựa chọn, liệt kê theo thứ tự")
    void questions_areAddedInOrderWithOptions() throws Exception {
        String first =
                idOf(
                        addQuestion(
                                "Dấu * sau SELECT nghĩa là gì?",
                                false,
                                option("Mọi cột", true),
                                option("Mọi bảng", false)));
        addQuestion(
                "Toán tử nào ghép điều kiện?",
                true,
                option("AND", true),
                option("OR", true),
                option("ORDER BY", false));

        JsonNode questions = body(send(HttpMethod.GET, questionsPath, null, SETTER));

        assertThat(questions)
                .extracting(question -> question.get("prompt").asText())
                .containsExactly("Dấu * sau SELECT nghĩa là gì?", "Toán tử nào ghép điều kiện?");
        assertThat(questions.get(0).get("id").asText()).isEqualTo(first);
        assertThat(questions.get(0).get("multiple").asBoolean()).isFalse();
        assertThat(questions.get(1).get("options"))
                .extracting(
                        item -> item.get("label").asText() + "=" + item.get("correct").asBoolean())
                .containsExactly("AND=true", "OR=true", "ORDER BY=false");
    }

    @Test
    @DisplayName("Sửa câu hỏi thay toàn bộ lựa chọn, xoá câu hỏi xoá luôn lựa chọn")
    void questions_areReplacedAndDeleted() throws Exception {
        String question = idOf(addQuestion("Câu cũ", false, option("A", true), option("B", false)));

        JsonNode replaced =
                body(
                        send(
                                HttpMethod.PUT,
                                "/api/studio/quiz-questions/" + question,
                                questionJson(
                                        "Câu mới",
                                        true,
                                        option("X", true),
                                        option("Y", true),
                                        option("Z", false)),
                                SETTER));
        assertThat(replaced.get("prompt").asText()).isEqualTo("Câu mới");
        assertThat(replaced.get("options")).hasSize(3);
        assertThat(count("quiz_options")).isEqualTo(3);

        ResponseEntity<String> deleted =
                send(HttpMethod.DELETE, "/api/studio/quiz-questions/" + question, null, SETTER);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(count("quiz_questions")).isZero();
        assertThat(count("quiz_options")).isZero();
    }

    @Test
    @DisplayName("Đáp án đúng sai luật, thiếu lựa chọn hay đề trống đều trả 400")
    void invalidQuestions_areRejected() throws Exception {
        ResponseEntity<String> noCorrect =
                addQuestion("Câu", false, option("A", false), option("B", false));
        ResponseEntity<String> twoCorrectSingle =
                addQuestion("Câu", false, option("A", true), option("B", true));
        ResponseEntity<String> oneOption = addQuestion("Câu", false, option("A", true));
        ResponseEntity<String> blankPrompt =
                addQuestion(" ", false, option("A", true), option("B", false));

        assertThat(noCorrect.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(noCorrect.getBody()).contains("\"field\":\"options\"");
        assertThat(twoCorrectSingle.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(twoCorrectSingle.getBody()).contains("\"field\":\"options\"");
        assertThat(oneOption.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(blankPrompt.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(count("quiz_questions")).isZero();
    }

    @Test
    @DisplayName("SETTER khác nhận 404, bản chờ duyệt bị khoá sửa quiz")
    void quizRules_areEnforced() throws Exception {
        String question = idOf(addQuestion("Câu", false, option("A", true), option("B", false)));

        assertThat(send(HttpMethod.GET, questionsPath, null, OTHER_SETTER).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(
                        send(
                                        HttpMethod.DELETE,
                                        "/api/studio/quiz-questions/" + question,
                                        null,
                                        OTHER_SETTER)
                                .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        jdbcTemplate.update("UPDATE course_revisions SET status = 'IN_REVIEW'");
        ResponseEntity<String> locked =
                addQuestion("Câu 2", false, option("A", true), option("B", false));
        assertThat(locked.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(locked.getBody()).contains("\"code\":\"COURSE_LOCKED\"");
    }

    @SafeVarargs
    private ResponseEntity<String> addQuestion(
            String prompt, boolean multiple, Map<String, Object>... options) throws Exception {
        return send(
                HttpMethod.POST, questionsPath, questionJson(prompt, multiple, options), SETTER);
    }

    @SafeVarargs
    private String questionJson(String prompt, boolean multiple, Map<String, Object>... options)
            throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("prompt", prompt, "multiple", multiple, "options", List.of(options)));
    }

    private static Map<String, Object> option(String label, boolean correct) {
        return Map.of("label", label, "correct", correct);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private String idOf(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).get("id").asText();
    }

    private JsonNode body(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> send(HttpMethod method, String path, String json, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(json, headers), String.class);
    }
}
