package com.sqljudge.learning.integration;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseFixtures {

    private static final String AUTHOR_ID = "0192f3a0-0000-7000-8000-000000000009";

    private final JdbcTemplate jdbcTemplate;

    CourseFixtures(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void clear() {
        jdbcTemplate.update("DELETE FROM lessons");
        jdbcTemplate.update("DELETE FROM chapters");
        jdbcTemplate.update("DELETE FROM course_revisions");
        jdbcTemplate.update("DELETE FROM courses");
    }

    String publishedCourse(String slug, String difficulty, String publishedAt) {
        return course(slug, "PUBLISHED", difficulty, publishedAt, false);
    }

    String course(
            String slug, String status, String difficulty, String publishedAt, boolean deleted) {
        String courseId = UUID.randomUUID().toString();
        String revisionId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO courses (id, slug, author_id, status, published_revision_id,"
                        + " deleted_at, version, created_at, updated_at) VALUES (UUID_TO_BIN(?), ?,"
                        + " UUID_TO_BIN(?), ?, UUID_TO_BIN(?), ?, 0, NOW(6), NOW(6))",
                courseId,
                slug,
                AUTHOR_ID,
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
        return revisionId;
    }

    String chapter(String revisionId, String title, int position) {
        String chapterId = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO chapters (id, revision_id, title, position, created_at, updated_at)"
                        + " VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), ?, ?, NOW(6), NOW(6))",
                chapterId,
                revisionId,
                title,
                position);
        return chapterId;
    }

    String lesson(String chapterId, String slug, int position) {
        String lessonKey = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO lessons (id, chapter_id, lesson_key, slug, title, body, position,"
                        + " created_at, updated_at) VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?),"
                        + " UUID_TO_BIN(?), ?, ?, ?, ?, NOW(6), NOW(6))",
                UUID.randomUUID().toString(),
                chapterId,
                lessonKey,
                slug,
                "Bài " + slug,
                "# Nội dung " + slug,
                position);
        return lessonKey;
    }
}
