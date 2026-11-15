CREATE TABLE enrollments (
    id BINARY(16) NOT NULL PRIMARY KEY,
    course_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_enrollments_courses FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uk_enrollments_course_id_user_id UNIQUE (course_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_enrollments_user_id_created_at ON enrollments (user_id, created_at);

CREATE TABLE lesson_progress (
    id BINARY(16) NOT NULL PRIMARY KEY,
    course_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    lesson_key BINARY(16) NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_lesson_progress_courses FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uk_lesson_progress_user_id_lesson_key UNIQUE (user_id, lesson_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_lesson_progress_user_id_course_id ON lesson_progress (user_id, course_id);
CREATE INDEX idx_lesson_progress_user_id_completed_at ON lesson_progress (user_id, completed_at);
