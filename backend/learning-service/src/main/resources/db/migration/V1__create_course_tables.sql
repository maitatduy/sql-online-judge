CREATE TABLE courses (
    id BINARY(16) NOT NULL PRIMARY KEY,
    slug VARCHAR(160) NOT NULL,
    author_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    published_revision_id BINARY(16) NULL,
    draft_revision_id BINARY(16) NULL,
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_courses_slug UNIQUE (slug)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_courses_status_author_id ON courses (status, author_id);

CREATE TABLE course_revisions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    course_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    difficulty VARCHAR(10) NOT NULL,
    estimated_minutes INT NOT NULL,
    lesson_count INT NOT NULL,
    submitted_at DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_course_revisions_courses FOREIGN KEY (course_id) REFERENCES courses (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_course_revisions_course_id ON course_revisions (course_id);
CREATE INDEX idx_course_revisions_difficulty_published_at ON course_revisions (difficulty, published_at);

CREATE TABLE chapters (
    id BINARY(16) NOT NULL PRIMARY KEY,
    revision_id BINARY(16) NOT NULL,
    title VARCHAR(200) NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_chapters_course_revisions FOREIGN KEY (revision_id) REFERENCES course_revisions (id),
    CONSTRAINT uk_chapters_revision_id_position UNIQUE (revision_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE lessons (
    id BINARY(16) NOT NULL PRIMARY KEY,
    chapter_id BINARY(16) NOT NULL,
    lesson_key BINARY(16) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body MEDIUMTEXT NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_lessons_chapters FOREIGN KEY (chapter_id) REFERENCES chapters (id),
    CONSTRAINT uk_lessons_chapter_id_position UNIQUE (chapter_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_lessons_lesson_key ON lessons (lesson_key);
