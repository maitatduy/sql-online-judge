CREATE TABLE contest_attempts (
    id BINARY(16) NOT NULL PRIMARY KEY,
    contest_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    problem_id BINARY(16) NOT NULL,
    submission_id BINARY(16) NOT NULL,
    verdict VARCHAR(8) NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_contest_attempts_contests FOREIGN KEY (contest_id) REFERENCES contests (id),
    CONSTRAINT uk_contest_attempts_submission_id UNIQUE (submission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_contest_attempts_contest_id_user_id_problem_id
    ON contest_attempts (contest_id, user_id, problem_id);
