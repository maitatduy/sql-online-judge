CREATE TABLE code_runs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    problem_id BINARY(16) NOT NULL,
    problem_revision_id BINARY(16) NOT NULL,
    dialect VARCHAR(16) NOT NULL,
    code MEDIUMTEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    outcome VARCHAR(8) NULL,
    result_json JSON NULL,
    error_message VARCHAR(2000) NULL,
    runtime_ms INT NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
