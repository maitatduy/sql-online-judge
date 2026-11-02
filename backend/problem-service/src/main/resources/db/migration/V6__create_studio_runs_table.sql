CREATE TABLE studio_runs (
    id BINARY(16) NOT NULL PRIMARY KEY,
    problem_id BINARY(16) NOT NULL,
    dataset_version_id BINARY(16) NOT NULL,
    dialect VARCHAR(16) NOT NULL,
    sql_code TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    result_json LONGTEXT NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by BINARY(16) NULL,
    updated_by BINARY(16) NULL,
    CONSTRAINT fk_studio_runs_problems FOREIGN KEY (problem_id) REFERENCES problems (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_studio_runs_problem_id_created_at ON studio_runs (problem_id, created_at);
