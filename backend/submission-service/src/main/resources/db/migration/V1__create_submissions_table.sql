CREATE TABLE submissions (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    problem_id BINARY(16) NOT NULL,
    problem_revision_id BINARY(16) NOT NULL,
    dialect VARCHAR(16) NOT NULL,
    code MEDIUMTEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    verdict VARCHAR(8) NULL,
    judged_testcases INT NOT NULL,
    total_testcases INT NOT NULL,
    runtime_ms INT NULL,
    error_message VARCHAR(2000) NULL,
    first_failed_index INT NULL,
    first_failed_is_hidden BOOLEAN NULL,
    diff_json JSON NULL,
    attempts INT NOT NULL,
    idempotency_key VARCHAR(100) NULL,
    judged_at DATETIME(6) NULL,
    deleted_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT uk_submissions_user_id_idempotency_key UNIQUE (user_id, idempotency_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_submissions_user_id_problem_id_created_at ON submissions (user_id, problem_id, created_at);
