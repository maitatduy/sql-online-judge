CREATE TABLE quiz_questions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    lesson_id BINARY(16) NOT NULL,
    question_key BINARY(16) NOT NULL,
    prompt VARCHAR(500) NOT NULL,
    is_multiple BOOLEAN NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_quiz_questions_lessons FOREIGN KEY (lesson_id) REFERENCES lessons (id),
    CONSTRAINT uk_quiz_questions_lesson_id_position UNIQUE (lesson_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_quiz_questions_question_key ON quiz_questions (question_key);

CREATE TABLE quiz_options (
    id BINARY(16) NOT NULL PRIMARY KEY,
    question_id BINARY(16) NOT NULL,
    option_key BINARY(16) NOT NULL,
    label VARCHAR(300) NOT NULL,
    is_correct BOOLEAN NOT NULL,
    position INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_quiz_options_quiz_questions FOREIGN KEY (question_id) REFERENCES quiz_questions (id),
    CONSTRAINT uk_quiz_options_question_id_position UNIQUE (question_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
