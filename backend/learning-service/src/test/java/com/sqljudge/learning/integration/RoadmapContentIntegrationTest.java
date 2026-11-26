package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(
        properties = "spring.flyway.locations=classpath:db/migration,classpath:db/content")
class RoadmapContentIntegrationTest {

    private static final String CONTENT_DATABASE = "roadmap_content";
    private static final String CONTENT_SCRIPTS = "classpath:db/content/R__*.sql";
    private static final String ROOT_USER = "root";
    private static final int MYSQL_PORT = 3306;
    private static final String SHOP_DATASET_VERSION = "01a1e000-0000-7000-8000-000000000d02";
    private static final String SHOP_DATASET_VARIANT = "01a1e000-0000-7000-8000-000000000d03";
    private static final String SHOP_DATASET_CHECKSUM =
            "b9e493a8886c9ecb9e24ba075d73d685c07d83fba6f75b1a8906f609f317d58b";
    private static final List<String> ROADMAP_SLUGS =
            List.of(
                    "sql-truy-van-co-ban",
                    "sql-noi-bang",
                    "sql-gom-nhom",
                    "sql-truy-van-long",
                    "sql-cte",
                    "sql-window-function");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private DataSource dataSource;

    @DynamicPropertySource
    static void useSeparateContentDatabase(DynamicPropertyRegistry registry) throws SQLException {
        try (Connection connection =
                        DriverManager.getConnection(
                                ContainerSupport.MYSQL.getJdbcUrl(),
                                ROOT_USER,
                                ContainerSupport.MYSQL.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS " + CONTENT_DATABASE);
        }
        registry.add(
                "spring.datasource.url",
                () ->
                        "jdbc:mysql://"
                                + ContainerSupport.MYSQL.getHost()
                                + ":"
                                + ContainerSupport.MYSQL.getMappedPort(MYSQL_PORT)
                                + "/"
                                + CONTENT_DATABASE);
        registry.add("spring.datasource.username", () -> ROOT_USER);
        registry.add("spring.datasource.password", ContainerSupport.MYSQL::getPassword);
    }

    @Test
    @DisplayName("6 khoá theo lộ trình đã xuất bản, số bài khớp với các chương")
    void roadmapCourses_arePublishedWithConsistentLessonCounts() throws Exception {
        for (String slug : ROADMAP_SLUGS) {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("/api/courses/" + slug, String.class);

            assertThat(response.getStatusCode()).as(slug).isEqualTo(HttpStatus.OK);
            JsonNode course = objectMapper.readTree(response.getBody());
            int lessonsInChapters = 0;
            for (JsonNode chapter : course.get("chapters")) {
                lessonsInChapters += chapter.get("lessons").size();
            }
            assertThat(course.get("lessonCount").asInt()).as(slug).isEqualTo(lessonsInChapters);
            assertThat(lessonsInChapters).as(slug).isGreaterThanOrEqualTo(10);
        }
    }

    @Test
    @DisplayName("Mỗi bài có ô SQL trên biến thể công khai của dataset Cửa hàng và có quiz")
    void everyLesson_hasShopCellAndQuiz() {
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM lessons l WHERE NOT EXISTS (SELECT 1"
                                        + " FROM lesson_cells c WHERE c.lesson_id = l.id)",
                                Long.class))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM lessons l WHERE NOT EXISTS (SELECT 1"
                                        + " FROM quiz_questions q WHERE q.lesson_id = l.id)",
                                Long.class))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM lesson_cells WHERE dataset_version_id <>"
                                        + " UUID_TO_BIN(?) OR dataset_variant_id <>"
                                        + " UUID_TO_BIN(?) OR dataset_checksum <> ? OR dialect"
                                        + " <> 'POSTGRESQL'",
                                Long.class,
                                SHOP_DATASET_VERSION,
                                SHOP_DATASET_VARIANT,
                                SHOP_DATASET_CHECKSUM))
                .isZero();
    }

    @Test
    @DisplayName("Câu một đáp án có đúng một lựa chọn đúng, câu nhiều đáp án có ít nhất một")
    void quizQuestions_haveValidCorrectOptions() {
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM quiz_questions q WHERE (SELECT COUNT(*)"
                                        + " FROM quiz_options o WHERE o.question_id = q.id AND"
                                        + " o.is_correct) NOT BETWEEN 1 AND CASE WHEN"
                                        + " q.is_multiple THEN 100 ELSE 1 END",
                                Long.class))
                .isZero();
    }

    @Test
    @DisplayName("Chạy lại nội dung không nhân đôi dữ liệu và giữ ghi danh của học viên")
    void contentScripts_areIdempotent() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO enrollments (id, course_id, user_id, created_at, updated_at) SELECT"
                        + " UUID_TO_BIN(UUID()), id, UUID_TO_BIN(UUID()), NOW(6), NOW(6) FROM"
                        + " courses WHERE slug = 'sql-cte'");
        List<Long> before = contentCounts();

        try (Connection connection = dataSource.getConnection()) {
            for (Resource script : contentScripts()) {
                ScriptUtils.executeSqlScript(connection, script);
            }
        }

        assertThat(contentCounts()).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM enrollments", Long.class))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Chuỗi trong file nội dung không kéo dài qua nhiều dòng")
    void contentFiles_haveNoStringLiteralSpanningLines() throws IOException {
        for (Resource script : contentScripts()) {
            assertThat(literalsSpanningLines(script.getContentAsString(StandardCharsets.UTF_8)))
                    .as(script.getFilename())
                    .isZero();
        }
    }

    private List<Long> contentCounts() {
        List<Long> counts = new ArrayList<>();
        for (String table :
                List.of(
                        "courses",
                        "course_revisions",
                        "chapters",
                        "lessons",
                        "lesson_cells",
                        "quiz_questions",
                        "quiz_options")) {
            counts.add(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class));
        }
        return counts;
    }

    private static Resource[] contentScripts() throws IOException {
        return new PathMatchingResourcePatternResolver().getResources(CONTENT_SCRIPTS);
    }

    private static int literalsSpanningLines(String script) {
        boolean insideLiteral = false;
        boolean literalHasLineBreak = false;
        int spanning = 0;
        for (char character : script.toCharArray()) {
            if (character == '\'') {
                if (insideLiteral && literalHasLineBreak) {
                    spanning += 1;
                }
                insideLiteral = !insideLiteral;
                literalHasLineBreak = false;
            } else if (insideLiteral && (character == '\n' || character == '\r')) {
                literalHasLineBreak = true;
            }
        }
        return spanning;
    }
}
