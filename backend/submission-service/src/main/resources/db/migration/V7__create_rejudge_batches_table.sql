CREATE TABLE rejudge_batches (
    id BINARY(16) NOT NULL PRIMARY KEY,
    problem_id BINARY(16) NOT NULL,
    problem_revision_id BINARY(16) NOT NULL,
    total_count INT NOT NULL,
    remaining_count INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    finished_at DATETIME(6) NULL,
    version BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_rejudge_batches_problem_id_status ON rejudge_batches (problem_id, status);
