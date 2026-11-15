package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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
class QuizApiIntegrationTest extends ContainerSupport {

    private static final String ATTEMPTS = "/api/courses/sql-co-ban/lessons/bai/quiz-attempts";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private CourseFixtures fixtures;
    private String lessonKey;
    private String singleQuestion;
    private String singleRight;
    private String singleWrong;
    private String multipleQuestion;
    private String multipleRightA;
    private String multipleRightB;
    private String multipleWrong;

    @BeforeEach
    void createQuiz() {
        fixtures = new CourseFixtures(jdbcTemplate);
        fixtures.clear();
        String revisionId = fixtures.publishedCourse("sql-co-ban", "EASY", "2026-11-01 08:00:00");
        String chapterId = fixtures.chapter(revisionId, "Chương", 1);
        lessonKey = fixtures.lesson(chapterId, "bai", 1);
        fixtures.lesson(chapterId, "khong-quiz", 2);
        multipleQuestion = fixtures.question(lessonKey, 2, "Mệnh đề nào lọc dòng?", true);
        singleQuestion = fixtures.question(lessonKey, 1, "Dấu * nghĩa là gì?", false);
        singleWrong = fixtures.option(singleQuestion, 2, "Một cột tên là *", false);
        singleRight = fixtures.option(singleQuestion, 1, "Mọi cột", true);
        multipleRightA = fixtures.option(multipleQuestion, 1, "WHERE", true);
        multipleRightB = fixtures.option(multipleQuestion, 2, "HAVING", true);
        multipleWrong = fixtures.option(multipleQuestion, 3, "ORDER BY", false);
    }

    @Test
    @DisplayName("Bài giảng trả câu hỏi và lựa chọn theo thứ tự, không lộ đáp án đúng")
    void lesson_returnsQuestionsWithoutAnswers() throws Exception {
        ResponseEntity<String> response =
                exchange(HttpMethod.GET, "/api/courses/sql-co-ban/lessons/bai", null);

        JsonNode questions = objectMapper.readTree(response.getBody()).get("questions");
        assertThat(questions)
                .extracting(question -> question.get("questionKey").asText())
                .containsExactly(singleQuestion, multipleQuestion);
        assertThat(questions.get(0).get("prompt").asText()).isEqualTo("Dấu * nghĩa là gì?");
        assertThat(questions.get(0).get("multiple").asBoolean()).isFalse();
        assertThat(questions.get(0).get("options"))
                .extracting(option -> option.get("optionKey").asText())
                .containsExactly(singleRight, singleWrong);
        assertThat(questions.get(1).get("multiple").asBoolean()).isTrue();
        assertThat(response.getBody()).doesNotContainIgnoringCase("correct");
    }

    @Test
    @DisplayName("Trả lời đúng hết thì bài được hoàn thành")
    void attempt_allCorrect_completesLesson() throws Exception {
        JsonNode result =
                attempt(
                        Map.of(
                                singleQuestion, List.of(singleRight),
                                multipleQuestion, List.of(multipleRightB, multipleRightA)));

        assertThat(result.get("allCorrect").asBoolean()).isTrue();
        assertThat(result.get("results"))
                .allSatisfy(question -> assertThat(question.get("correct").asBoolean()).isTrue());
        assertThat(result.get("progress").get("completedLessonKeys"))
                .extracting(JsonNode::asText)
                .containsExactly(lessonKey);
    }

    @Test
    @DisplayName("Sai một câu thì chỉ báo câu sai, không lộ đáp án và chưa hoàn thành bài")
    void attempt_withMistake_reportsWrongQuestionsOnly() throws Exception {
        JsonNode result =
                attempt(
                        Map.of(
                                singleQuestion, List.of(singleRight),
                                multipleQuestion, List.of(multipleRightA, multipleWrong)));

        assertThat(result.get("allCorrect").asBoolean()).isFalse();
        assertThat(result.get("results"))
                .extracting(
                        question ->
                                question.get("questionKey").asText()
                                        + "="
                                        + question.get("correct").asBoolean())
                .containsExactly(singleQuestion + "=true", multipleQuestion + "=false");
        assertThat(result.get("results").get(1).size()).isEqualTo(2);
        assertThat(result.get("progress").get("completedLessonKeys")).isEmpty();
    }

    @Test
    @DisplayName("Thiếu câu trả lời tính là sai")
    void attempt_missingAnswer_isWrong() throws Exception {
        JsonNode result = attempt(Map.of(singleQuestion, List.of(singleRight)));

        assertThat(result.get("allCorrect").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("Bài có quiz không hoàn thành bằng nút, bài không có quiz không nộp quiz được")
    void quizRules_areEnforced() {
        ResponseEntity<String> completion =
                exchange(HttpMethod.POST, "/api/courses/sql-co-ban/lessons/bai/completion", null);
        ResponseEntity<String> noQuiz =
                exchange(
                        HttpMethod.POST,
                        "/api/courses/sql-co-ban/lessons/khong-quiz/quiz-attempts",
                        "{\"answers\":[]}");

        assertThat(completion.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(completion.getBody()).contains("\"code\":\"LESSON_HAS_QUIZ\"");
        assertThat(noQuiz.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(noQuiz.getBody()).contains("\"code\":\"LESSON_HAS_NO_QUIZ\"");
    }

    @Test
    @DisplayName("Thiếu danh sách trả lời trả 400")
    void attempt_withoutAnswers_isRejected() {
        ResponseEntity<String> response = exchange(HttpMethod.POST, ATTEMPTS, "{}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"code\":\"VALIDATION_FAILED\"");
    }

    private JsonNode attempt(Map<String, List<String>> answers) throws Exception {
        List<Map<String, Object>> body =
                answers.entrySet().stream()
                        .map(
                                answer ->
                                        Map.<String, Object>of(
                                                "questionKey",
                                                answer.getKey(),
                                                "optionKeys",
                                                answer.getValue()))
                        .toList();
        ResponseEntity<String> response =
                exchange(
                        HttpMethod.POST,
                        ATTEMPTS,
                        objectMapper.writeValueAsString(Map.of("answers", body)));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TestJwts.learnerToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), String.class);
    }
}
