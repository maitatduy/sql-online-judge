package com.sqljudge.learning.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CourseListApiIntegrationTest extends ContainerSupport {

    private static final UUID AUTHOR_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000009");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void clearCourses() {
        jdbcTemplate.update("DELETE FROM course_revisions");
        jdbcTemplate.update("DELETE FROM courses");
    }

    @Test
    @DisplayName("Chưa có khoá học nào thì khách nhận trang rỗng")
    void list_noCourses_returnsEmptyPage() throws Exception {
        JsonNode page = getPage("/api/courses");

        assertThat(page.get("items")).isEmpty();
        assertThat(page.get("page").asInt()).isEqualTo(1);
        assertThat(page.get("size").asInt()).isEqualTo(20);
        assertThat(page.get("totalItems").asLong()).isZero();
    }

    @Test
    @DisplayName("Chỉ khoá đã xuất bản và chưa xoá được liệt kê, khoá mới xuất bản đứng trước")
    void list_mixedStatuses_returnsOnlyPublishedNewestFirst() throws Exception {
        insertCourse("sql-co-ban", "PUBLISHED", "EASY", "2026-11-01 08:00:00", false);
        insertCourse("join-nang-cao", "PUBLISHED", "HARD", "2026-11-05 08:00:00", false);
        insertCourse("ban-nhap", "DRAFT", "EASY", null, false);
        insertCourse("da-an", "ARCHIVED", "EASY", "2026-10-01 08:00:00", false);
        insertCourse("da-xoa", "PUBLISHED", "EASY", "2026-10-02 08:00:00", true);

        JsonNode items = getPage("/api/courses").get("items");

        assertThat(items)
                .extracting(item -> item.get("slug").asText())
                .containsExactly("join-nang-cao", "sql-co-ban");
        JsonNode first = items.get(0);
        assertThat(first.get("title").asText()).isEqualTo("Khoá join-nang-cao");
        assertThat(first.get("summary").asText()).isEqualTo("Tóm tắt join-nang-cao");
        assertThat(first.get("difficulty").asText()).isEqualTo("HARD");
        assertThat(first.get("lessonCount").asInt()).isEqualTo(4);
        assertThat(first.get("estimatedMinutes").asInt()).isEqualTo(45);
    }

    @Test
    @DisplayName("Lọc theo độ khó chỉ trả khoá có đúng độ khó đó")
    void list_filteredByDifficulty_returnsMatchingCourses() throws Exception {
        insertCourse("sql-co-ban", "PUBLISHED", "EASY", "2026-11-01 08:00:00", false);
        insertCourse("join-nang-cao", "PUBLISHED", "HARD", "2026-11-05 08:00:00", false);

        JsonNode items = getPage("/api/courses?difficulty=EASY").get("items");

        assertThat(items)
                .extracting(item -> item.get("slug").asText())
                .containsExactly("sql-co-ban");
    }

    @Test
    @DisplayName("Kích thước trang vượt 100 bị từ chối với lỗi validation")
    void list_pageSizeTooLarge_returnsValidationError() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/courses?size=101", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"code\":\"VALIDATION_FAILED\"");
    }

    private JsonNode getPage(String path) throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private void insertCourse(
            String slug, String status, String difficulty, String publishedAt, boolean deleted) {
        String courseId = UUID.randomUUID().toString();
        String revisionId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO courses (id, slug, author_id, status, published_revision_id,"
                        + " deleted_at, version, created_at, updated_at) VALUES (UUID_TO_BIN(?), ?,"
                        + " UUID_TO_BIN(?), ?, UUID_TO_BIN(?), ?, 0, NOW(6), NOW(6))",
                courseId,
                slug,
                AUTHOR_ID.toString(),
                status,
                publishedAt == null ? null : revisionId,
                deleted ? "2026-11-10 08:00:00" : null);
        jdbcTemplate.update(
                "INSERT INTO course_revisions (id, course_id, status, title, summary, difficulty,"
                        + " estimated_minutes, lesson_count, published_at, created_at, updated_at)"
                        + " VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, ?, ?, 45, 4, ?, NOW(6),"
                        + " NOW(6))",
                revisionId,
                courseId,
                publishedAt == null ? "DRAFT" : "PUBLISHED",
                "Khoá " + slug,
                "Tóm tắt " + slug,
                difficulty,
                publishedAt);
    }
}
