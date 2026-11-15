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
        jdbcTemplate.update("DELETE FROM lesson_runs");
        jdbcTemplate.update("DELETE FROM lesson_progress");
        jdbcTemplate.update("DELETE FROM enrollments");
        jdbcTemplate.update("DELETE FROM quiz_options");
        jdbcTemplate.update("DELETE FROM quiz_questions");
        jdbcTemplate.update("DELETE FROM lesson_cells");
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

    String cell(String lessonKey, int position, String starterSql) {
        String cellKey = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO lesson_cells (id, lesson_id, cell_key, position, starter_sql, dialect,"
                        + " dataset_version_id, dataset_variant_id, dataset_checksum, created_at,"
                        + " updated_at) SELECT UUID_TO_BIN(?), id, UUID_TO_BIN(?), ?, ?,"
                        + " 'POSTGRESQL', UUID_TO_BIN(?), UUID_TO_BIN(?), 'abc123', NOW(6), NOW(6)"
                        + " FROM lessons WHERE lesson_key = UUID_TO_BIN(?)",
                UUID.randomUUID().toString(),
                cellKey,
                position,
                starterSql,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                lessonKey);
        return cellKey;
    }

    String question(String lessonKey, int position, String prompt, boolean multiple) {
        String questionKey = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO quiz_questions (id, lesson_id, question_key, prompt, is_multiple,"
                        + " position, created_at, updated_at) SELECT UUID_TO_BIN(?), id,"
                        + " UUID_TO_BIN(?), ?, ?, ?, NOW(6), NOW(6) FROM lessons"
                        + " WHERE lesson_key = UUID_TO_BIN(?)",
                UUID.randomUUID().toString(),
                questionKey,
                prompt,
                multiple,
                position,
                lessonKey);
        return questionKey;
    }

    String option(String questionKey, int position, String label, boolean correct) {
        String optionKey = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO quiz_options (id, question_id, option_key, label, is_correct,"
                        + " position, created_at, updated_at) SELECT UUID_TO_BIN(?), id,"
                        + " UUID_TO_BIN(?), ?, ?, ?, NOW(6), NOW(6) FROM quiz_questions WHERE"
                        + " question_key = UUID_TO_BIN(?)",
                UUID.randomUUID().toString(),
                optionKey,
                label,
                correct,
                position,
                questionKey);
        return optionKey;
    }
}
